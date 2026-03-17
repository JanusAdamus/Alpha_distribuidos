package mx.unam.alpha.common.util;

import java.util.List;

public final class StatsUtils {

    private StatsUtils() {
    }

    public static double average(List<Long> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        long sum = 0L;
        for (Long value : values) {
            sum += value;
        }
        return (double) sum / values.size();
    }

    public static double stddev(List<Long> values) {
        if (values.size() < 2) {
            return 0.0;
        }
        double average = average(values);
        double sum = 0.0;
        for (Long value : values) {
            double diff = value - average;
            sum += diff * diff;
        }
        return Math.sqrt(sum / values.size());
    }
}
