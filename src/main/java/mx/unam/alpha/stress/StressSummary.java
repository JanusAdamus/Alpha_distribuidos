package mx.unam.alpha.stress;

import mx.unam.alpha.common.util.StatsUtils;

import java.util.ArrayList;
import java.util.List;

public class StressSummary {

    private final List<Long> registerTimes = new ArrayList<>();
    private final List<Long> hitTimes = new ArrayList<>();
    private int successfulRegisters;
    private int totalRegisters;
    private int successfulHits;
    private int totalHits;

    public void merge(StressWorkerResult result) {
        totalRegisters++;
        if (result.isRegisterSuccess()) {
            successfulRegisters++;
        }
        registerTimes.addAll(result.getRegisterTimes());
        hitTimes.addAll(result.getHitTimes());
        successfulHits += result.getHitSuccesses();
        totalHits += result.getHitTimes().size();
    }

    public double registerAverageMs() {
        return StatsUtils.average(registerTimes);
    }

    public double registerStddevMs() {
        return StatsUtils.stddev(registerTimes);
    }

    public double registerSuccessPct() {
        return totalRegisters == 0 ? 0.0 : successfulRegisters * 100.0 / totalRegisters;
    }

    public double hitAverageMs() {
        return StatsUtils.average(hitTimes);
    }

    public double hitStddevMs() {
        return StatsUtils.stddev(hitTimes);
    }

    public double hitSuccessPct() {
        return totalHits == 0 ? 0.0 : successfulHits * 100.0 / totalHits;
    }
}
