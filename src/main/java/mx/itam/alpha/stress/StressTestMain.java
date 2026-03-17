package mx.itam.alpha.stress;

import mx.itam.alpha.common.config.AppConfig;
import mx.itam.alpha.common.util.CsvUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Punto de entrada del estresador.
 * Genera escenarios con distintas cantidades de clientes y persiste
 * las métricas agregadas en CSV para análisis posterior.
 */
public class StressTestMain {

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.load();
        Map<String, String> options = parseArgs(args);
        runWithOptions(config, options, System.out::println);
    }

    public static void runWithOptions(AppConfig config, Map<String, String> options, Consumer<String> logger)
            throws Exception {
        Consumer<String> safeLogger = logger == null ? line -> { } : logger;

        // Estos parámetros se pueden cambiar desde la UI del servidor,
        // desde scripts o desde argumentos directos de línea de comandos.
        String host = options.getOrDefault("host", config.getTcpHost());
        int port = Integer.parseInt(options.getOrDefault("port", String.valueOf(config.getTcpPort())));
        String clientsRaw = options.getOrDefault("clients", "10,50,100");
        int hitsPerClient = Integer.parseInt(options.getOrDefault("hits", "10"));
        int repetitions = Integer.parseInt(options.getOrDefault("repetitions", "10"));
        long thinkTimeMs = Long.parseLong(options.getOrDefault("thinkTimeMs", "15"));
        Path output = Path.of(options.getOrDefault("output", "samples/stress-results.csv"));

        CsvUtils.writeHeaderIfNeeded(output,
                "timestamp,clients,repetition,register_avg_ms,register_stddev_ms,register_success_pct,"
                        + "game_avg_ms,game_stddev_ms,game_success_pct");

        List<Integer> clientCounts = parseClientCounts(clientsRaw);
        for (int clients : clientCounts) {
            for (int repetition = 1; repetition <= repetitions; repetition++) {
                // Cada fila del CSV representa una repetición completa de una configuración.
                StressSummary summary = runScenario(host, port, clients, hitsPerClient, thinkTimeMs, repetition);
                List<String> row = List.of(
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()),
                        String.valueOf(clients),
                        String.valueOf(repetition),
                        format(summary.registerAverageMs()),
                        format(summary.registerStddevMs()),
                        format(summary.registerSuccessPct()),
                        format(summary.hitAverageMs()),
                        format(summary.hitStddevMs()),
                        format(summary.hitSuccessPct())
                );
                CsvUtils.appendRow(output, row);
                safeLogger.accept("Clientes=" + clients
                        + " repetición=" + repetition
                        + " registro(avg/std/success)=" + format(summary.registerAverageMs()) + "/"
                        + format(summary.registerStddevMs()) + "/"
                        + format(summary.registerSuccessPct())
                        + " juego(avg/std/success)=" + format(summary.hitAverageMs()) + "/"
                        + format(summary.hitStddevMs()) + "/"
                        + format(summary.hitSuccessPct()));
            }
        }
        safeLogger.accept("Resultados guardados en " + output.toAbsolutePath());
    }

    private static StressSummary runScenario(String host, int port, int clients, int hitsPerClient,
                                             long thinkTimeMs, int repetition) throws Exception {
        // Se limita el pool para no crear miles de hilos cuando la carga crezca.
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(clients, 128));
        List<Future<StressWorkerResult>> futures = new ArrayList<>();
        String runPrefix = "stress_r" + repetition + "_c" + clients + "_" + System.currentTimeMillis();
        for (int index = 0; index < clients; index++) {
            String username = runPrefix + "_u" + index;
            futures.add(executor.submit(new StressClientWorker(
                    host, port, username, "12345", hitsPerClient, thinkTimeMs)));
        }

        StressSummary summary = new StressSummary();
        for (Future<StressWorkerResult> future : futures) {
            summary.merge(future.get());
        }
        executor.shutdownNow();
        return summary;
    }

    public static Map<String, String> parseArgs(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (String arg : args) {
            if (!arg.startsWith("--") || !arg.contains("=")) {
                continue;
            }
            int separator = arg.indexOf('=');
            options.put(arg.substring(2, separator), arg.substring(separator + 1));
        }
        return options;
    }

    private static List<Integer> parseClientCounts(String raw) {
        List<Integer> values = new ArrayList<>();
        for (String value : raw.split(",")) {
            values.add(Integer.parseInt(value.trim()));
        }
        return values;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
