import java.util.List;
import java.rmi.RemoteException;
import java.rmi.Remote;
import java.rmi.NotBoundException;
import java.net.MalformedURLException;

public interface Tracker_Interface extends Remote {
    
    Integer getgridDimensions() throws RemoteException;
   
    Integer getTrackerPort() throws RemoteException;
    List<Game_Interface> getTrackerServerList() throws RemoteException;

    List<Game_Interface> setTrackerServerList(List<Game_Interface> serverList) throws RemoteException;
    Integer getNumOfTreasures() throws RemoteException;
    void initializeGame(int n, int k) throws RemoteException;
   
    List<Game_Interface> joinGame(String host, int port, String playerName) throws RemoteException, MalformedURLException, NotBoundException;

}
