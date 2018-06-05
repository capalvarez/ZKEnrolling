package device;

import com.zkteco.biometric.FingerprintSensor;

public class FormattedFingerprints {
    private int templateLength;
    private String template;
    private byte[][] images;

    public FormattedFingerprints(int numberOfImages){
        images = new byte[numberOfImages][2048];
    }

    public void addTemplate(String t, int length){
        template = t;
        templateLength = length;
    }

    public String getTemplate(){
        return template;
    }

    public int getTemplateLength(){
        return templateLength;
    }
}
