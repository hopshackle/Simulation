package hopshackle.simulation.metric;

import java.util.*;
import java.util.stream.Collectors;

public class StatsCollator {

    private static Map<String, Double> statistics = new HashMap<>();
    private static Map<String, Integer> count = new HashMap<>();

    public static void clear() {
        statistics = new HashMap<>();
        count = new HashMap<>();
    }

    public static void addStatistics(Map<String, Double> newStats) {
        newStats.forEach(
                (k, v) -> addStatistics(k, v)
        );
    }

    public static void addStatistics(String key, Double value) {
        double oldV = statistics.getOrDefault(key, 0.00);
        double newValue = oldV + value;
        statistics.put(key, newValue);
        int newCount = count.getOrDefault(key, 0) + 1;
        count.put(key, newCount);
    }

    public static String summaryString() {
        return statistics.entrySet().stream()
                .map(tuple -> String.format("%-20s = %.4g\n", tuple.getKey(), tuple.getValue() /
                        count.get(tuple.getKey())))
                .sorted()
                .collect(Collectors.joining());
    }
}

