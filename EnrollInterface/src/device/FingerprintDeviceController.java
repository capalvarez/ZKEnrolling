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

public class FingerprintDeviceController {
    private long deviceId = 0;
    private boolean activeDevice;
    private boolean enrolling = false;
    private WorkThread captureThread = null;
    private JButton imageButton;
    private JTextArea informationArea;

    private FingerprintAlgorithmController algorithmController;

    private int fingerprintWidth = 0;
    private int fingerprintHeight = 0;

    private String rut;
    private FingerprintImageSaver fingerprintImageSaver;
    private byte[][] imagesBuffer;

    public FingerprintDeviceController(JButton imageButton, JTextArea area, String storagePath){
        this.imageButton = imageButton;
        this.informationArea = area;
        this.fingerprintImageSaver = new FingerprintImageSaver(storagePath);
        imagesBuffer = new byte[3][];
    }

    public void openDevice() throws OpenDeviceFailedException, NoDeviceConnectedException, FingerprintAlgorithmException{
        if(deviceId != 0){
            return;
        }

        int returnCode = FingerprintSensorErrorCode.ZKFP_ERR_OK;
        if(FingerprintSensorErrorCode.ZKFP_ERR_OK != FingerprintSensorEx.Init()) {
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

    public void enroll(String rut) throws ClosedDeviceException{
        if(deviceId == 0){
            throw new ClosedDeviceException();
        }
        this.rut = rut;
        enrolling = true;
    }

    public FormattedFingerprints getFingerprints() {
        return algorithmController.getFingerprints();
    }

    public void clear(){
        algorithmController.cleanUp();
    }

    public boolean saveFingerprints(MySQLController controller){
        try{
            FormattedFingerprints fingerprints = algorithmController.getFingerprints();

            controller.insertTemplate(rut, fingerprints.getTemplate(), fingerprints.getTemplateLength());

            for (int i = 0; i < imagesBuffer.length; i++) {
                controller.insertImage(rut, imagesBuffer[i]);
            }
        }catch (SQLException e){
            return false;
        }

        return true;
    }

    private class WorkThread extends Thread{
        @Override
        public void run() {
            super.run();
            int ret;
            int numberOfFingerprints = 0;

            while(activeDevice){
                if(!enrolling){
                    try{
                        Thread.sleep(2000);
                        continue;
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }

                imagesBuffer[numberOfFingerprints] = new byte[fingerprintWidth * fingerprintHeight];
                byte[] template = new byte[2048];
                int[] templateLen = new int[1];
                templateLen[0] = 2048;

                int statusCode = FingerprintSensorEx.AcquireFingerprint(deviceId, imagesBuffer[numberOfFingerprints], template,
                        templateLen);

                if (statusCode == 0) {
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
                    String imagePath = fingerprintImageSaver.saveTemplateAsImage(imagesBuffer[numberOfFingerprints], fingerprintWidth, fingerprintHeight,
                            imageName);

                    algorithmController.setImageToProcess(imagePath);
                    try {
                        imageButton.setIcon(null);
                        imageButton.setIcon(new ImageIcon(ImageIO.read(new File(imagePath))));
                    } catch (IOException e){
                        e.printStackTrace();
                    }

                    numberOfFingerprints++;
                    informationArea.append("Captured fingerprint number " + numberOfFingerprints + "\n");

                    if(numberOfFingerprints>2){
                        informationArea.setText("Finished capturing fingerprint!\n");

                        try{
                            algorithmController.processFingerprints();
                        }catch (Exception e){
                            informationArea.setText("Could not process obtained fingerprints, please try capturing again :(");
                        }

                        numberOfFingerprints = 0;
                        enrolling = false;
                    }
                }
            }
        }


    }
}


