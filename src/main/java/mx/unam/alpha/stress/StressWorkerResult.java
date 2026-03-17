package mx.unam.alpha.stress;

import java.util.ArrayList;
import java.util.List;

public class StressWorkerResult {

    private boolean registerSuccess;
    private final List<Long> registerTimes = new ArrayList<>();
    private final List<Long> hitTimes = new ArrayList<>();
    private int hitSuccesses;

    public boolean isRegisterSuccess() {
        return registerSuccess;
    }

    public void setRegisterSuccess(boolean registerSuccess) {
        this.registerSuccess = registerSuccess;
    }

    public List<Long> getRegisterTimes() {
        return registerTimes;
    }

    public List<Long> getHitTimes() {
        return hitTimes;
    }

    public int getHitSuccesses() {
        return hitSuccesses;
    }

    public void incrementHitSuccesses() {
        hitSuccesses++;
    }
}
