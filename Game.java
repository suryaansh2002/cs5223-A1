import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

public class Game extends UnicastRemoteObject implements GameInterface {
    
    private static final Logger logger = Logger.getLogger(Game.class.getName());
    private static final long serialVersionUID = 1L;

    private String host;
    private int port;
    private String playerID; // playerID is limited to 2 characters
    private TrackerInterface tracker;
    private List<GameInterface> games;
    private Map<GameInterface, String> playersList;
    private GameState gameState;
    private Thread gameInputThread;
    private Thread primaryPingThread;
    private Thread backupPingThread;
    private boolean gameStarted = false;
    private boolean primaryServer = false;
    private boolean backupServer = false;

    public Game(String host, int port, String playerID) throws RemoteException {
        this.host = host;
        this.port = port;
        if (playerID.length() < 2) {
            throw new IllegalArgumentException("playerID must be at least 2 characters long");
        }
        this.playerID = playerID.substring(0, 2);
        this.games =new ArrayList<>();
        this.playersList = new LinkedHashMap<>();
    }

    @Override
    public void ping() throws RemoteException {
        // ping the game
    }

    public GameInterface connectToGame(String host, int port, String playerID) throws RemoteException {
        try {
            return (GameInterface) Naming.lookup("rmi://" + host + ":" + port + "/" + playerID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setGameState(GameState gameState) throws RemoteException {
        this.gameState = gameState;
    }

    public void setPrimary(boolean primary) throws RemoteException {
        this.primaryServer = primary;
    }

    private GameInterface getPrimaryServer() throws RemoteException {
        if (this.primaryServer) {
            return this;
        }
        try {
            for (GameInterface game : this.games) {
                if (game != this && game.isPrimary()) {
                    return game;
                }
            }
            // if no primary server is found, update the games list and try again
            games = tracker.getGames();
            updatePlayersList();
            for (GameInterface game : this.games) {
                if (game != this && game.isPrimary()) {
                    return game;
                }
            }
            throw new RemoteException("Primary server not found");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isPrimary() throws RemoteException {
        return this.primaryServer;
    }

    public void setBackup(boolean backup) throws RemoteException {
        this.backupServer = backup;
    }

    public boolean isBackup() throws RemoteException {
        return this.backupServer;
    }

    public GameInterface getBackupServer() throws RemoteException {
        if (this.backupServer) {
            return this;
        }
        try {
            for (GameInterface game : this.games) {
                if (game != this && game.isBackup()) {
                    return game;
                }
            }
            // if no backup server is found, update the games list and try again
            games = tracker.getGames();
            updatePlayersList();
            for (GameInterface game : this.games) {
                if (game != this && game.isBackup()) {
                    return game;
                }
            }
            return null;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setBackupServer(GameState gameState) throws RemoteException, InterruptedException {
        if (this.isPrimary()) {
            updatePlayersList();
            for (GameInterface game : this.games) {
                try {
                    if (game != this && !game.isPrimary()) {
                        game.ping();
                        game.setBackupServer(gameState);
                        game.setBackup(true);
                        game.updateGameState(gameState);
                        game.updateGamesList(games);
                        games = tracker.getGames();
                        game.startBackupPingThread();
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // game is not reachable, remove the player
                    removePlayer(game.getPlayerID());
                }
            }

            games = tracker.getGames();
            updatePlayersList();
            if (games.size() >= 2) {
                setBackupServer(gameState);
            } else {
                logger.log(Level.INFO, "No backup server, only one player in the game");
            }
        }
    }

    public void promoteBackupServer(String primaryServerPlayerId) throws RemoteException, InterruptedException {
        if (this.isBackup()) {
            this.setPrimary(true);
            this.setBackup(false);

            games = tracker.getGames();
            //test if the primary server is still alive
            try {
                GameInterface primary = getPrimaryServer();
                primary.ping();
            } catch (RemoteException e) {
                e.printStackTrace();
                // if the primary server is not reachable, remove the player
                removePlayer(primaryServerPlayerId);
            }
            this.updatePlayersList();
            games = tracker.setGames(new ArrayList<>(games));

            this.startPrimaryPingThread();
            try{
                setBackupServer(gameState);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            if (this.backupPingThread != null) {
                this.backupPingThread.interrupt();
            }
            logger.log(Level.INFO, "Promoted to primary server");
        }
    }

    private synchronized void updatePlayersList() throws RemoteException {
        playersList.clear();

        for (GameInterface game : this.games) {
            try {
                // add the player to the list
                playersList.put(game, game.getPlayerID());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        games = new ArrayList<>(playersList.keySet());
        gameState.updatePlayersList(new ArrayList<>(playersList.values()));
        printPlayersList();
        tracker.setGames(games);
    }

    public String getPlayerID() throws RemoteException {
        return this.playerID;
    }

    public synchronized boolean addPlayer(String playerID) throws RemoteException {
        // if the player is not the primary server, return false
        if (!this.primaryServer) {
            return false;
        }
        // if the player is already in the game, return false
        if (this.playersList.containsValue(playerID)) {
            return false;
        }
        // if the player is not in the game, add the player to the game
        GameInterface game = null;
        try {
            game = connectToGame(host, port, playerID);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        games.add(game);
        updatePlayersList();

        if (games.size() == 1){
            initGameState();
        }
        gameState.addPlayer(playerID);
        if (games.size() == 2) {
            setBackup(true);
        }
        updatePlayersList();
        game.startGame(gameState);
        tracker.setGames(games);

        if (games.size() >= 2) {
            GameInterface backup = getBackupServer();
            backup.updateGamesList(games);
        } 
    
        return true;
    }

    private synchronized void removePlayer(String playerID) throws RemoteException, InterruptedException {
        for (GameInterface game : this.games) {
            if (game.getPlayerID().equals(playerID)) {
                try {
                    game.quitGame();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        }
    }

    private void printPlayersList() {
        logger.log(Level.INFO, "Current players: {0}", this.games.size());
        if (this.playersList.isEmpty()){
            logger.log(Level.INFO, "No players");
        } else {
            for (Map.Entry<GameInterface, String> entry : this.playersList.entrySet()) {
                logger.log(Level.INFO, "PlayerID: {0}", entry.getValue());
            }
        }
    }

    public void updateGamesList(List<GameInterface> games) throws RemoteException {
        this.games = games;
        updatePlayersList();
    }

    public void setTracker(TrackerInterface tracker) throws RemoteException, InterruptedException {
        this.tracker = tracker;
        // add the game to the tracker
        initGameState();
        this.games = tracker.registerGame(this.host, this.port, this.playerID);
        logger.log(Level.INFO, "Current number of players: {0}", this.games.size());
        // if the game is the primary server, start the game. Otherwise, get the game state from the primary server
        if (this.primaryServer) {
            initGameState();
            gameState.addPlayer(this.playerID);
            startGame(this.gameState);
        } else {
            try {
                // get the game state from the primary server, then add the player
                games = tracker.getGames();
                GameInterface primary = this.games.get(0);
                primary.addPlayer(this.playerID);
            } catch (RemoteException e) {
                // if not reachable, print an error message, then try again
                e.printStackTrace();
                Thread.sleep(100);
                try {
                    games = tracker.getGames();
                    GameInterface primary = this.games.get(0);
                    primary.addPlayer(this.playerID);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (getBackupServer() != null) {
            //print game summary
            GameUI.printGameSummary(gameState, playerID, getPrimaryServer().getPlayerID(), getBackupServer().getPlayerID());
        } else {
            //print game summary
            GameUI.printGameSummary(gameState, playerID, getPrimaryServer().getPlayerID(), "No backup server");
        }
        // start the game thread
        startGameThread();
        // if the game is the primary server, start the ping thread
        // if the game is the backup server, start the backup ping thread
        if (this.primaryServer) {
            startPrimaryPingThread();
        } else if (this.backupServer) {
            startBackupPingThread();
        }
    }


    private void initGameState() throws RemoteException {
        int n = this.tracker.getGridSize();
        int k = this.tracker.getNumTreasures();
        this.gameState = new GameState(n, k);
    }

    public synchronized void updateGameState(GameState gameState) throws RemoteException {
        this.gameState = gameState;
    }
    
    public synchronized void startGame(GameState gameState) throws RemoteException {
        this.gameState = gameState;
        if (this.gameState != null) {
            logger.log(Level.INFO, "Game started by {0}", this.playerID);
        }
        this.gameStarted = true;
    }

    public synchronized void quitGame() throws RemoteException, InterruptedException {
        this.gameStarted = false;
        UnicastRemoteObject.unexportObject(this, true);
        try {
            Naming.unbind("rmi://localhost:" + this.port + "/Game");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.gameInputThread != null) {
            this.gameInputThread.interrupt();
        }
        if (this.primaryPingThread != null) {
            this.primaryPingThread.interrupt();
        }
        if (this.backupPingThread != null) {
            this.backupPingThread.interrupt();
        }
        this.gameState.quitGame(this.playerID);
        this.tracker.setGames(games);
        logger.log(Level.INFO, "Game quit by {0}", this.playerID);
    }

    public synchronized void startGameThread() throws RemoteException{
        this.gameInputThread = new Thread() {
            public void run() {
                while (gameStarted) {
                    try {
                        movePlayerInput();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        this.gameInputThread.start();
    }

    public synchronized void startPrimaryPingThread() throws RemoteException {
        if (! this.primaryServer) {
            return;
        }
        this.primaryPingThread = new Thread() {
            public void run() {
                while (gameStarted && primaryServer) {
                    try {
                        pingAllPlayers();
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        this.primaryPingThread.start();
    }

    public synchronized void startBackupPingThread() throws RemoteException {
        if (!this.backupServer) {
            return;
        }
        this.backupPingThread = new Thread() {
            public void run() {
                while (gameStarted && backupServer) {
                    try {
                        pingPrimaryServer();
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        this.backupPingThread.start();
    }

    private void pingAllPlayers() throws RemoteException, InterruptedException {
        if (!this.isPrimary()) {
            return;
        }
        if (!this.gameStarted || this.gameState == null) {
            Thread.sleep(100);
            return;
        }
        if (games.size() == 1) {
            return;
        } 
        if (games.size() >= 2) {
            // set the second player as the backup server
            GameInterface backup = games.get(1);
            String backupPlayerID = backup.getPlayerID();
            try {
                backup.ping();
                if (!backup.isBackup()) {
                    setBackupServer(gameState);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                removePlayer(backupPlayerID);
                setBackupServer(gameState);
                return;
            }
        }
        if (games.size() > 2) {
            GameInterface backup = games.get(1);
            // ping the rest of the players
            for (int i = 2; i < games.size(); i++) {
                GameInterface game = games.get(i);
                try {
                    game.ping();
                } catch (RemoteException e) {
                    e.printStackTrace();
                    removePlayer(game.getPlayerID());
                    backup.setBackupServer(gameState);
                    backup.updateGamesList(games);
                    return;
                }
            }
        }
    }

    private void pingPrimaryServer() throws RemoteException, InterruptedException {
        if (!this.isBackup()) {
            return;
        }
        if (!this.gameStarted || this.gameState == null) {
            Thread.sleep(100);
            return;
        }
        GameInterface primary = getPrimaryServer();
        try {
            primary.ping();
        } catch (RemoteException e) {
            e.printStackTrace();
            removePlayer(primary.getPlayerID());
            primary.promoteBackupServer(this.playerID);
        }
    }

    public void movePlayerInput() throws InterruptedException, IOException {
        // if game is not started or the gamestate is null, return
        if (!gameStarted || gameState == null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
            return;
        }
        // get the player's input
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String input = "";
        do {
            try {
                logger.info("Enter a direction (up (4), down (2), left (1), right (3)), Refresh (0), or Quit (9): ");
                while (!reader.ready()) {
                    Thread.sleep(100);
                }
                input = reader.readLine();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
                return;
            }
        } while ("".equals(input));
        // process the player's input
        Direction move = Direction.getDirection(input);

        GameInterface primary = null;
        try {
            primary = getPrimaryServer();
        } catch (RemoteException e) {
            // if the primary server is not found, print an error message and try again
            e.printStackTrace();
            Thread.sleep(100);
            primary = getPrimaryServer();
        }
        // if the primary server is found, move the player
        if (primary != null) {
            try {
                GameState newGameState = primary.movePlayerDirection(this.playerID, move);
                this.gameState = newGameState;
            } catch (RemoteException e) {
                e.printStackTrace();
                // if the primary server is not reachable, promote a backup server to primary
                primary = getPrimaryServer();
                GameState newGameState = primary.movePlayerDirection(this.playerID, move);
                this.gameState = newGameState;
            }
        }
    }

    public synchronized GameState movePlayerDirection(String playerID, Direction move) throws RemoteException, InterruptedException {
        if (!this.isPrimary()){
            throw new RemoteException("Only the primary server can move players");
        }        
        // compute the new player position with the move
        Position newPosition = gameState.computeNewPosition(playerID, move);
        if (move == Direction.QUIT) {
            try {
                quitGame();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return gameState;
        } 

        // move the player to the new position
        gameState.movePlayerPosition(playerID, newPosition);

        // update the backup server
        GameInterface backup = getBackupServer();
        if (backup != null) {
            try{
                backup.updateGameState(gameState);
                backup.updateGamesList(games);
            } catch (RemoteException e) {
                e.printStackTrace();
                // if the backup server is not reachable, promote a new backup server
                setBackupServer(gameState);
            }
        }

        logger.log(Level.INFO, "Player {0} moved to {1}", new Object[]{playerID, newPosition});
        updatePlayersList();
        return gameState;
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            logger.log(Level.SEVERE, "Usage: java Game <host> <port> <playerID>");
            System.exit(1);
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String playerID = args[2];
        try {
            String trackerURL = "rmi://" + host + ":" + port + "/Tracker";
            TrackerInterface tracker = (TrackerInterface) Naming.lookup(trackerURL);
            Game game = new Game(host, port, playerID);
            String gameURL = "rmi://" + host + ":" + port + "/" + playerID;
            Naming.rebind(gameURL, game);
            // print names of all registered games
            for (String name : Naming.list("rmi://" + host + ":" + port + "/")) {
                logger.log(Level.INFO, "Game: {0}", name);
            }

            game.setTracker(tracker);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Game exception: {0}", e.toString());
            e.printStackTrace();
        }
    }
    
}
