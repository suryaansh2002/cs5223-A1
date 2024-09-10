import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface TrackerInterface extends Remote {
    GameInfo registerGame(String playerID, String ipAddress, int port) throws RemoteException;
    Map<String, GameInfo> getGameMap() throws RemoteException;
}