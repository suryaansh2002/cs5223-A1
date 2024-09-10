import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class Tracker implements TrackerInterface {
    private static final Map<String, GameInfo> gameMap = new HashMap<>();
    private static int N;
    private static int K;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Tracker [N] [K]");
            return;
        }

        N = Integer.parseInt(args[0]);
        K = Integer.parseInt(args[1]);

        try {
            Tracker tracker = new Tracker();
            TrackerInterface stub = (TrackerInterface) UnicastRemoteObject.exportObject(tracker, 0);

            Registry registry = LocateRegistry.createRegistry(2000);
            registry.rebind("Tracker", stub);

            System.out.println("Tracker started on port 2000 with N=" + N + ", K=" + K);

            while (true) {
                // Keep the tracker running
            }
        } catch (RemoteException e) {
            System.out.println("Port 2000 is already in use or another RMI error occurred.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Tracker exception: " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public synchronized GameInfo registerGame(String playerID, String ipAddress, int port) throws RemoteException {
        String key = ipAddress + ":" + port;
        if (!gameMap.containsKey(key)) {
            gameMap.put(key, new GameInfo(playerID, ipAddress, port));
            System.out.println("New game registered: " + key);
        }
        return new GameInfo(N, K, gameMap);
    }

    @Override
    public synchronized Map<String, GameInfo> getGameMap() throws RemoteException {
        return gameMap;
    }
}