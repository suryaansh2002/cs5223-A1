import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Tracker_Interface extends Remote {
    Integer getPort() throws RemoteException;
    Integer getN() throws RemoteException;
    Integer getK() throws RemoteException;
    List<Game_Interface> getServerList() throws RemoteException;

    List<Game_Interface> joinGame(String host, int port, String playerName) throws RemoteException, MalformedURLException, NotBoundException;
    void initializeGame(int n, int k) throws RemoteException;
    List<Game_Interface> setServerList(List<Game_Interface> serverList) throws RemoteException;
}
