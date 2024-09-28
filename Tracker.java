// Tracker class responsible for holding list of players and their IP addresses, 
// along with N & K initialization.

// converts objects to byte stream, easy storage / transmission - efficiency
// import java.io.Serializable; // not necessary as UnicastRemoteObject already has it.
import java.net.MalformedURLException;
// for remote method invocations for calling objects on other JVMs
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

// Tracker class created, used for RMI, can recieve remote calls.
// Tracker_Interface for abstraction
public class Tracker extends UnicastRemoteObject implements Tracker_Interface {

    // constructor - initializes port, n, k, creates empty list for serverList i.e. game instances
    public Tracker(Integer port, Integer n, Integer k) throws RemoteException, NotBoundException {
        this.port = port;
        this.n = n;
        this.k = k;

        this.serverList = new ArrayList<>();
    }

    private List<Game_Interface> serverList; // holds Game_Interface objects

    private Integer port; // port where tracker is running
    private Integer n; // number of players
    private Integer k; // number of treasures

    @Override // overrride used for overriding methods from interface
    public Integer getPort() throws RemoteException {
        return port; // return port where tracker is running
    }

    @Override
    public Integer getN() throws RemoteException {
        return n; // return number of players
    }

    @Override
    public Integer getK() throws RemoteException {
        return k; // return number of treasures
    }

    @Override
    public List<Game_Interface> getServerList() throws RemoteException {
        return serverList; // return player and address
    }

    //logic for joining game
    @Override
    public synchronized List<Game_Interface> joinGame(String host, int port, String playerName) throws RemoteException, MalformedURLException, NotBoundException {
        // primary - 1st player joining game
        // backup - 2nd player joining game

        Logger.info("playerName: " + playerName + " is joining game.");

        String url = new String("//" + host + ":" + port + "/" + playerName); // building URL for new player
        Logger.info("player url = " + url.toString()); // prints player URL

        /*
        Remote Object Lookup: looking up (via the url) remote object that represents a game on a server. 
        Remote Method Invocation: After looking up the game object, 'game' can be used to call methods defined 
        in Game_Interface from local machine, which will execute those methods on the remote server 
        where the actual game logic resides.
         */
        Game_Interface game = (Game_Interface) Naming.lookup(url); 

        if (serverList.size() == 0) { // if no other game instances i.e. no other players
            Logger.info(game.getName() + " is primary server");
            game.setprimary(true); // assign first player as primary
            serverList.add(game); // add the instance url to the server list
        } else {
            game.setprimary(false); // necessary for if multiple players joining at once, might have multiple primary by mistake
        }
        return serverList;
    }

    @Override // for setting n & k
    public void initializeGame(int n, int k) throws RemoteException { 
        this.n = n;
        this.k = k;
    }
    // prints current number of players along with who is primary & backup
    private void printCurrentServerState() throws RemoteException {
        Logger.info("Game State -> Server List Size = " + serverList.size());
        int i = 0;
        for (Game_Interface Game_Interface : serverList) {
            i++;
            try {
                // Player 1: playerName = sneha; isprimary = true; isbackup = false
                // Player 2: playerName = suryaansh; isprimary = false; isbackup = true
                // Player 3: playerName = David; isprimary = false; isbackup = false
                Logger.info("Player " + i + ": playerName = " + Game_Interface.getName() +
                        "; isprimary = " + Game_Interface.getIsprimary() + "; isbackup = " + Game_Interface.getIsbackup());
            } catch (Exception e){
            }
        }
    }

    @Override // for setting server list
    public synchronized List<Game_Interface> setServerList(List<Game_Interface> inputServerList) throws RemoteException {
        this.serverList = inputServerList;
        printCurrentServerState();
        return serverList;
    }

    private static void createTracker(Tracker_Interface tracker) throws RemoteException, NotBoundException, AlreadyBoundException, MalformedURLException {

        String url = new String("//localhost:" + tracker.getPort() + "/tracker"); // starting tracker at localhost
        Logger.info("Tracker URL: " + url.toString()); // for debugging

        //try and retry for 3 times (for crashes)
        try {
            Naming.rebind(url, tracker);
        } catch (Exception e) {
            try {
                Thread.sleep (200);
                Naming.rebind(url, tracker);
            } catch (Exception e2) {
                try {
                    Thread.sleep (200);
                    Naming.rebind(url, tracker);
                } catch (Exception e3) {
                    e3.printStackTrace();
                    Logger.exception(e3);
                    return;
                }
            }
        }

        Logger.info("TRACKER CREATED"); // on success
    }
    // main method
    public static void main(String[] args) throws RemoteException, NotBoundException, AlreadyBoundException, MalformedURLException {
        // get port, n, k (if given), else use defaults
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 1099;
        int n = args.length > 1 ? Integer.parseInt(args[1]) : 15;
        int k = args.length > 2 ? Integer.parseInt(args[2]) : 10;
        Tracker_Interface tracker = new Tracker(port, n, k);
        createTracker(tracker);
        tracker.initializeGame(n, k);
    }
}

