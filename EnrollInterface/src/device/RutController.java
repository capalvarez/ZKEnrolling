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

    private String clearFormat(String value){
        value =  value.toUpperCase();
        value = value.replace(".", "");
        value = value.replace("-", "");

        return value;
    }

    private boolean isRut(String value){
        try{
            String cleared = this.clearFormat(value);

            if(value.length()<2){
                return false;
            }

            String rutWithoutDV = cleared.substring(0, cleared.length() - 1);

            Integer.parseInt(rutWithoutDV);
            char lastDigit = cleared.charAt(cleared.length()-1);

            return (lastDigit=='K' || isNumeric(lastDigit)) && computeDV(rutWithoutDV) == lastDigit;

        } catch(java.lang.NumberFormatException e){
            return false;
        }
    }

    private char computeDV(String rut){
        int sum = 0;
        int mult = 2;

        for (int i = rut.length() - 1; i >= 0 ; i--) {
            sum += Character.getNumericValue(rut.charAt(i)) * mult;
            mult = (mult + 1)% 8;

            if(mult < 2){
                mult = 2;
            }
        }

        switch (sum % 11){
            case 0: return Character.forDigit(0, 10);
            case 1: return 'K';
            default: return Character.forDigit(11 - (sum % 11), 10);
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

    public boolean checkPassword(String pass){
        if(pass.length() == 0 || pass.length() > 7){
            return false;
        }

        try {
            Integer.parseInt(pass);
            return true;
        }catch (java.lang.NumberFormatException e){
            return false;
        }

    }
    
}
