import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

interface GameInterface extends Remote {
    void ping() throws RemoteException;
    void notifyNewUser(GameInfo newGame) throws RemoteException;
    void updateGameList(List<GameInfo> updatedList) throws RemoteException;
}

public class Game extends UnicastRemoteObject implements GameInterface {
    private String playerId;
    private List<GameInfo> gameList;
    private int serverPort;
    private Timer pingTimer;
    private static final String TRACKER_IP = "127.0.0.1"; // Default Tracker IP
    private static final int TRACKER_PORT = 2000; // Default Tracker port

    protected Game(String playerId, int port) throws RemoteException {
        super(port);  // Run Game instance on the specified port
        this.playerId = playerId;
        this.serverPort = port;
        this.gameList = new ArrayList<>();
    }

    @Override
    public void ping() throws RemoteException {
        System.out.println("Ping received from primary server.");
    }

    @Override
    public void notifyNewUser(GameInfo newGame) throws RemoteException {
        System.out.println("New user to ping: " + newGame.playerId);
        gameList.add(newGame);  // Add the new user to the game list
    }

    @Override
    public void updateGameList(List<GameInfo> updatedList) throws RemoteException {
        gameList = updatedList;
        System.out.println("Updated game list received from primary server:");
        for (GameInfo game : gameList) {
            System.out.println("Player: " + game.playerId + " at " + game.ipAddress + ":" + game.port);
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Game [port-number] [Player-ID]");
            System.exit(1);
        }

        int gamePort = Integer.parseInt(args[0]);
        String playerId = args[1];

        try {
            // Locate Tracker
            Registry registry = LocateRegistry.getRegistry(TRACKER_IP, TRACKER_PORT);
            TrackerInterface tracker = (TrackerInterface) registry.lookup("Tracker");

            // Register this Game instance
            String ipAddress = java.net.InetAddress.getLocalHost().getHostAddress();
            Game gameInstance = new Game(playerId, gamePort);
            Registry gameRegistry = LocateRegistry.createRegistry(gamePort);
            gameRegistry.rebind("Game", gameInstance);

            List<GameInfo> gameList = tracker.registerGame(playerId, ipAddress, gamePort);
            gameInstance.gameList = new ArrayList<>(gameList);  // Initialize game list with the list from the Tracker

            System.out.println("Registered with Tracker. Current games:");
            for (GameInfo game : gameList) {
                System.out.println("Player: " + game.playerId + " at " + game.ipAddress + ":" + game.port);
            }

            // If this is the primary server, start pinging
            if (gameList.get(0).playerId.equals(playerId)) {
                System.out.println("I am the primary server.");
                startPinging(gameInstance);
            }

            // Keep the Game running
            while (true) {
                if (System.in.read() == '9') {
                    System.out.println("Game server is shutting down.");
                    if (gameInstance.pingTimer != null) {
                        gameInstance.pingTimer.cancel();
                    }
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startPinging(Game gameInstance) {
        gameInstance.pingTimer = new Timer();
        gameInstance.pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Pinging all game servers...");
                List<GameInfo> gameListCopy = new ArrayList<>(gameInstance.gameList); // Use a copy to avoid ConcurrentModificationException
                for (GameInfo game : gameListCopy) {
                    if (game.port != gameInstance.serverPort) { // Skip pinging itself
                        try {
                            Registry registry = LocateRegistry.getRegistry(game.ipAddress, game.port);
                            GameInterface gameInstance = (GameInterface) registry.lookup("Game");
                            gameInstance.ping();
                            System.out.println("Ping to " + game.playerId + " was successful.");
                        } catch (Exception e) {
                            System.out.println("Failed to ping " + game.playerId + ": " + e.getMessage());
                            // Remove dead server from the list and notify Tracker
                            try {
                                Registry trackerRegistry = LocateRegistry.getRegistry(TRACKER_IP, TRACKER_PORT);
                                TrackerInterface tracker = (TrackerInterface) trackerRegistry.lookup("Tracker");
                                tracker.removeGame(game.playerId);
                            } catch (Exception ex) {
                                System.out.println("Failed to notify Tracker about dead server: " + ex.getMessage());
                            }
                        }
                    }
                }
            }
        }, 0, 2000);
    }
}
