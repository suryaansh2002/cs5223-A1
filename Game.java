import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class Game {
    private static final int PING_INTERVAL = 2000; // 2 seconds
    private static boolean isPrimary = false;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Game [port-number] [player-id]");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String playerID = args[1];

        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 2000);
            TrackerInterface tracker = (TrackerInterface) registry.lookup("Tracker");

            GameInfo gameInfo = tracker.registerGame(playerID, "localhost", port);
            System.out.println("Game registered with Tracker. N=" + gameInfo.getN() + ", K=" + gameInfo.getK());

            Map<String, GameInfo> gameMap = gameInfo.getGameMap();
            System.out.println("Current game map: " + gameMap);

            // Determine if this instance is the primary server
            isPrimary = (gameMap.size() == 1);

            // Start pinging other servers if this instance is the primary server
            if (isPrimary) {
                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new PingTask(gameMap), PING_INTERVAL, PING_INTERVAL);
            }

            // Keep the game running until user input (e.g., pressing 9)
            while (true) {
                int input = System.in.read();
                if (input == '9') {
                    System.out.println("Game stopped.");
                    System.exit(0);
                }
            }
        } catch (NotBoundException e) {
            System.err.println("Tracker not found: " + e.getMessage());
        } catch (RemoteException e) {
            System.err.println("RMI exception: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Game exception: " + e.getMessage());
        }
    }

    private static class PingTask extends TimerTask {
        private final Map<String, GameInfo> gameMap;

        PingTask(Map<String, GameInfo> gameMap) {
            this.gameMap = gameMap;
        }

        @Override
        public void run() {
            for (GameInfo gameInfo : gameMap.values()) {
                // Ping each game server and log the result
                System.out.println("Pinging " + gameInfo.getPlayerID() + " at " + gameInfo.getIpAddress() + ":" + gameInfo.getPort());
                // Here you can implement actual ping logic if needed
            }
        }
    }
}
