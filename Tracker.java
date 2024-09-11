import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;
interface TrackerInterface extends Remote {
    List<GameInfo> registerGame(String playerId, String ipAddress, int port) throws RemoteException;
    void notifyPrimary(GameInfo newGame) throws RemoteException;
    int getN() throws RemoteException;
    int getK() throws RemoteException;
}

class GameInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    String playerId;
    String ipAddress;
    int port;

    public GameInfo(String playerId, String ipAddress, int port) {
        this.playerId = playerId;
        this.ipAddress = ipAddress;
        this.port = port;
    }
}

public class Tracker extends UnicastRemoteObject implements TrackerInterface {
    private int N, K;
    private List<GameInfo> games;

    public Tracker(int N, int K) throws RemoteException {
        this.N = N;
        this.K = K;
        this.games = new ArrayList<>();
    }

    @Override
    public List<GameInfo> registerGame(String playerId, String ipAddress, int port) throws RemoteException {
        GameInfo newGame = new GameInfo(playerId, ipAddress, port);
        games.add(newGame);
        System.out.println("New game registered: " + playerId + " at " + ipAddress + ":" + port);
        
        // Notify primary server if it's not the first user
        if (games.size() > 1) {
            GameInfo primaryGame = games.get(0);
            try {
                Registry registry = LocateRegistry.getRegistry(primaryGame.ipAddress, primaryGame.port);
                GameInterface primaryServer = (GameInterface) registry.lookup("Game");
                primaryServer.notifyNewUser(newGame);
            } catch (NotBoundException e) {
                System.out.println("Primary server not found in the registry: " + e.getMessage());
            } catch (RemoteException e) {
                System.out.println("Error contacting primary server: " + e.getMessage());
            }
        }
        return games;
    }

    @Override
    public void notifyPrimary(GameInfo newGame) throws RemoteException {
        // Already handled in registerGame to avoid redundancy
    }

    @Override
    public int getN() throws RemoteException {
        return N;
    }

    @Override
    public int getK() throws RemoteException {
        return K;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Tracker [N] [K]");
            System.exit(1);
        }

        int N = Integer.parseInt(args[0]);
        int K = Integer.parseInt(args[1]);

        try {
            Tracker tracker = new Tracker(N, K);
            Registry registry = LocateRegistry.createRegistry(2000);  // Fixed port at 2000
            registry.rebind("Tracker", tracker);
            System.out.println("Tracker is running on port 2000 with N=" + N + " and K=" + K);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
