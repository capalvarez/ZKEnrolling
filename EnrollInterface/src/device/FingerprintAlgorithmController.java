package device;

import com.zkteco.biometric.FingerprintSensor;
import com.zkteco.biometric.FingerprintSensorEx;
import device.exceptions.CouldNotProcessFingerprintException;
import device.exceptions.FingerprintAlgorithmException;

import java.util.ArrayList;

public class FingerprintAlgorithmController {
    private long deviceDBHandler = 0;
    private ArrayList<String> imagesToProcess;
    private ArrayList<FormattedFingerprints> fingerprints;

    public FingerprintAlgorithmController() throws FingerprintAlgorithmException{
        deviceDBHandler = FingerprintSensorEx.DBInit();

        if (deviceDBHandler == 0){
            throw new FingerprintAlgorithmException();
        }

        imagesToProcess = new ArrayList<String>();
        fingerprints = new ArrayList<>();
    }

    public void setImageToProcess(String path){
        imagesToProcess.add(path);
    }

    public void processFingerprints(int index) throws FingerprintAlgorithmException, CouldNotProcessFingerprintException{
        FormattedFingerprints fingerprints = new FormattedFingerprints(3);

        int fingerprintFormat = 1; // ISO
        FingerprintSensorEx.DBSetParameter(deviceDBHandler, 5010, fingerprintFormat);

        byte[][] templates = new byte[3][2048];
        int[][] sizes = new int[3][1];

        for (int j = 0; j < 3; j++) {
            sizes[j][0] = 2048;
            String s = imagesToProcess.get(3*index + j);
            int returnCode = FingerprintSensorEx.ExtractFromImage(deviceDBHandler, s, 500, templates[j], sizes[j]);

            if (returnCode != 0) {
                throw new CouldNotProcessFingerprintException();
            }
        }

        byte[] finalTemplate = new byte[2048];
        int[] finalLength = new int[1];
        finalLength[0] = 2048;

        int returnCode = FingerprintSensorEx.DBMerge(deviceDBHandler, templates[0], templates[1], templates[2],
                finalTemplate, finalLength);

        if (returnCode != 0) {
            throw new FingerprintAlgorithmException();
        }

        String base64Template = FingerprintSensor.BlobToBase64(finalTemplate, finalLength[0]);
        fingerprints.addTemplate(base64Template, finalLength[0]);

        this.fingerprints.add(fingerprints);
    }

    public ArrayList<FormattedFingerprints> getFingerprints(){
        return this.fingerprints;
    }

    public void cleanUp(){
        imagesToProcess.clear();
        fingerprints.clear();
    }
}
