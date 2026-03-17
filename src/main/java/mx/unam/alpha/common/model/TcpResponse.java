package mx.unam.alpha.common.model;

public class TcpResponse {

    private ResponseStatus status;
    private String message;
    private boolean accepted;
    private long serverTime;
    private SessionInfo sessionInfo;
    private PlayerState playerState;
    private GameStateSnapshot gameState;

    public static TcpResponse ok(String message) {
        TcpResponse response = new TcpResponse();
        response.status = ResponseStatus.OK;
        response.message = message;
        response.accepted = true;
        response.serverTime = System.currentTimeMillis();
        return response;
    }

    public static TcpResponse error(String message) {
        TcpResponse response = new TcpResponse();
        response.status = ResponseStatus.ERROR;
        response.message = message;
        response.accepted = false;
        response.serverTime = System.currentTimeMillis();
        return response;
    }

    public static TcpResponse unauthorized(String message) {
        TcpResponse response = new TcpResponse();
        response.status = ResponseStatus.UNAUTHORIZED;
        response.message = message;
        response.accepted = false;
        response.serverTime = System.currentTimeMillis();
        return response;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    public void setStatus(ResponseStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    public void setSessionInfo(SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    public PlayerState getPlayerState() {
        return playerState;
    }

    public void setPlayerState(PlayerState playerState) {
        this.playerState = playerState;
    }

    public GameStateSnapshot getGameState() {
        return gameState;
    }

    public void setGameState(GameStateSnapshot gameState) {
        this.gameState = gameState;
    }
}
