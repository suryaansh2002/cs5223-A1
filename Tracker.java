import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Tracker extends UnicastRemoteObject implements TrackerInterface, Serializable{
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(Tracker.class.getName());

    private List<GameInterface> games;

    private int gridSize;
    private int numTreasures;
    private int port;

    public Tracker(Integer port, Integer N, Integer K) throws RemoteException {
        this.port = port;
        this.gridSize = N;
        this.numTreasures = K;

        this.games = new ArrayList<>();
    }

    public Integer getPort() {
        return this.port;
    }

    public Integer getGridSize() {
        return this.gridSize;
    }

    public Integer getNumTreasures() {
        return this.numTreasures;
    }

    public List<GameInterface> getGames() {
        return this.games;
    }

    public List<GameInterface> registerGame(String host, Integer port, String playerID) {
        String gameURL = "rmi://" + host + ":" + port + "/" + playerID;
        try {
            GameInterface game = (GameInterface) Naming.lookup(gameURL);
            
            if (this.games.isEmpty()) {
                game.setPrimary(true);
                games.add(game);
            } else {
                game.setPrimary(false);
            }
            return this.games;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error registering game: {0}", e.getMessage());
            e.printStackTrace();
        }

        return this.games;
    }

    public void initGame(int n, int k) {
        this.gridSize = n;
        this.numTreasures = k;
    }

    public List<GameInterface> setGames(List<GameInterface> games) {
        this.games = games;
        printCurrentGames();
        return this.games;
    }

    private void printCurrentGames() {
        logger.log(Level.INFO, "Current games:");
        for (GameInterface game : this.games) {
            try {
                logger.log(Level.INFO, "Game: {0} Primary: {1} Backup: {2}", new Object[]{game.getPlayerID(), game.isPrimary(), game.isBackup()});
            } catch (RemoteException e) {
                logger.log(Level.SEVERE, "Error getting game details: {0}", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            logger.severe("Usage: java Tracker <port> <N> <K>");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        int n = Integer.parseInt(args[1]);
        int k = Integer.parseInt(args[2]);
        
        try {
            TrackerInterface tracker = new Tracker(port, n, k);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind("Tracker", tracker);
            logger.log(Level.INFO, "Tracker ready on port {0}", port);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Tracker exception: {0}", e.getMessage());
            e.printStackTrace();
        }
    }
}
