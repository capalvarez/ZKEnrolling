package device;

import com.zkteco.biometric.FingerprintSensorErrorCode;
import com.zkteco.biometric.FingerprintSensorEx;
import db.MySQLController;
import device.exceptions.*;
import utils.ByteArrayUtils;
import utils.FingerprintImageSaver;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

public class FingerprintDeviceController {
    private static int ATTEMPTS = 3;

    private long deviceId = 0;
    private boolean activeDevice;
    private boolean enrolling = false;
    private WorkThread captureThread = null;
    private JButton imageButton;
    private JTextArea informationArea;

    private FingerprintAlgorithmController algorithmController;

    private int fingerprintWidth = 0;
    private int fingerprintHeight = 0;

    private ArrayList<String> fingerMessages;

    private String rut;
    private ArrayList<Integer> toEnroll;
    private int currentIndex;
    private FingerprintImageSaver fingerprintImageSaver;
    private ArrayList<byte[][]> imagesBuffer;

    public FingerprintDeviceController(JButton imageButton, JTextArea area, String storagePath, ArrayList<String> messages){
        this.imageButton = imageButton;
        this.informationArea = area;
        this.fingerprintImageSaver = new FingerprintImageSaver(storagePath);
        imagesBuffer = new ArrayList<>();
        this.fingerMessages = messages;
    }

    public void setNumberOfFingers(int numberOfFingers){
        for (int i = 0; i < numberOfFingers; i++){
            imagesBuffer.add(new byte[3][]);
        }
    }

    public void openDeviceNoThread() throws OpenDeviceFailedException, NoDeviceConnectedException, FingerprintAlgorithmException{
        if(deviceId != 0){
            return;
        }

        int returnCode = FingerprintSensorEx.Init();

        if(FingerprintSensorErrorCode.ZKFP_ERR_OK != returnCode) {
            throw new OpenDeviceFailedException();
        }

        int devicesNumber = FingerprintSensorEx.GetDeviceCount();
        if(devicesNumber < 0){
            this.freeSensor();
            throw new NoDeviceConnectedException();
        }

        deviceId = FingerprintSensorEx.OpenDevice(0);

        if(deviceId == 0){
            this.freeSensor();
            throw new OpenDeviceFailedException();
        }

        algorithmController = new FingerprintAlgorithmController();

        byte[] paramValue = new byte[4];
        int[] size = new int[1];

        size[0] = 4;
        FingerprintSensorEx.GetParameters(deviceId, 1, paramValue, size);
        fingerprintWidth = ByteArrayUtils.byteArrayToInt(paramValue);

        size[0] = 4;
        FingerprintSensorEx.GetParameters(deviceId, 2, paramValue, size);
        fingerprintHeight = ByteArrayUtils.byteArrayToInt(paramValue);

        activeDevice = true;
    }

    public void openDevice() throws OpenDeviceFailedException, NoDeviceConnectedException, FingerprintAlgorithmException{
        this.openDeviceNoThread();

        captureThread = new WorkThread();
        captureThread.start();
    }

    public void closeDevice(){
        this.freeSensor();
    }

    private void freeSensor(){
        activeDevice = false;

        if(deviceId!= 0) {
            FingerprintSensorEx.CloseDevice(deviceId);
            deviceId = 0;
        }

        FingerprintSensorEx.Terminate();
    }

    public void enroll(String rut, ArrayList<Integer> toEnroll) throws ClosedDeviceException{
        if(deviceId == 0){
            throw new ClosedDeviceException();
        }
        this.rut = rut;
        enrolling = true;

        this.toEnroll = toEnroll;
        this.currentIndex = 0;
    }

    public ArrayList<FormattedFingerprints> getFingerprints() {
        return algorithmController.getFingerprints();
    }

    public void clear(){
        algorithmController.cleanUp();
        imagesBuffer = new ArrayList<>();
    }

    public void cancelEnroll(){
        this.enrolling = false;
        algorithmController.cleanUp();
        imagesBuffer = new ArrayList<>();
    }


    public boolean saveFingerprints(MySQLController controller){
        try{
            ArrayList<FormattedFingerprints> fingerprints = algorithmController.getFingerprints();

            for (int i = 0; i < fingerprints.size(); i++){
                FormattedFingerprints fingerprint = fingerprints.get(i);

                controller.insertTemplate(rut, fingerprint.getTemplate(), fingerprint.getTemplateLength(), toEnroll.get(i));
            }

        }catch (SQLException e){
            return false;
        }

        return true;
    }


    private class WorkThread extends Thread{
        ArrayList<Integer> testedFingers = new ArrayList<Integer>();

        private int getNextFinger(int currentFinger){
            int nextFinger = (currentFinger + 1)%10;

            while(testedFingers.contains(nextFinger)){
                nextFinger = (nextFinger + 1)%10;
            }

            return nextFinger;
        }

        @Override
        public void run() {
            super.run();
            int ret;
            int numberOfFingerprints = 0;
            int attemptsLeft = ATTEMPTS;
            int counter = 0;

            while(activeDevice){
                if(!enrolling){
                    numberOfFingerprints = 0;
                    try{
                        Thread.sleep(2000);
                        continue;
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }

                imagesBuffer.get(currentIndex)[numberOfFingerprints] = new byte[fingerprintWidth * fingerprintHeight];
                byte[] template = new byte[2048];
                int[] templateLen = new int[1];
                templateLen[0] = 2048;

                int statusCode = FingerprintSensorEx.AcquireFingerprint(deviceId, imagesBuffer.get(currentIndex)[numberOfFingerprints], template,
                        templateLen);

                if(statusCode == -8){
                    counter++;

                    if(counter >= 50000){
                      informationArea.setText("No se detecta el dispositivo, se habra desconectado?\n\n Se le recomienda cerrar el programa y volver a abrirlo.");
                    }
                }

                if (statusCode == 0) {
                    counter = 0;
                    byte[] paramValue = new byte[4];
                    int[] size = new int[1];
                    size[0] = 4;

                    int isFakeFingerprint;

                    ret = FingerprintSensorEx.GetParameters(deviceId, 2004, paramValue, size);
                    isFakeFingerprint = ByteArrayUtils.byteArrayToInt(paramValue);

                    if (ret == 0 && (byte) (isFakeFingerprint & 31) != 31) {
                        return;
                    }

                    String imageName = rut + "_" + numberOfFingerprints + ".bmp";
                    String imagePath = fingerprintImageSaver.saveTemplateAsImage(imagesBuffer.get(currentIndex)[numberOfFingerprints], fingerprintWidth, fingerprintHeight,
                            imageName);

                    algorithmController.setImageToProcess(imagePath);
                    try {
                        imageButton.setIcon(null);
                        imageButton.setIcon(new ImageIcon(ImageIO.read(new File(imagePath))));
                    } catch (IOException e){
                        e.printStackTrace();
                    }

                    numberOfFingerprints++;
                    informationArea.append("Huella capturada numero " + numberOfFingerprints + "\n");

                    if(numberOfFingerprints>2){
                        boolean failed = false;
                        try{
                            algorithmController.processFingerprints(currentIndex);
                            informationArea.setText("Hemos obtenido todas las muestras correctamente!\n");
                            currentIndex++;
                        }catch (Exception e){
                            attemptsLeft--;

                            if(attemptsLeft <= 0){
                                informationArea.setText("Despues de " + ATTEMPTS + " intentos no fue posible obtener esta huella.\nSe intentará con el siguiente dedo.\n");
                                testedFingers.add(toEnroll.get(currentIndex));

                                if(testedFingers.size()>9){
                                    informationArea.setText("Se ha intentado con todos los dedos y no ha\nsido posible adquirir dos huellas.\nFavor enrolar con clave.");
                                }

                                toEnroll.set(currentIndex, getNextFinger(toEnroll.get(currentIndex)));
                                informationArea.append(fingerMessages.get(toEnroll.get(currentIndex)));
                                numberOfFingerprints = 0;

                                continue;
                            }

                            failed = true;
                            informationArea.setText("No fue posible procesar las huellas. Intentos pendientes: "+ attemptsLeft +"\n");
                        }

                        numberOfFingerprints = 0;
                        if(currentIndex >=  toEnroll.size()){
                            informationArea.setText("Todas las huellas seleccionados han sido obtenidas\nPara guardar las huellas, aprete \"Guardar\"");
                            enrolling = false;
                        }else {
                            if(!failed){
                                attemptsLeft = ATTEMPTS;
                                informationArea.append("Siguiente dedo a enrollar!\n");
                                testedFingers.add(toEnroll.get(currentIndex));
                            }

                            informationArea.append(fingerMessages.get(toEnroll.get(currentIndex)));
                        }
                    }
                }
            }
        }


    }
}


