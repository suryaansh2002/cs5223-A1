import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface GameInterface extends Remote {    
    // Get a player ID
    String getPlayerID() throws RemoteException;
    // connect a game to a tracker
    void setTracker(TrackerInterface tracker) throws RemoteException, InterruptedException;
    // start the game on the primary server
    void startGame(GameState gameState) throws RemoteException;
    // Set the game state
    void setGameState(GameState gameState) throws RemoteException;

    // ping for health check
    void ping() throws RemoteException;
    // start a thread to ping the players from the primary server
    void startPrimaryPingThread() throws RemoteException;
    // Set a game as primary server
    void setPrimary(boolean primary) throws RemoteException;
    // Boolean check if the game is a primary server
    boolean isPrimary() throws RemoteException;


    // Set a new backup server
    void setBackupServer(GameState gameState) throws RemoteException, InterruptedException;
    // set a game as backup server
    void setBackup(boolean backup) throws RemoteException;
    // Boolean check if the game is a backup server
    boolean isBackup() throws RemoteException;
    // Promotion of a backup server to primary
    void promoteBackupServer(String primaryServerPlayerId) throws RemoteException, InterruptedException;
    // start a thread to ping the primary server from the backup server
    void startBackupPingThread() throws RemoteException;

    // update the game state on the backup server
    void updateGameState(GameState gameState) throws RemoteException;
    // update the games list on the backup server
    void updateGamesList(List<GameInterface> games) throws RemoteException;

    // Add a new player to the game
    boolean addPlayer(String playerId) throws RemoteException;
    // Move a player in a certain direction and return the updated game state
    GameState movePlayerDirection(String playerId, Direction move) throws RemoteException, InterruptedException;
    // quit the game
    void quitGame() throws RemoteException, InterruptedException;
}
