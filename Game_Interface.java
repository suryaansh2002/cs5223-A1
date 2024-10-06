import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Game_Interface extends Remote {
    // Primary/Backup Server Management
    void setprimary(Boolean primary) throws RemoteException;
    void setbackup(Boolean backup) throws RemoteException;
    boolean getIsprimary() throws RemoteException;
    boolean getIsbackup() throws RemoteException;
    void assignNewbackupServer(GameState gameState) throws RemoteException;
    void backupBecomeprimary(String originalprimaryplayerName) throws RemoteException;
    void startbackupPingPrimaryThread() throws RemoteException;
    boolean primaryServerAddNewPlayer(String playerName) throws RemoteException, MalformedURLException, NotBoundException, InterruptedException;

    // Game State Management
    void updateGameState(GameState gameState) throws RemoteException;
    void setServerGameState(GameState serverGameState) throws RemoteException;
    GameState getServerGameState() throws RemoteException;
    void setGameStart(Boolean gameStart) throws RemoteException;

    // Player Interaction
    GameState move(String playerName, Direction direction, int numOfStep) throws RemoteException, GameErrorException, MalformedURLException, NotBoundException;
    void startGame(GameState gameState) throws RemoteException;
    void quit() throws RemoteException;

    // Utility Methods
    void setGameListValue(List<Game_Interface> gameList) throws RemoteException;
    void updateServersGameList(List<Game_Interface> gameList) throws RemoteException;
    void joinGameFromTracker() throws RemoteException, NotBoundException, MalformedURLException;
    String getName() throws RemoteException;
    void ping() throws RemoteException;
}
