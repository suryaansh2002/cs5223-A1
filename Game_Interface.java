import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Game_Interface extends Remote {
    void setprimary(Boolean primary) throws RemoteException;
    void setGameListValue(List<Game_Interface> gameList) throws RemoteException;
    void joinGameFromTracker() throws RemoteException, NotBoundException, MalformedURLException;
    void ping() throws RemoteException;
    void startbackupPingPrimaryThread() throws RemoteException;
    void updateGameState(GameState gameState) throws RemoteException;
    void updateServersGameList(List<Game_Interface> gameList) throws RemoteException;
    void assignNewbackupServer(GameState gameState) throws RemoteException;
    void backupBecomeprimary(String originalprimaryplayerName) throws RemoteException;
    boolean primaryServerAddNewPlayer(String playerName) throws RemoteException, MalformedURLException, NotBoundException, InterruptedException;
    void startGame(GameState gameState) throws RemoteException;
    GameState move(String playerName, Direction direction, int numOfStep) throws RemoteException, GameErrorException, MalformedURLException, NotBoundException;
    void setbackup(Boolean backup) throws RemoteException;
    String getName() throws RemoteException;
    boolean getIsprimary() throws RemoteException;
    boolean getIsbackup() throws RemoteException;
    void setServerGameState(GameState serverGameState) throws RemoteException;
    void setGameStart(Boolean gameStart) throws RemoteException;
    GameState getServerGameState() throws RemoteException;
    void quit() throws RemoteException;
}
