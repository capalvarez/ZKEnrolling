import db.MySQLController;
import device.FingerprintDeviceController;
import device.RutController;
import device.exceptions.ClosedDeviceException;
import device.exceptions.FingerprintAlgorithmException;
import device.exceptions.NoDeviceConnectedException;
import device.exceptions.OpenDeviceFailedException;
import utils.ConfigManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
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
    private JButton enrolarConClaveButton;
    private JButton cancelarCapturaDeHuellasButton;

    private ArrayList<JCheckBox> fingers = new ArrayList<JCheckBox>();
    private ArrayList<String> fingerMessages = new ArrayList<>();

    RutController rutController;
    FingerprintDeviceController fingerprintController;
    MySQLController dbController;

    public FingerprintInterface() throws ClassNotFoundException {
        ConfigManager configManager = new ConfigManager(getClass().getResourceAsStream("config.properties"));

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
            Image myImage = ImageIO.read(getClass().getResourceAsStream("Hands.PNG"));
            HandImage.setIcon(new ImageIcon(myImage));
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
        } catch (OpenDeviceFailedException e) {
            informationArea.append("¡No puedo conectarme con el huellero! ¿Está conectado?\n");
        } catch (NoDeviceConnectedException e) {
            informationArea.append("¡No hay ningún huellero conectado!\n");
        } catch (FingerprintAlgorithmException e) {
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
                if (fingerprintController.saveFingerprints(dbController)) {
                    cleanAll();
                    informationArea.setText("¡Éxito! ¡Huellas guardadas correctamente!\n");
                } else {
                    informationArea.setText("No pude guardar las huellas en la base de datos :(\n");
                }

            }
        });

        cancelarCapturaDeHuellasButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

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

    private ArrayList<Integer> getSelectedFingerIndexes() {
        ArrayList<Integer> selected = new ArrayList<Integer>();

        for (int i = 0; i < fingers.size(); i++) {
            JCheckBox box = fingers.get(i);

            if (box.isSelected()) {
                selected.add(i);
            }
        }

        return selected;
    }

    private void deselectAllFingers() {
        for (int i = 0; i < fingers.size(); i++) {
            JCheckBox box = fingers.get(i);
            box.setSelected(false);
        }
    }

    private void cleanAll() {
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

        if (rutField.getText().equals("")) {
            informationArea.setText("No puedo enrolar sin rut :P\n");
            return;
        }

        if (rutController.checkIfRut()) {
            String rut = rutController.getRut();
            try {
                ArrayList<Integer> selectedFingers = getSelectedFingerIndexes();

                if (selectedFingers.size() < 2) {
                    informationArea.setText("No puedo enrolar con menos de dos dedos!\nFavor seleccionar al menos dos dedos a capturar\n");
                    return;
                }

                informationArea.setText("¡Estoy listo para comenzar a enrolar!\n");
                fingerprintController.setNumberOfFingers(selectedFingers.size());

                informationArea.append(fingerMessages.get(selectedFingers.get(0)));
                fingerprintController.enroll(rut, selectedFingers);
            } catch (ClosedDeviceException e) {
                informationArea.append("No puedo enrolar si no está conectado el huellero\n");
            }
        } else {
            informationArea.setText("Rut inválido\n");
        }
    }

    private void onCancel() {
        fingerprintController.closeDevice();
        dbController.disconnect();
    }

    public static void main(String[] args) {
        try {
            ServerSocket ss = new ServerSocket();
            ss.bind(new InetSocketAddress(100));
        } catch (SocketException e) {
            JOptionPane.showMessageDialog(null, "No puede tener el programa corriendo más de una vez!", "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "No puede tener el programa corriendo más de una vez!", "Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        try {
            FingerprintInterface dialog = new FingerprintInterface();
            dialog.pack();
            dialog.setVisible(true);
            System.exit(0);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        contentPane.setForeground(new Color(-1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(5, 10, 10, 10), -1, -1));
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        contentPane.add(panel1, gbc);
        enrolarButton = new JButton();
        enrolarButton.setText("Guardar huellas");
        panel1.add(enrolarButton, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        capturarHuellaButton = new JButton();
        capturarHuellaButton.setText("Capturar Huella");
        panel1.add(capturarHuellaButton, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        limpiarHuellasButton = new JButton();
        limpiarHuellasButton.setText("Limpiar");
        panel1.add(limpiarHuellasButton, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 4, new Insets(10, 10, 10, 10), -1, -1));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        contentPane.add(panel2, gbc);
        rutField = new JTextField();
        panel2.add(rutField, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Rut");
        panel2.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        informationArea = new JTextArea();
        informationArea.setText("");
        panel2.add(informationArea, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(300, 100), null, 0, false));
        imageButton = new JButton();
        imageButton.setHorizontalAlignment(2);
        imageButton.setHorizontalTextPosition(2);
        imageButton.setText("");
        imageButton.setVerticalAlignment(1);
        imageButton.setVerticalTextPosition(1);
        panel2.add(imageButton, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTH, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(350, 400), null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        panel2.add(panel3, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, 1, 1, null, new Dimension(100, 200), null, 0, false));
        HandImage = new JLabel();
        HandImage.setText("");
        panel3.add(HandImage, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 10, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel4, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        a1CheckBox = new JCheckBox();
        a1CheckBox.setText("1");
        panel4.add(a1CheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        a10CheckBox = new JCheckBox();
        a10CheckBox.setText("10");
        panel4.add(a10CheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 9, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        a2CheckBox1 = new JCheckBox();
        a2CheckBox1.setText("2");
        panel4.add(a2CheckBox1, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        a3CheckBox = new JCheckBox();
        a3CheckBox.setText("3");
        panel4.add(a3CheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        a9CheckBox = new JCheckBox();
        a9CheckBox.setText("9");
        panel4.add(a9CheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 8, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        a8CheckBox = new JCheckBox();
        a8CheckBox.setText("8");
        panel4.add(a8CheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 7, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        a4CheckBox = new JCheckBox();
        a4CheckBox.setText("4");
        panel4.add(a4CheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        a5CheckBox = new JCheckBox();
        a5CheckBox.setText("5");
        panel4.add(a5CheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        a6CheckBox = new JCheckBox();
        a6CheckBox.setText("6");
        panel4.add(a6CheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        a7CheckBox = new JCheckBox();
        a7CheckBox.setText("7");
        panel4.add(a7CheckBox, new com.intellij.uiDesigner.core.GridConstraints(0, 6, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        enrolarConClaveButton = new JButton();
        enrolarConClaveButton.setText("Enrolar con clave");
        panel2.add(enrolarConClaveButton, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
