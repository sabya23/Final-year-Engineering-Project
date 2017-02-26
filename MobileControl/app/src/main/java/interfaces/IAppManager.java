package interfaces;

import java.io.UnsupportedEncodingException;

/**
 * Created by sanchita on 2/5/15.
 */
public interface IAppManager {

    public String getUserName();
    public String sendMessage(String username, String toUsername, String message) throws UnsupportedEncodingException;
    public String authenticateUser(String usernameText, String passwordText) throws UnsupportedEncodingException;
    public void messageReceived(String username, String message);
    public boolean isNetworkConnected();
    public boolean isUserAuthenticated();
    public String getLastRawDeviceList();
    public void exit();
    public String signUpUser(String usernameText, String passwordText, String email);
    public String addNewDeviceRequest(String deviceUserName);
    public String sendDeviceRqResponse(String approvedDeviceList, String discardedDeviceList);
}