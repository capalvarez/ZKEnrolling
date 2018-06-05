package device.exceptions;

public class OpenDeviceFailedException extends Exception {
    public OpenDeviceFailedException(){
        super("Could not open the device, please try again");
    }

}
