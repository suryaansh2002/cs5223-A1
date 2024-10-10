
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import javax.swing.JFrame;


public class Game extends UnicastRemoteObject implements Game_Interface {

    // Variable storing the name of the player
    private String playerName;


    // Function to set the Game List Value
    @Override
    public void setGameListValue(List<Game_Interface> gameList) {
        this.gameList = gameList;
    }

    // a mapping of all the active game instances to their player name values
    private Map<Game_Interface, String> Game_InterfacePlayerNameMapping;

    // Boolean to check whether current instance of the game is the primary sevrer
    private Boolean isprimary = false;

    // Boolean to check whether current instance of the game is the backup sevrer
    private Boolean isbackup = false;

    // State of the Game with a particular player
    private GameState serverGameState;

    // List of active Games
    private List<Game_Interface> gameList;

    // Connected Tracker
    private Tracker_Interface tracker;

  // The thread responsible for prompting the user for input.
    private Thread inputUserThread;

    //  Thread  used by primary to ping all players
    private Thread primaryPingAllThread;

    //  Thread  used by backup server to ping primary server
    private Thread backupPingPrimaryThread;

    private Boolean gameStart = false;

    private Integer numOfStep = 0;

    private Boolean forceQuit = false;

    private String host;
    private int port;

    private GameGUI gameGUI;


    public Game(String host, int port, String playerName) throws RemoteException, AlreadyBoundException, NotBoundException {
        this.gameStart = false;

        this.gameList = new ArrayList<>();
        this.Game_InterfacePlayerNameMapping = new LinkedHashMap<>();
        this.serverGameState = null;

        this.inputUserThread = null;
        this.primaryPingAllThread = null;
        this.backupPingPrimaryThread = null;

        this.host = host;
        this.port = port;
        this.playerName = playerName;

        // If backup server is null Create a new instance of GameGUI with only primary server name.
        this.gameGUI = null;

        // // Set the window properties for the GUI
        // gameGUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // gameGUI.setSize(600, 600);  // Adjust window size as needed
        // gameGUI.setVisible(true);   // Show the GUI
    }



    // Main Function
    public static void main(String[] args)
            throws RemoteException, NotBoundException, AlreadyBoundException, InterruptedException, MalformedURLException, GameErrorException {
        // Get host and port from Command Line arguements
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        String port = args.length > 1 ? args[1] : "1099";

        // If player name provided: use that, else randomly generate a two character long name.
        Random r = new Random();
        String s1 = String.valueOf((char) (r.nextInt(26) + 'a'));
        String s2 = String.valueOf((char) (r.nextInt(26) + 'a'));
        String playerName = args.length > 2 ? args[2] : s1 + s2;
        findTrackerAndBindToRMI(host, port, playerName);
    }


    // function to find tracker and bind to rmi registery
    private static void findTrackerAndBindToRMI(String host, String port, String playerName)
            throws RemoteException, NotBoundException, InterruptedException, AlreadyBoundException, MalformedURLException, GameErrorException {
        String url = "//" + host + ":" + port + "/tracker";
        Logger.info("Finding and Connecting to Tracker at: " + url.toString());

        Tracker_Interface tracker = (Tracker_Interface) Naming.lookup(url);
        Logger.info("Tracker Found");

        Game game = new Game(host, Integer.valueOf(port), playerName);

        String url2 = "//" + host + ":" + port + "/" + playerName;
        
        Logger.info("New Player Binding: " + url2.toString());
        Naming.rebind(url2, game);

        game.connectGameAndTracker(tracker);
    }

    //  function to connect game to the tracker, join the game
    public void connectGameAndTracker(Tracker_Interface tracker) throws RemoteException, NotBoundException, MalformedURLException, GameErrorException, InterruptedException {
        this.tracker = tracker;
    
        joinGameFromTracker();
        
        joinGameFromPrimaryServer();
        
        startGameThread();

        if (isprimary) {
            this.startprimaryPingAllThread();
        } else if (isbackup) {
            this.startbackupPingPrimaryThread();
        }
    }



    // Function to initialize Game State and Try-retry to join the game from the tracker.[]
    @Override
    public synchronized void joinGameFromTracker() throws RemoteException, NotBoundException, MalformedURLException {
        initializeGameState();
        //try and retry for 3 times to join game from tracker to handle for crashes
        try {
            this.gameList = tracker.joinGame(host, port, playerName);
        } catch (Exception error) {
            try {
                this.gameList = tracker.joinGame(host, port, playerName);
            } catch (Exception error2) {
                try {
                    this.gameList = tracker.joinGame(host, port, playerName);
                } catch (Exception error3) {
                    error3.printStackTrace();
                    Logger.exception(error3);
                    return;
                }
            }
        }
    }


    private void joinGameFromPrimaryServer() throws RemoteException, MalformedURLException, NotBoundException, GameErrorException, InterruptedException {

        Logger.info("No. of Active Players:" + gameList.size());

        if (isprimary) {
            // IF current game server is primary server then intialize its game state, 
            // assign itself a random position on the board and start the game.
            initializeGameState();
            serverGameState.registerPlayer(playerName);
            startGame(serverGameState);

        } else {
            // if it is not a primary then add current player to primary servers game list
            // try and retry for 3 times to handle for crashes or unavailability of primary server
            try {
                Game_Interface primary = gameList.get(0);
                primary.primaryServerAddNewPlayer(this.playerName);
            } catch (Exception error) {
                Logger.exception(error);
                Thread.sleep(500);
                try {
                    gameList = tracker.getTrackerServerList();
                    Game_Interface primary = gameList.get(0);
                    primary.primaryServerAddNewPlayer(this.playerName);
                } catch (Exception error2) {
                    Logger.exception(error2);
                    Thread.sleep(500);
                    try {
                        gameList = tracker.getTrackerServerList();
                        Game_Interface primary = gameList.get(0);
                        primary.primaryServerAddNewPlayer(this.playerName);
                    } catch (Exception error3) {
                        error3.printStackTrace();
                    }
                }


            }
        }

        if (getbackup() == null) {
            // If backup server is null Create a new instance of GameGUI with only primary server name.
            this.gameGUI = new GameGUI(serverGameState, playerName, getprimary().getName(), "");

            // Set the window properties for the GUI
            gameGUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            gameGUI.setSize(600, 600);  // Adjust window size as needed
            gameGUI.setVisible(true);   // Show the GUI
        }
        else {
            // Create a new instance of GameGUI with both primary and backup names
            this.gameGUI = new GameGUI(serverGameState, playerName, getprimary().getName(), getbackup().getName());

            // Set the window properties for the GUI
            gameGUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            gameGUI.setSize(600, 600);  // Set the window size (adjust as needed)
            gameGUI.setVisible(true);   // Display the window
        }
            
    }

    // Initializing Game State using N and K from tracker
    private void initializeGameState() throws RemoteException {
        int n = tracker.getgridDimensions();
        int k = tracker.getNumOfTreasures();

        // Create new instance of GameState
        serverGameState = new GameState(n, k);
    }
    


    // Function to be used by primary server to add a new player to its game list,
    // update the game interface - player id mapping to include new player
    // Update Tracker's server list
    // Update Back up server's game list if needed.
    @Override
    public synchronized boolean primaryServerAddNewPlayer(String playerName) throws RemoteException, MalformedURLException, NotBoundException, InterruptedException {

        //if the player is not primary, it means tracker call wrong gamer
        if (!isprimary) {
            Logger.error("Only primary server can add new player. Called wrong server by- " + playerName);
            return false;
        }

        // if the player already exists, return
        if (Game_InterfacePlayerNameMapping.containsValue(playerName)) {
            Logger.error("Player with name " + playerName + " already exists. Please choose a different name.");
            return false;
        }
        
        Game_Interface game = null;
        
        try {
            // Primary server trying to connect to new game joining
            game = connectToGame(host, port, playerName);
        } catch (Exception ex) {
            Logger.exception(ex);
            return false;
        }
        
        gameList.add(game);

        updateGameToPlayerNameMapping();

        if (gameList.size() == 1) { // Current Game is Primary Server
            Logger.info("Intializing Primary Game, name:" + playerName);
            initializeGameState();
        }
        // Assign random position to new player, set score to 0
        serverGameState.registerPlayer(playerName);

        if (gameList.size() == 2) { // If only two servers in game make current server as backup
            game.setbackup(true);
        }

        updateGameToPlayerNameMapping();

        tracker.setTrackerServerList(gameList); // Update Tracker's server list.


        game.startGame(serverGameState);

       
        if (gameList.size() >= 2) { // If more than 2 servers in game, to update server list 
            Game_Interface backup = gameList.get(1);
            backup.updateServersGameList(gameList);
        }

        return true;
    }

    // Function to assign a new back up server in case the primary server fails.
    @Override
    public synchronized void assignNewbackupServer(GameState gameState) throws RemoteException {

        //  To ensure that only Primary Server can call it
        if (isprimary) {
            updateGameToPlayerNameMapping();

            // Iterate through all players in game list by pinging
            // the 1st player responses will be the new backup
            
            int i = 1;

            while (i < gameList.size()) {
                Game_Interface Game_Interface = gameList.get(i);
                String Game_Interface_Name = Game_Interface.getName();
                try {
                    Game_Interface.ping();
                    
                    // Ping Successfull

                    updateGameToPlayerNameMapping();
                    
                    Game_Interface.updateGameState(serverGameState);
                    Game_Interface.updateServersGameList(gameList);

                    // New Backup assigned
                    Game_Interface.setbackup(true);
                    
                    // Tracker Game list updated
                    gameList = tracker.setTrackerServerList(gameList);
                    
                    updateGameToPlayerNameMapping();

                    // Newly assigned Backup server to begin pinging primary server.
                    Game_Interface.startbackupPingPrimaryThread();

                    return;
                } catch (RemoteException e) {
                    Logger.exception(e);
                    // Removing dead server which primary was unable to ping
                    Logger.error("Played pinged by primary is dead, Name:" + Game_Interface_Name);
                    removeDeadGameServer(Game_Interface_Name);
                    gameList = tracker.getTrackerServerList();
                    gameList.remove(Game_Interface);
                    gameList = tracker.setTrackerServerList(new ArrayList<>(gameList));
                    updateGameToPlayerNameMapping();
                }
            }

            // If none of the players replied, to get updated list from tracker
            gameList = tracker.getTrackerServerList();
            updateGameToPlayerNameMapping();

            if (gameList.size() > 2) {
                assignNewbackupServer(gameState);
            } else {
                Logger.info("One player present in game, no backup server.");
            }
        }
    }

    // Function to run game thread that will take player input moves
    public synchronized void startGameThread() throws RemoteException {

        Logger.info("Starting Game Thread. Player Name: " + playerName);
        this.inputUserThread = new Thread() {
            public void run() {
                while (!forceQuit) {
                    try {
                        makePlayerMoveInput();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Logger.exception(e);
                    }
                }
            }
        };
        this.inputUserThread.start();
    }

    // Function to ping a particular server to check whether it is still alive.
    @Override
    public void ping() throws RemoteException {
        return;
    }

    // Update server list of a particular game instance.    
    @Override
    public void updateServersGameList(List<Game_Interface> gameList) throws RemoteException {
        this.gameList = gameList;
        this.updateGameToPlayerNameMapping();
        Logger.info("Updating Server's Game List. Player Name: " + playerName);
    }

    // Function to print list of all active players.
    private void printPlayerNames() {
        Logger.info("Total Active Players:" + gameList.size());
        if (Game_InterfacePlayerNameMapping.isEmpty()) {
            Logger.info("No active players!!");
            return;
        }

        Set<Game_Interface> keys = Game_InterfacePlayerNameMapping.keySet();
        int i = 0;
        for (Game_Interface Game_Interface : keys) {
            i++;
            Logger.info("Player " + i + " Name is: " + Game_InterfacePlayerNameMapping.get(Game_Interface));
        }
    }

    // Function to update the Game_InterfacePlayerNameMapping variable using the gameList
    private synchronized void updateGameToPlayerNameMapping() throws RemoteException {

        Game_InterfacePlayerNameMapping.clear();
        Logger.info("Updating updateGameToPlayerNameMapping");
        for (Game_Interface Game_Interface : gameList) {
            try {
                Game_InterfacePlayerNameMapping.put(Game_Interface, Game_Interface.getName());
            } catch (Exception e) {
                //e.printStackTrace();
                Logger.exception(e);
            }
        }

        gameList = new ArrayList<>(Game_InterfacePlayerNameMapping.keySet());
        serverGameState.updatePlayerList(new ArrayList<>(Game_InterfacePlayerNameMapping.values()));
        // Logger 
        printPlayerNames();
        // Updating  Trackers Server list
        tracker.setTrackerServerList(gameList);
    }

    // Function to be used by Primary server to ping all other game players to check whether they are alive.
    private void primaryPingAllPlayers() throws RemoteException, GameErrorException, InterruptedException {

        if (!gameStart || serverGameState == null) {
            Thread.sleep(100);
            return;
        }

        // To make sure only primary server pings all the players.
        if (!isprimary) {
            Logger.error("Wrong primary to ping all players!!! player name = " + playerName);
            return;
        }


        if (gameList.size() == 1) { // Only Primary Server
            return;
        }


        if (gameList.size() >= 2) {
            Game_Interface backup = gameList.get(1);
            String backupId = Game_InterfacePlayerNameMapping.get(backup);
            try { // Handle case in which Backup server had failed, to assign new back up server
                backup.ping();
                if (backup.getIsbackup() == false) {
                    assignNewbackupServer(serverGameState);
                }
                try {
                    Thread.sleep(100); // ping interval for primary server
                } catch (InterruptedException e) {
                    Logger.exception(e);
                    Logger.info("In pinging backup: primaryPingAllPlayers() thread interrupted during sleep.");
                }
            } catch (RemoteException e) {
                Logger.exception(e);
                Logger.info("Ping to Backup server failed. Backup " + backupId + "is down. ");

                removeDeadGameServer(backupId);
                this.gameList.remove(backup);
                updateGameToPlayerNameMapping();

                assignNewbackupServer(serverGameState);

                return;
            } catch (Exception e2) {
                Logger.exception(e2);
                return;
            }
        }

        primaryRegularPing();
    }


    private void primaryRegularPing() throws RemoteException {
        if (gameList.size() > 2) { // For regular (non primary/backup) servers
            Game_Interface backup = gameList.get(1);
            Iterator<Game_Interface> iter = gameList.subList(2, gameList.size()).iterator();

            while (iter.hasNext()) {
                // Iterating through all the Game interfaces (keys) of the mapping
                Game_Interface Game_Interface = iter.next();
                String gameId = Game_InterfacePlayerNameMapping.get(Game_Interface);

                try {
                    Game_Interface.ping();
                    try {
                        Thread.sleep(100); // primary ping interval
                    } catch (InterruptedException e) {
                        Logger.exception(e);
                        Logger.info("In pinging regular server: primaryPingAllPlayers() thread interrupted during sleep.");
                    }
                } catch (RemoteException e) {
                    Logger.exception(e);
                    Logger.info("Ping to Player failed! Dead Player's name: " + gameId);
                    Logger.info("Player " + gameId + " is down.");

                    removeDeadGameServer(gameId);
                    iter.remove();
                    updateGameToPlayerNameMapping();

                    backup.setServerGameState(serverGameState);
                    backup.updateServersGameList(gameList);

                    return;
                } catch (Exception e2) {
                    Logger.exception(e2);
                    return;
                }
            }
        }
    }

    // Function for backup server to ping primary server to check whether it is still alive or not.
    private void backupPingsPrimary() throws RemoteException, GameErrorException, InterruptedException {

        // To ensure that this function can be called only by Backup server
        if (!isbackup) {
            Logger.error("Only Backup server can ping primary. Player Name: " + playerName);
            return;
        }
        if (gameList.size() >= 1) {
            Game_Interface primary = gameList.get(0);
            String primaryServerName = Game_InterfacePlayerNameMapping.get(primary);
            try {
                if (primary != this) {
                    primary.ping();
                    try {
                        Thread.sleep(100); //backup ping interval
                    } catch (InterruptedException error) {
                        Logger.exception(error);
                        Logger.info("backupPingsPrimary(): Thread interrupted in sleep.");
                    }
                }
            } catch (RemoteException error2) {
                Logger.exception(error2);
                Logger.info("Ping to Primary Failed, Backup becoming Primary!! Player Name: " + playerName);

                removeDeadGameServer(primaryServerName);
                backupBecomeprimary(primaryServerName);

                return;
            }
        }
    }
    // Function to start primary pinging thread
    public synchronized void startprimaryPingAllThread() throws RemoteException {
        if (forceQuit) {
            return;
        }
        Logger.info("Start primaryPingAllThread. Player Name: " + playerName);

        // To ensure only Primary server calls this function.
        if (!isprimary) {
            Logger.error("Only Primary server can use this thread. " + playerName);
            return;
        }

        // run thread
        this.primaryPingAllThread = new Thread() {
            public void run() {
                while (!forceQuit && isprimary) {
                    try {
                        primaryPingAllPlayers();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Logger.exception(e);
                    }
                }
            }
        };

        this.primaryPingAllThread.start();
    }

    // Function to start backup pinging primary thread
    @Override
    public synchronized void startbackupPingPrimaryThread() throws RemoteException {
        if (forceQuit) {
            return;
        }

        Logger.info("Start backupPingPrimaryThread. Player Name: " + playerName);

        // To ensure only Backup server calls this function.
        if (!isbackup) {
            Logger.error("Only Backup server can use this thread. " + playerName);
            return;
        }

        // run thread
        this.backupPingPrimaryThread = new Thread() {
            public void run() {
                while (!forceQuit && isbackup) {
                    try {
                        backupPingsPrimary();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Logger.exception(e);
                    }
                }
            }
        };

        this.backupPingPrimaryThread.start();
    }
    // Function to update Game State of Game Server
    @Override
    public synchronized void updateGameState(GameState gameState) throws RemoteException {
        this.serverGameState = gameState;
        Logger.info("Update Game State of Player: " + playerName);
    }



    // Function to connect to game server instance
    public static Game_Interface connectToGame(String host, int port, String playerName) throws RemoteException, NotBoundException, InterruptedException, MalformedURLException {

        String url = "//" + host + ":" + port + "/" + playerName;

        return (Game_Interface) Naming.lookup(url);
    }


    // Function to set server game state and to start game.
    @Override
    public synchronized void startGame(GameState gameState) throws RemoteException {
        this.serverGameState = gameState;
        this.gameStart = true;
    }

    // Function to handle player input to make a make a move
    private void makePlayerMoveInput() throws InterruptedException, IOException, GameErrorException, NotBoundException {


        // If Game start is false (game has not yet registered with tracker and primamry server) player cannot make a move
        if (!gameStart || serverGameState == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Logger.exception(e);
                Logger.info("Thread Interruped in Sleep");
            }
            return;
        }
        // Reading in Input
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String input = "";
        do {
            try {
                Logger.info("Player " + playerName + ", Enter move- 0: Refresh, 1: West, 2: South, 3: East, 4: North, 9: Quit Game :");
                if (forceQuit) {
                    return;
                }
                while (!br.ready()) {
                    Thread.sleep(200);
                }
                input = br.readLine();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Logger.exception(e);
                return;
            }
        } while ("".equals(input));
        String move = input.replaceAll("\n", "");

        // Mapping move to direction
        Direction direction = Direction.getDirection(move);

        Logger.info("Direction:" + direction.getDirection() + ", Player Name: " + playerName);
       
       // Sending move to primary server to actually make the move.
        Game_Interface primary = playerMoveCallPrimary();

        playermovePlayer(direction, primary);
    }




    private void playermovePlayer(Direction direction, Game_Interface primary)
            throws GameErrorException, MalformedURLException, NotBoundException, InterruptedException, RemoteException {
        try {
            // Primary Server Making the move
            GameState gameState = primary.move(this.playerName, direction, numOfStep);
            serverGameState = gameState;
        } catch (RemoteException error) {
            // Primary Server died while making move
            // Waiting for new primary to be ready before making move again.
            error.printStackTrace();
            Logger.exception(error);
            Logger.info("makePlayerMoveInput(): Primary Server Died while making move. No. of active players: " + gameList.size());

            Thread.sleep(500); // wait for new primary to come online;]

            try {
                primary = getprimary();
            } catch (RemoteException e2) {
                Logger.info("makePlayerMoveInput(): Primary Server is still dead. No. of active players: " + gameList.size());
                Thread.sleep(500);
                try {
                    primary = getprimary();
                    // make a move
                    GameState gameState = primary.move(this.playerName, direction, numOfStep);
                    serverGameState = gameState;
                } catch (RemoteException e3) {
                    e3.printStackTrace();
                    Logger.exception(e3);
                    Logger.info("makePlayerMoveInput(): Primary Server is still dead. Failure.  No. of active players: " + gameList.size());
                }
            }
        }

        if (direction.getDirection() != 9) {
            if (getbackup() == null) {
                this.gameGUI.updateGameState(serverGameState,primary.getName(),""); 
            }
            else {
                this.gameGUI.updateGameState(serverGameState, primary.getName(), getbackup().getName()); 
            }
            Logger.info("Move made:" + direction.getDirection() + ", Player Name:" + playerName);
            numOfStep++;
        }
    }

    @Override
    public synchronized GameState move(String playerName, Direction direction, int numOfStep) throws RemoteException, GameErrorException, MalformedURLException, NotBoundException {
        Logger.info("Making move. Player Name: " + playerName);
        
        // Because only primary server can make a move
        if (this.isprimary == false) {
            throw new GameErrorException("Non-Primary Server cannot make move");
        }

        if (direction == Direction.QUIT) {
            try {
                Logger.info("move(): Player " + playerName + " decided to quit the game. Goodbye!");
                leaveGame(playerName);
            } catch (Exception error) {
                error.printStackTrace();
                Logger.exception(error);
            }
            return serverGameState;
        }

        Logger.info("move(): Player Name: " + playerName + " trying to make move. Direction:" + direction + ".  Primary Server:" + this.playerName);

        serverGameState.movePlayer(playerName, direction, numOfStep);

        // Move made

        Game_Interface backup = this.getbackup();

        // If backup server exisists then its game state needs to be updated with the latest 
        // game state of the move made.
        
        if (backup != null) {
            try {
                backup.updateGameState(serverGameState);
                backup.updateServersGameList(gameList);
            } catch (RemoteException e) {
                // If unable to update backup game state -> backup is down, elect new backup
                Logger.exception(e);
                assignNewbackupServer(serverGameState);
            }
        }

        Logger.info("move(): Player Name: " + playerName + "made a move in direction- " + direction + " Primary game server name: " + this.playerName);
        updateGameToPlayerNameMapping();
        return serverGameState;
    }

    private Game_Interface playerMoveCallPrimary() throws InterruptedException, RemoteException {
        Game_Interface primary = null;
        try {
            primary = getprimary();
        } catch (Exception e) {
            Thread.sleep(500); // In case primary server is dead when player makes a move 
            // Wait till backup becomes primary, once primary is availabl, get new primary 
            gameList = tracker.getTrackerServerList();
            updateGameToPlayerNameMapping();

            try {
                // original primary down, try to get new primary
                primary = getprimary();
            } catch (Exception ex) {
                e.printStackTrace();
                Logger.exception(e);
                Logger.error(playerName + " unable to get Primary Server");
            }
        }
        return primary;
    }

    // Returns Primary Game Server Interface
    private Game_Interface getprimary() throws GameErrorException, RemoteException, InterruptedException {
        if (isprimary) {
            return this;
        }

        try {
            for (int i = 0; i < gameList.size(); i++) {
                if (gameList.get(i).getIsprimary())
                    return gameList.get(i);
            }

            gameList = tracker.getTrackerServerList();
            updateGameToPlayerNameMapping();
            for (int i = 0; i < gameList.size(); i++) {
                if (gameList.get(i).getIsprimary()) {
                    return gameList.get(i);
                }
            }
            throw new GameErrorException("Primary Server Not Found!");
        } catch (Exception e) {
            Logger.exception(e);
            Thread.sleep(500);
            try {
                gameList = tracker.getTrackerServerList();
                updateGameToPlayerNameMapping();
                return getprimary();
            } catch (Exception error) {
                error.printStackTrace();
                gameList = tracker.getTrackerServerList();
                updateGameToPlayerNameMapping();
                return getprimary();
            }
        }
    }

    // Returns Backup Game Server Interface
    private Game_Interface getbackup() throws GameErrorException, RemoteException {
        if (isbackup) {
            return this;
        }
        try {
            return fetchBackupServer();
        } catch (Exception error) {
            //wait try again to get back up server
            try {
                Thread.sleep(300);
            } catch (InterruptedException error2) {
                Logger.info("getbackup(): Thread interrupted during sleep.");
            }
            Logger.info("getbackup(): Get updated user list from Tracker and try to find back up server");

            try {
                gameList = tracker.getTrackerServerList();
                updateGameToPlayerNameMapping();
                fetchBackupServer();
            } catch (Exception error2) {
                error2.printStackTrace();
                Logger.exception(error2);
            }
        }
        return null;
    }

    private Game_Interface fetchBackupServer() throws RemoteException, GameErrorException {
        this.gameList = tracker.getTrackerServerList();
        if (gameList.size() >= 2 && gameList.get(1).getIsbackup())
        {
            return gameList.get(1);
        } else {
            // try to retrieve the list from Tracker and retry
            Logger.info("getbackup(): Cant get backup from game list. Fetch the updated list from tracker and retry.");
            gameList = tracker.getTrackerServerList();
            updateGameToPlayerNameMapping();
            if (gameList.size() >= 2 && gameList.get(1).getIsbackup()) {
                return gameList.get(1);
            } else {
                throw new GameErrorException("Backup Server not found");
            }
        }
    }

    // Function to handle Failure of primary server by making current backup server to primary.
    
    @Override
    public synchronized void backupBecomeprimary(String oldprimaryServerName) throws RemoteException {
    // Only to be called by current Backup server
        if (isbackup) {
            isbackup = false;
            isprimary = true;

            gameList = tracker.getTrackerServerList(); 
            Logger.info("backupBecomeprimary() -  No. of active Players:  " + gameList.size()
                    + " Old Primary Server Name: " + oldprimaryServerName);

            try {
                if (oldprimaryServerName != null) {
                    Game_Interface oldprimary = connectToGame(host, port, oldprimaryServerName);
                    gameList.remove(oldprimary);
                }
            } catch (Exception ex) {
                Logger.info("Original primary is already removed, can ignore this exception");
            }
            updateGameToPlayerNameMapping();
            gameList = tracker.setTrackerServerList(new ArrayList<>(gameList));


            // Since now this is the primary thread, it will ping all the servers
            
            this.startprimaryPingAllThread();

            try {
                // Backup has become primary so new backup needs to be assigned
                assignNewbackupServer(serverGameState);
            } catch (Exception error) {
                error.printStackTrace();
                Logger.exception(error);
                Logger.error("Unable to make new backup servera");
            }

            // To stop old backup server thread which was pinging the old primary
            if (this.backupPingPrimaryThread != null) {
                Logger.info("To stop old backup server thread which was pinging the old primary Player Name: " + playerName + " isprimary: " + isprimary + " isbackup: " + isbackup);
                this.backupPingPrimaryThread.interrupt();
            }
        }
    }

    // Returns name of player
    @Override
    public String getName() throws RemoteException {
        return playerName;
    }

    // Function to check whether  a particular server is primary or not.
    @Override
    public boolean getIsprimary() throws RemoteException {
        return isprimary;
    }

    // Function to set a  game server as primary
    @Override
    public void setprimary(Boolean primary) throws RemoteException {
        isprimary = primary;
    }

    // Function to check whether  a particular server is backup or not.
    @Override
    public boolean getIsbackup() throws RemoteException {
        return isbackup;
    }

    // Sets backup server
    @Override
    public void setbackup(Boolean backup) throws RemoteException {
        isbackup = backup;
    }

    // Function to set Game state of server
    @Override
    public void setServerGameState(GameState serverGameState) throws RemoteException {
        this.serverGameState = serverGameState;
    }

    // Function to set gameStart variable
    @Override
    public void setGameStart(Boolean gameStart) throws RemoteException {
        this.gameStart = gameStart;
    }


    // Function to return game state of a particular game server.
    @Override
    public GameState getServerGameState() throws RemoteException {
        return serverGameState;
    }


    private void unbind(String url) throws RemoteException {
        try {
            Naming.unbind(url);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.exception(e);
        }
    }

    private void leaveGame(String playerName) throws GameErrorException, RemoteException, MalformedURLException, NotBoundException, InterruptedException {
        Logger.info("Player: " + playerName + " leaving game,");
        if (!isprimary) {
            throw new GameErrorException("leaveGame(): Primary Server only should handle leaving of games.");
        }

        Game_Interface game = connectToGame(host, port, playerName);

        if (game.getIsprimary()) { // Primary server leaving the game, backup to be made new primary
            leaveGetNewPrimary(playerName, game);
        } else if (game.getIsbackup()) {
            // Backup server leaving the game, mew backup to be assigned
            leaveAssignNewBackup(playerName, game);
        } else {
            regularLeaveGame(playerName, game);
        }

        game.quit();
    }



    private void regularLeaveGame(String playerName, Game_Interface game) throws RemoteException {
        // A regular player leaving the game
        Logger.info("leaveGame(): Normal player " + playerName + "quiting");

        // Remove player from Game state
        this.serverGameState.playerQuit(playerName);

        // Remove Player from Game List
        this.gameList.remove(game);

        // Update Mapping
        this.updateGameToPlayerNameMapping();

        // Update Server list of tracker
        tracker.setTrackerServerList(this.gameList);
    }



    private void leaveAssignNewBackup(String playerName, Game_Interface game) throws RemoteException {
        Logger.info("leaveGame(): Backup server " + playerName + " leaving the game, new backup to be assigned");

            this.serverGameState.playerQuit(playerName);
            this.gameList.remove(game);
            this.updateGameToPlayerNameMapping();
            tracker.setTrackerServerList(this.gameList);

            this.assignNewbackupServer(serverGameState);
    }



    private void leaveGetNewPrimary(String playerName, Game_Interface game) throws GameErrorException, RemoteException {
        Logger.info("leaveGame(): Primary server " + playerName + " leaving the game, backup to be made new primary");

        Game_Interface newprimary = this.getbackup();
        if (newprimary != null) {

            // Removes Player from Game State
            this.serverGameState.playerQuit(playerName);

            // Removes Player from Game List
            this.gameList.remove(game);

            // Update mapping to remove player
            this.updateGameToPlayerNameMapping();

            // Updating Server list with tracker
            tracker.setTrackerServerList(this.gameList);

            try {
                // make back up to primary
                newprimary.backupBecomeprimary(this.playerName);
            } catch (Exception error) {
                error.printStackTrace();
                Logger.exception(error);
            }
        } else {
            if (this.gameList.size() == 1) {
                // No backup server, primary server also leaving, no active players left
                this.serverGameState.playerQuit(playerName);
                this.gameList.clear();
                this.updateGameToPlayerNameMapping();
                tracker.setTrackerServerList(this.gameList);
            }
        }
    }


    // Function to handle the case when a player suddenly fails without pressing 9 to quity
    private synchronized void removeDeadGameServer(String playerName) throws RemoteException {
        // Primary Game Server removes Failed Player and update game state.
        serverGameState.playerQuit(playerName);
    }

    // Function called by a player when he is quiting the game to unbind from the rmi
    @Override
    public void quit() throws RemoteException {
        try {
            Logger.info("quit(): Plater Name:" + playerName);
            UnicastRemoteObject.unexportObject(this, true);

            String url = "//" + host + ":" + port + "/" + playerName;
            Logger.info("quit(): Unbinding Player's lookup URL " + url);

            unbind(url);

            Logger.info("quit(): Player QUIT successfully, player ID: " + playerName);
            if (this.inputUserThread != null) {
                this.inputUserThread.interrupt();
            }

            if (this.primaryPingAllThread != null) {
                this.primaryPingAllThread.interrupt();
            }

            if (this.backupPingPrimaryThread != null) {
                this.backupPingPrimaryThread.interrupt();
            }

            forceQuit = true;
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.exception(e);
        }
    }
}

