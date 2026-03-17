package mx.unam.alpha.common.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class GameStateSnapshot {

    private String gameId;
    private long monsterId;
    private int activeRow;
    private int activeCol;
    private boolean monsterVisible;
    private Map<String, Integer> scores = new LinkedHashMap<>();

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public long getMonsterId() {
        return monsterId;
    }

    public void setMonsterId(long monsterId) {
        this.monsterId = monsterId;
    }

    public int getActiveRow() {
        return activeRow;
    }

    public void setActiveRow(int activeRow) {
        this.activeRow = activeRow;
    }

    public int getActiveCol() {
        return activeCol;
    }

    public void setActiveCol(int activeCol) {
        this.activeCol = activeCol;
    }

    public boolean isMonsterVisible() {
        return monsterVisible;
    }

    public void setMonsterVisible(boolean monsterVisible) {
        this.monsterVisible = monsterVisible;
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    public void setScores(Map<String, Integer> scores) {
        this.scores = scores;
    }
}
