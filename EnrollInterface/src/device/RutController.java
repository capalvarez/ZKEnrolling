package device;

import utils.FingerprintImageSaver;

import javax.swing.*;

public class RutController {
    private JTextField rutField;

    public RutController(JTextField rutField){
        this.rutField = rutField;
    }

    public String getRut(){
        return this.formatRut(this.rutField.getText());
    }

    public boolean checkIfRut(){
        String rut = rutField.getText();

        if(rut == ""){
            return false;
        }

        return isRut(rut);
    }

    private String formatRut(String rut){
        rut = rut.toUpperCase();
        rut = rut.replace(".", "");
        rut = rut.replace("-", "");

        return FingerprintImageSaver.leftPad(rut, 10);
    }

    private boolean isRut(String value){
        try{
            value =  value.toUpperCase();
            value = value.replace(".", "");
            value = value.replace("-", "");
            int rutAux = Integer.parseInt(value.substring(0, value.length() - 1));

            char lastDigit = value.charAt(value.length()-1);

            return lastDigit=='K' || isNumeric(lastDigit);

        } catch(java.lang.NumberFormatException e){
            return false;
        }
    }

    private boolean isNumeric(char a){
        try{
            int aux = Integer.parseInt(a+"");
            return true;
        } catch (java.lang.NumberFormatException e){
            return false;
        }
    }
    
}
