// Tracker class responsible for holding list of players and their IP addresses, 
// along with N & K initialization.


import java.net.MalformedURLException;
// for remote method invocations for calling objects on other JVMs
import java.util.List;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;

// Tracker class created, used for RMI, can recieve remote calls.
// Tracker_Interface for abstraction
public class Tracker extends UnicastRemoteObject implements Tracker_Interface {

    // constructor - initializes port, n, k, creates empty list for serverList i.e. game instances
    public Tracker(Integer trackerPort, Integer gridDimensions, Integer numOfTreasures) throws RemoteException, NotBoundException {
        this.gridDimensions = gridDimensions;
        this.serverList = new ArrayList<>();
        this.numOfTreasures = numOfTreasures;
        this.trackerPort = trackerPort;

    }


    private Integer numOfTreasures; // number of treasures
    private Integer trackerPort; // port where tracker is running
    private List<Game_Interface> serverList; // holds Game_Interface objects
    private Integer gridDimensions; // Grid size

       
    @Override
    public List<Game_Interface> getTrackerServerList() throws RemoteException {
        return serverList; // return player and address
    }

   
    @Override
    public Integer getNumOfTreasures() throws RemoteException {
        return numOfTreasures; // return number of treasures
    }



    @Override // for setting n & k
    public void initializeGame(int gridDimensions, int numOfTreasures) throws RemoteException { 
        this.gridDimensions = gridDimensions;
        this.numOfTreasures = numOfTreasures;
    }

 
    //logic for joining game
    @Override
    public synchronized List<Game_Interface> joinGame(String ipAddress, int port, String playerName) throws MalformedURLException, NotBoundException, RemoteException {
        Logger.info("playerName: " + playerName + " is joining game.");
        // throw error if playerName is already in use
        for (Game_Interface game : serverList) {
            if (game.getName().equals(playerName)) {
                Logger.error("Player with name " + playerName + " already exists. Please choose a different name.");
                throw new RemoteException("Player with name " + playerName + " already exists. Please choose a different name.");
            }
        }

        String url = new String("//" + ipAddress + ":" + port + "/" + playerName); // building URL for new player

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

    // prints current number of players along with who is primary & backup
    private void logServerState() throws RemoteException {
        Logger.info("No. of active players: " + serverList.size());
        int i = 0;
        for (Game_Interface Game_Interface : serverList) {
            i++;
            try {
                Logger.info(
                    "Player " + i + ": playerName = " + Game_Interface.getName() +
                     "; Primary = " + Game_Interface.getIsprimary() + "; Backup = " + Game_Interface.getIsbackup());
            } catch (Exception e){
                // catch exception but continue
            }
        }
    }
    private static void createTracker(Tracker_Interface tracker) throws RemoteException, NotBoundException, AlreadyBoundException, MalformedURLException {
        try {
            Naming.rebind("//localhost:" + tracker.getTrackerPort() + "/tracker", tracker);
        } catch (Exception e) {
            try {
                Thread.sleep (200);
                Naming.rebind("//localhost:" + tracker.getTrackerPort() + "/tracker", tracker);
            } catch (Exception e2) {
                try {
                    Thread.sleep (200);
                    Naming.rebind("//localhost:" + tracker.getTrackerPort() + "/tracker", tracker);
                } catch (Exception e3) {
                    e3.printStackTrace();
                    try {
                        Thread.sleep (200);
                        Naming.rebind("//localhost:" + tracker.getTrackerPort() + "/tracker", tracker);
                    } catch (Exception e4) {
                        e3.printStackTrace();
                        Logger.exception(e4);
                        return;
                    }
                }
            }
        }

        Logger.info("Tracker Created Successfully! "); // on success
    }


    @Override
    public Integer getgridDimensions() throws RemoteException {
        return gridDimensions; // return number of players
    }


    @Override // overrride used for overriding methods from interface
    public Integer getTrackerPort() throws RemoteException {
        return trackerPort; // return port where tracker is running
    }

    @Override // for setting server list
    public synchronized List<Game_Interface> setTrackerServerList(List<Game_Interface> inputServerList) throws RemoteException {
        this.serverList = inputServerList;
        logServerState();
        return serverList;
    }

    // main method
    public static void main(String[] args) throws RemoteException, NotBoundException, AlreadyBoundException, MalformedURLException {
        
        if(args.length<3){
            Logger.error("Need Port, N, and K to run tracker!");
        }
        
        int trackerPort = Integer.parseInt(args[0]);
        int gridDimensions = Integer.parseInt(args[1]);
        int numOfTreasures = Integer.parseInt(args[2]);
       
        Tracker_Interface tracker = new Tracker(trackerPort, gridDimensions, numOfTreasures);
        
        createTracker(tracker);
        
        tracker.initializeGame(gridDimensions, numOfTreasures);
    }
}

