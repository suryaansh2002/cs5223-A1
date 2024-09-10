import java.io.Serializable;
import java.util.Map;

public class GameInfo implements Serializable {
    private final String playerID;
    private final String ipAddress;
    private final int port;
    private final int N;
    private final int K;
    private final Map<String, GameInfo> gameMap;

    public GameInfo(String playerID, String ipAddress, int port) {
        this.playerID = playerID;
        this.ipAddress = ipAddress;
        this.port = port;
        this.N = 0;
        this.K = 0;
        this.gameMap = null;
    }

    public GameInfo(int N, int K, Map<String, GameInfo> gameMap) {
        this.playerID = null;
        this.ipAddress = null;
        this.port = 0;
        this.N = N;
        this.K = K;
        this.gameMap = gameMap;
    }

    public String getPlayerID() {
        return playerID;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public int getN() {
        return N;
    }

    public int getK() {
        return K;
    }

    public Map<String, GameInfo> getGameMap() {
        return gameMap;
    }
}