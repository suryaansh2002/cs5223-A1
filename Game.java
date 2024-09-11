import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

interface GameInterface extends Remote {
    void ping() throws RemoteException;
    void notifyNewUser(GameInfo newGame) throws RemoteException;
}

public class Game extends UnicastRemoteObject implements GameInterface {
    private String playerId;
    private String trackerIP = "127.0.0.1"; // Assuming Tracker is always running on localhost
    private int trackerPort = 2000; // Fixed Tracker port
    private List<GameInfo> gameList;

    protected Game(String playerId, int port) throws RemoteException {
        super(port);  // Run Game instance on the specified port
        this.playerId = playerId;
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

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Game [port-number] [Player-ID]");
            System.exit(1);
        }

        int gamePort = Integer.parseInt(args[0]);
        String playerId = args[1];

        try {
            // Locate Tracker
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 2000);
            TrackerInterface tracker = (TrackerInterface) registry.lookup("Tracker");

            // Register this Game instance
            String ipAddress = java.net.InetAddress.getLocalHost().getHostAddress();
            Game gameInstance = new Game(playerId, gamePort);
            Registry gameRegistry = LocateRegistry.createRegistry(gamePort);
            gameRegistry.rebind("Game", gameInstance);

            List<GameInfo> gameList = tracker.registerGame(playerId, ipAddress, gamePort);
            gameInstance.gameList = gameList;

            System.out.println("Registered with Tracker. Current games:");
            for (GameInfo game : gameList) {
                System.out.println("Player: " + game.playerId + " at " + game.ipAddress + ":" + game.port);
            }

            // If this is the primary server, start pinging
            if (gameList.get(0).playerId.equals(playerId)) {
                System.out.println("I am the primary server.");
                startPinging(gameInstance.gameList);
            }

            // Keep the Game running
            while (true) {
                if (System.in.read() == '9') {
                    System.out.println("Game server is shutting down.");
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startPinging(List<GameInfo> gameList) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Pinging all game servers...");
                for (GameInfo game : gameList) {
                    try {
                        Registry registry = LocateRegistry.getRegistry(game.ipAddress, game.port);
                        GameInterface gameInstance = (GameInterface) registry.lookup("Game");
                        gameInstance.ping();
                    } catch (Exception e) {
                        System.out.println("Failed to ping " + game.playerId);
                    }
                }
            }
        }, 0, 2000);
    }
}
