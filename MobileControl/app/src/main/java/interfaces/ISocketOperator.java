package interfaces;

/**
 * Created by sanchita on 2/5/15.
 */
public interface ISocketOperator {
    public String sendHttpRequest(String params);
    public int startListening(int port);
    public void stopListening();
    public void exit();
    public int getListeningPort();
}