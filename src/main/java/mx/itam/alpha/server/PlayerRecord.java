package mx.itam.alpha.server;

import mx.itam.alpha.common.model.PlayerState;

import java.util.UUID;

class PlayerRecord {

    private final String playerId = UUID.randomUUID().toString();
    private final String username;
    private final String passwordHash;
    private int score;
    private boolean connected;
    private long activeSessionId;
    private long sessionSequence;

    PlayerRecord(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public synchronized int getScore() {
        return score;
    }

    public synchronized int incrementScore() {
        score++;
        return score;
    }

    public synchronized void resetScore() {
        score = 0;
    }

    public synchronized long activateSession() {
        sessionSequence++;
        activeSessionId = sessionSequence;
        connected = true;
        return activeSessionId;
    }

    public synchronized boolean hasSession(long sessionId) {
        return connected && activeSessionId == sessionId;
    }

    public synchronized boolean disconnectIfMatches(long sessionId) {
        if (!hasSession(sessionId)) {
            return false;
        }
        connected = false;
        activeSessionId = 0L;
        return true;
    }

    public synchronized PlayerState toPlayerState() {
        PlayerState state = new PlayerState();
        state.setPlayerId(playerId);
        state.setUsername(username);
        state.setScore(score);
        state.setConnected(connected);
        return state;
    }
}
