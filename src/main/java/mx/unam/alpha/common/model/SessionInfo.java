package mx.unam.alpha.common.model;

public class SessionInfo {

    private String playerId;
    private String username;
    private String tcpHost;
    private int tcpPort;
    private String jmsBrokerUrl;
    private String monsterTopic;
    private String eventsTopic;
    private String winnerTopic;
    private int boardRows;
    private int boardCols;
    private int targetScore;
    private long spawnIntervalMs;

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTcpHost() {
        return tcpHost;
    }

    public void setTcpHost(String tcpHost) {
        this.tcpHost = tcpHost;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public String getJmsBrokerUrl() {
        return jmsBrokerUrl;
    }

    public void setJmsBrokerUrl(String jmsBrokerUrl) {
        this.jmsBrokerUrl = jmsBrokerUrl;
    }

    public String getMonsterTopic() {
        return monsterTopic;
    }

    public void setMonsterTopic(String monsterTopic) {
        this.monsterTopic = monsterTopic;
    }

    public String getEventsTopic() {
        return eventsTopic;
    }

    public void setEventsTopic(String eventsTopic) {
        this.eventsTopic = eventsTopic;
    }

    public String getWinnerTopic() {
        return winnerTopic;
    }

    public void setWinnerTopic(String winnerTopic) {
        this.winnerTopic = winnerTopic;
    }

    public int getBoardRows() {
        return boardRows;
    }

    public void setBoardRows(int boardRows) {
        this.boardRows = boardRows;
    }

    public int getBoardCols() {
        return boardCols;
    }

    public void setBoardCols(int boardCols) {
        this.boardCols = boardCols;
    }

    public int getTargetScore() {
        return targetScore;
    }

    public void setTargetScore(int targetScore) {
        this.targetScore = targetScore;
    }

    public long getSpawnIntervalMs() {
        return spawnIntervalMs;
    }

    public void setSpawnIntervalMs(long spawnIntervalMs) {
        this.spawnIntervalMs = spawnIntervalMs;
    }
}
