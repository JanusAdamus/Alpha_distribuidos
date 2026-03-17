package mx.itam.alpha.common.model;

public class MonsterSpawnEvent {

    private String gameId;
    private long monsterId;
    private int row;
    private int col;
    private long spawnedAt;

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

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public long getSpawnedAt() {
        return spawnedAt;
    }

    public void setSpawnedAt(long spawnedAt) {
        this.spawnedAt = spawnedAt;
    }
}
