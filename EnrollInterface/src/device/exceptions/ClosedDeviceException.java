package device.exceptions;

public class ClosedDeviceException extends Exception {
    public ClosedDeviceException(){
        super("Can not operate on a closed device");
    }

}
