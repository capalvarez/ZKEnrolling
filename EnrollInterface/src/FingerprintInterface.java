import db.MySQLController;
import device.FingerprintDeviceController;
import device.RutController;
import device.exceptions.ClosedDeviceException;
import device.exceptions.FingerprintAlgorithmException;
import device.exceptions.NoDeviceConnectedException;
import device.exceptions.OpenDeviceFailedException;
import utils.ConfigManager;
import utils.DatabaseConfig;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;

public class FingerprintInterface extends JDialog {
    private JPanel contentPane;
    private JTextField rutField;
    private JButton imageButton;
    private JTextArea informationArea;
    private JButton enrolarButton;
    private JButton capturarHuellaButton;
    private JButton limpiarHuellasButton;
    private JLabel HandImage;
    private JCheckBox a1CheckBox;
    private JCheckBox a10CheckBox;
    private JCheckBox a2CheckBox1;
    private JCheckBox a3CheckBox;
    private JCheckBox a9CheckBox;
    private JCheckBox a8CheckBox;
    private JCheckBox a4CheckBox;
    private JCheckBox a5CheckBox;
    private JCheckBox a6CheckBox;
    private JCheckBox a7CheckBox;

    private ArrayList<JCheckBox> fingers = new ArrayList<JCheckBox>();
    private ArrayList<String> fingerMessages = new ArrayList<>();

    RutController rutController;
    FingerprintDeviceController fingerprintController;
    MySQLController dbController;

    public FingerprintInterface(ConfigManager configManager) throws ClassNotFoundException{
        this.setBackground(Color.WHITE);
        dbController = new MySQLController(configManager.getDBConfig());

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(enrolarButton);

        imageButton.setDefaultCapable(false);

        fingerMessages.add("Por favor, acerce su meñique izquierdo (número 1) 3 veces\n");
        fingerMessages.add("Por favor, acerce su anular izquierdo (número 2) 3 veces\n");
        fingerMessages.add("Por favor, acerce su dedo medio izquierdo (número 3) 3 veces\n");
        fingerMessages.add("Por favor, acerce su índice izquierdo (número 4) 3 veces\n");
        fingerMessages.add("Por favor, acerce su pulgar izquierdo (número 5) 3 veces\n");
        fingerMessages.add("Por favor, acerce su pulgar derecho (número 6) 3 veces\n");
        fingerMessages.add("Por favor, acerce su índice derecho (número 7) 3 veces\n");
        fingerMessages.add("Por favor, acerce su dedo medio derecho (número 8) 3 veces\n");
        fingerMessages.add("Por favor, acerce su anular derecho (número 9) 3 veces\n");
        fingerMessages.add("Por favor, acerce su meñique derecho (número 10) 3 veces\n");

        rutController = new RutController(this.rutField);
        fingerprintController = new FingerprintDeviceController(imageButton, informationArea, configManager.getLocalStoragePath(), fingerMessages);

        try {
            HandImage.setIcon(new ImageIcon(ImageIO.read(new File("C:\\Users\\catalin\\IdeaProjects\\ZKEnrolling\\EnrollInterface\\src\\Hands.PNG"))));
        } catch (IOException e) {
            e.printStackTrace();
        }

        fingers.add(a1CheckBox);
        fingers.add(a2CheckBox1);
        fingers.add(a3CheckBox);
        fingers.add(a4CheckBox);
        fingers.add(a5CheckBox);
        fingers.add(a6CheckBox);
        fingers.add(a7CheckBox);
        fingers.add(a8CheckBox);
        fingers.add(a9CheckBox);
        fingers.add(a10CheckBox);

        a7CheckBox.setSelected(true);
        a8CheckBox.setSelected(true);

        try {
            fingerprintController.openDevice();
        } catch (OpenDeviceFailedException e){
            informationArea.append("¡No puedo conectarme con el huellero! ¿Está conectado?\n");
        } catch (NoDeviceConnectedException e){
            informationArea.append("¡No hay ningún huellero conectado!\n");
        } catch (FingerprintAlgorithmException e){
            informationArea.append("No puedo tomar huellas :(\n");
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
                    informationArea.setText("¡Éxito! ¡Huellas guardadas correctamente!\n");
                }else{
                    informationArea.setText("No pude guardar las huellas en la base de datos :(\n");
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

    private ArrayList<Integer> getSelectedFingerIndexes(){
        ArrayList<Integer> selected = new ArrayList<Integer>();

        for(int i = 0; i < fingers.size(); i++){
            JCheckBox box = fingers.get(i);

            if(box.isSelected()){
                selected.add(i);
            }
        }

        return selected;
    }

    private void deselectAllFingers(){
        for(int i = 0; i < fingers.size(); i++){
            JCheckBox box = fingers.get(i);
            box.setSelected(false);
        }
    }

    private void cleanAll(){
        fingerprintController.clear();
        imageButton.setIcon(null);
        informationArea.setText("");
        rutField.setText("");

        deselectAllFingers();

        a7CheckBox.setSelected(true);
        a8CheckBox.setSelected(true);
    }

    private void captureFingerprints() {
        informationArea.setText("");

        if(rutField.getText().equals("")){
            informationArea.setText("No puedo enrolar sin rut :P\n");
            return;
        }

        if(rutController.checkIfRut()){
            String rut = rutController.getRut();
            try{
                ArrayList<Integer> selectedFingers = getSelectedFingerIndexes();

                if(selectedFingers.size()<2){
                    informationArea.setText("No puedo enrolar con menos de dos dedos!\nFavor seleccionar al menos dos dedos a capturar\n");
                    return;
                }

                informationArea.setText("¡Estoy listo para comenzar a enrolar!\n");
                fingerprintController.setNumberOfFingers(selectedFingers.size());

                informationArea.append(fingerMessages.get(selectedFingers.get(0)));
                fingerprintController.enroll(rut, selectedFingers);
            } catch (ClosedDeviceException e){
                informationArea.append("No puedo enrolar si no está conectado el huellero\n");
            }
        }
        else{
            informationArea.setText("Rut inválido\n");
        }
    }

    private void onCancel() {
        fingerprintController.closeDevice();
    }

     public static void main(String[] args) {
        try{
            ServerSocket ss = new ServerSocket();
            ss.bind(new InetSocketAddress(100));
        } catch (SocketException e){
            JOptionPane.showMessageDialog(null, "No puede tener el programa corriendo más de una vez!", "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (Exception e){
            JOptionPane.showMessageDialog(null, "No puede tener el programa corriendo más de una vez!", "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

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
