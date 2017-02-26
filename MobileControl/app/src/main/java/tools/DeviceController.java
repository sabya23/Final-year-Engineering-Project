package tools;

import types.DeviceInfo;

/**
 * Created by sanchita on 2/5/15.
 */
public class DeviceController {

    private static DeviceInfo[] devices = null;
    private static DeviceInfo[] unapprovedDevices = null;
    private static String activeDevice;

    public static void setDeviceInfo(DeviceInfo[] device) {
        DeviceController.devices = device;
    }

    public static DeviceInfo checkDevice(String username, String userKey) {
        DeviceInfo result = null;
        if(devices != null) {
            for (int i = 0; i < devices.length; i++) {
                if(devices[i].username.equalsIgnoreCase(username) && devices[i].userKey.equalsIgnoreCase(userKey)) {
                    result = devices[i];
                    break;
                }
            }
        }
        return result;
    }

    public static void setActiveDevice(String deviceName) {
        activeDevice = deviceName;
    }

    public static String getActiveDevice() {
        return activeDevice;
    }

    public static DeviceInfo getDeviceInfo(String username) {
        DeviceInfo result = null;
        if(devices != null) {
            for (int i = 0; i < devices.length; i++) {
                if(devices[i].username.equalsIgnoreCase(username)) {
                    result = devices[i];
                    break;
                }
            }
        }
        return result;
    }

    public static void setUnapprovedDevices (DeviceInfo[] unapprovedDevices1) {
        unapprovedDevices = unapprovedDevices1;
    }

    public static DeviceInfo[] getDevices() {
        return devices;
    }

    public static DeviceInfo[] getUnapprovedDevices() {
        return unapprovedDevices;
    }
}