import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface TrackerInterface extends Remote {
    Integer getPort() throws RemoteException;
    Integer getGridSize() throws RemoteException;
    Integer getNumTreasures() throws RemoteException;
    
    // Get the list of games
    List<GameInterface> getGames() throws RemoteException;
    // Register a new game
    List<GameInterface> registerGame(String host, Integer port, String playerID) throws RemoteException;
    // Initialise the game
    void initGame(int n, int k) throws RemoteException;
    // Set the list of games
    List<GameInterface> setGames(List<GameInterface> games) throws RemoteException;
}