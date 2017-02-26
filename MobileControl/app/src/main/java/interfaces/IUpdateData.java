package interfaces;

import types.DeviceInfo;
import types.MessageInfo;

/**
 * Created by sanchita on 2/5/15.
 */
public interface IUpdateData {
    public void updateData(MessageInfo[] messages, DeviceInfo[] devices, DeviceInfo[] unapprovedDevices, String userKey);
}