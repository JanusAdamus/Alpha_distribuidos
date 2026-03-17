package mx.unam.alpha.common.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class GlobalGameEvent {

    private GlobalEventType type;
    private String gameId;
    private String message;
    private String username;
    private String winner;
    private int score;
    private long timestamp;
    private Map<String, Integer> scores = new LinkedHashMap<>();

    public GlobalEventType getType() {
        return type;
    }

    public void setType(GlobalEventType type) {
        this.type = type;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    public void setScores(Map<String, Integer> scores) {
        this.scores = scores;
    }
}
