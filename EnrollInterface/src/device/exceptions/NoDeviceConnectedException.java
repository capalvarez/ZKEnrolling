package device.exceptions;

public class NoDeviceConnectedException extends Exception {
    public NoDeviceConnectedException(){
        super("No device detected");
    }

}
