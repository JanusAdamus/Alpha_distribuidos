package mx.unam.alpha.server;

import java.util.concurrent.atomic.AtomicBoolean;

class MonsterState {

    private final long monsterId;
    private final int row;
    private final int col;
    private final AtomicBoolean claimed = new AtomicBoolean(false);

    MonsterState(long monsterId, int row, int col) {
        this.monsterId = monsterId;
        this.row = row;
        this.col = col;
    }

    public long getMonsterId() {
        return monsterId;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public boolean claim() {
        return claimed.compareAndSet(false, true);
    }

    public boolean isClaimed() {
        return claimed.get();
    }
}
