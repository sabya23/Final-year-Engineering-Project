package tools;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Vector;

import interfaces.IUpdateData;
import types.DeviceInfo;
import types.MessageInfo;
import types.STATUS;

/**
 * Created by sanchita on 2/5/15.
 */
public class XMLHandler extends DefaultHandler {

    private String userKey = new String();
    private IUpdateData updater;

    public XMLHandler(IUpdateData updater) {
        super();
        this.updater = updater;
    }

    private Vector<DeviceInfo> devices = new Vector<DeviceInfo>();
    private Vector<DeviceInfo> onlineDevices = new Vector<DeviceInfo>();
    private Vector<DeviceInfo> unapprovedDevices = new Vector<DeviceInfo>();

    private Vector<MessageInfo> unreadMessages = new Vector<MessageInfo>();

    @Override
    public void startDocument() throws SAXException {
        this.devices.clear();
        this.onlineDevices.clear();
        this.unapprovedDevices.clear();
        this.unreadMessages.clear();
        super.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {

        DeviceInfo[] allDevices = new DeviceInfo[devices.size() + onlineDevices.size()];
        DeviceInfo[] unapprovedDevice = new DeviceInfo[unapprovedDevices.size()];
        MessageInfo[] messages = new MessageInfo[unreadMessages.size()];

        int onlineDeviceCount = onlineDevices.size();

        for(int i = 0; i < onlineDeviceCount; i++) {
            allDevices[i] = onlineDevices.get(i);
        }

        int offlineDeviceCount = devices.size();

        for(int i = 0; i < offlineDeviceCount; i++) {
            allDevices[i + onlineDeviceCount] = devices.get(i);
        }

        int unapprovedDeviceCount = unapprovedDevices.size();

        for(int i = 0; i < unapprovedDeviceCount; i++) {
            unapprovedDevice[i] = unapprovedDevices.get(i);
        }

        int unreadMessageCount = unreadMessages.size();

        for(int i = 0; i < unreadMessageCount; i++) {
            messages[i] = unreadMessages.get(i);
        }

        this.updater.updateData(messages, allDevices, unapprovedDevice, userKey);
        super.endDocument();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if(localName.equals("device")) {
            DeviceInfo device = new DeviceInfo();
            device.username = attributes.getValue(DeviceInfo.USERNAME);
            String status = attributes.getValue(DeviceInfo.STATUS);
            device.ip = attributes.getValue(DeviceInfo.IP);
            device.port = attributes.getValue(DeviceInfo.PORT);
            device.userKey = attributes.getValue(DeviceInfo.USER_KEY);

            if(status != null && status.equals("online")) {
                device.status = STATUS.ONLINE;
                onlineDevices.add(device);
            }
            else if(status.equals("unApproved")) {
                device.status = STATUS.UNAPPROVED;
                unapprovedDevices.add(device);
            }
            else {
                device.status = STATUS.OFFLINE;
                devices.add(device);
            }
        }
        else if(localName.equals("user")) {
            this.userKey = attributes.getValue(DeviceInfo.USER_KEY);
        }
        else if(localName.equals("message")) {
            MessageInfo message = new MessageInfo();
            message.userId = attributes.getValue(MessageInfo.USER_ID);
            message.sendT = attributes.getValue(MessageInfo.SEND_T);
            message.messageText = attributes.getValue(MessageInfo.MESSAGE_TEXT);
            unreadMessages.add(message);
        }
        super.startElement(uri, localName, qName, attributes);
    }
}