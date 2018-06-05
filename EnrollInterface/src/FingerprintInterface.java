import db.MySQLController;
import device.FingerprintDeviceController;
import device.RutController;
import device.exceptions.ClosedDeviceException;
import device.exceptions.FingerprintAlgorithmException;
import device.exceptions.NoDeviceConnectedException;
import device.exceptions.OpenDeviceFailedException;
import utils.ConfigManager;
import utils.DatabaseConfig;

import javax.swing.*;
import java.awt.event.*;

public class FingerprintInterface extends JDialog {
    private JPanel contentPane;
    private JTextField rutField;
    private JButton imageButton;
    private JTextArea informationArea;
    private JButton enrolarButton;
    private JButton capturarHuellaButton;
    private JButton limpiarHuellasButton;

    RutController rutController;
    FingerprintDeviceController fingerprintController;
    MySQLController dbController;

    public FingerprintInterface(ConfigManager configManager) throws ClassNotFoundException{
        dbController = new MySQLController(configManager.getDBConfig());

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(enrolarButton);

        imageButton.setDefaultCapable(false);

        rutController = new RutController(this.rutField);
        fingerprintController = new FingerprintDeviceController(imageButton, informationArea, configManager.getLocalStoragePath());

        try {
            fingerprintController.openDevice();
        } catch (OpenDeviceFailedException e){
            informationArea.append("Could not open device!\n");
        } catch (NoDeviceConnectedException e){
            informationArea.append("No fingerprint device connected!\n");
        } catch (FingerprintAlgorithmException e){
            informationArea.append("Could not initialize fingerprint algorithm!\n");
        }

        limpiarHuellasButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cleanAll();
            }
        });

        enrolarButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(fingerprintController.saveFingerprints(dbController)){
                    cleanAll();
                    informationArea.setText("Sucess! Fingerprints saved!");
                }else{
                    informationArea.setText("Could not save fingerprints in database\n");
                }

            }
        });

        capturarHuellaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                captureFingerprints();
            }
        });

        // call onCancel() when cross is clicked
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void cleanAll(){
        fingerprintController.clear();
        imageButton.setIcon(null);
        informationArea.setText("");
        rutField.setText("");
    }

    private void captureFingerprints() {
        informationArea.setText("");

        if(rutController.checkIfRut()){
            String rut = rutController.getRut();
            try{
                informationArea.setText("Begining enrolling!\n");
                fingerprintController.enroll(rut);
            } catch (ClosedDeviceException e){
                informationArea.append("Device not open?\n");
            }
        }
        else{
            informationArea.setText("Can not enroll with no rut\n");
        }
    }

    private void onCancel() {
        fingerprintController.closeDevice();
    }

     public static void main(String[] args) {
        try {
            ConfigManager configManager = new ConfigManager("C:\\Users\\catalin\\IdeaProjects\\ZKEnrolling\\EnrollInterface\\resources\\config.properties");

            FingerprintInterface dialog = new FingerprintInterface(configManager);
            dialog.pack();
            dialog.setVisible(true);
            System.exit(0);
        } catch (ClassNotFoundException e){
            e.printStackTrace();
        }
    }
}
