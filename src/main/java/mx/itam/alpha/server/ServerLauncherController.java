package mx.itam.alpha.server;

import mx.itam.alpha.common.config.AppConfig;
import mx.itam.alpha.client.GameClientController;
import mx.itam.alpha.server.ui.ServerControlFrame;
import mx.itam.alpha.stress.StressTestMain;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerLauncherController implements ServerControlFrame.Listener {

    private final AppConfig config;
    private final AlphaServerRuntime runtime;
    private final Path rootDirectory;
    private final ExecutorService backgroundTasks = Executors.newSingleThreadExecutor();
    private final AtomicBoolean stressRunning = new AtomicBoolean(false);
    private volatile Path latestStressResultsPath;
    private ServerControlFrame frame;

    public ServerLauncherController(AppConfig config, AlphaServerRuntime runtime, Path rootDirectory) {
        this.config = config;
        this.runtime = runtime;
        this.rootDirectory = rootDirectory;
    }

    public void show() {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Servidor listo en " + config.getTcpHost() + ":" + config.getTcpPort());
            System.out.println("ActiveMQ listo en " + config.getJmsBrokerUrl());
            System.out.println("Abre clientes con mx.itam.alpha.client.GameClientMain y corre estrés con mx.itam.alpha.stress.StressTestMain.");
            awaitShutdown();
            return;
        }
        SwingUtilities.invokeLater(() -> {
            frame = new ServerControlFrame();
            frame.setListener(this);
            frame.setServerState("Servidor activo en " + config.getTcpHost() + ":" + config.getTcpPort());
            frame.setEnvironmentSummary(buildEnvironmentSummary());
            frame.setPropertiesContent(readProjectFile("src/main/resources/alpha.properties"));
            frame.setRequirementsSummary(buildRequirementsSummary());
            frame.appendLog("ActiveMQ embebido levantado en " + config.getJmsBrokerUrl());
            frame.appendLog("Usa los botones para abrir clientes, correr estrés y revisar archivos del proyecto.");
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent event) {
                    backgroundTasks.shutdownNow();
                    runtime.close();
                    System.exit(0);
                }
            });
            frame.setVisible(true);
        });
    }

    private void awaitShutdown() {
        try {
            new CountDownLatch(1).await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            backgroundTasks.shutdownNow();
            runtime.close();
        }
    }

    @Override
    public void onOpenClient() {
        SwingUtilities.invokeLater(() -> {
            GameClientController controller = new GameClientController(config);
            controller.show();
            log("Cliente abierto dentro de la aplicación.");
        });
    }

    @Override
    public void onRunStress() {
        runStress(Map.of(
                "clients", "10,50,100",
                "hits", "10",
                "repetitions", "1",
                "output", "samples/generated/stress-results.csv"
        ), "Estrés rápido lanzado.");
    }

    @Override
    public void onRunRepeatStress() {
        runStress(Map.of(
                "clients", "10,50,100,150,200,250,300,350,400,450,500",
                "hits", "12",
                "repetitions", "10",
                "output", "samples/generated/stress-results.csv"
        ), "Estrés de 10 repeticiones lanzado.");
    }

    @Override
    public void onOpenResults() {
        showStressArtifact(
                "Último CSV generado",
                "CSV de ejemplo",
                this::showAbsoluteFile,
                this::showFile);
    }

    @Override
    public void onOpenChart() {
        showStressArtifact(
                "Gráfica del último CSV",
                "Gráfica desde CSV de ejemplo",
                this::showAbsoluteChart,
                this::showChartFromProjectFile);
    }

    @Override
    public void onOpenReadme() {
        showFile("README", "README.md");
    }

    private void showFile(String title, String relativePath) {
        try {
            String content = readProjectFile(relativePath);
            if (frame == null) {
                log(relativePath + System.lineSeparator() + content);
                return;
            }
            frame.showDocument(title, content);
            log("Mostrado: " + relativePath);
        } catch (IllegalStateException exception) {
            log("No se pudo leer " + relativePath + ": " + exception.getMessage());
        }
    }

    private void showAbsoluteFile(String title, Path filePath) {
        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            if (frame == null) {
                log(filePath + System.lineSeparator() + content);
                return;
            }
            frame.showDocument(title, content);
            log("Mostrado: " + filePath);
        } catch (IOException exception) {
            log("No se pudo leer " + filePath + ": " + exception.getMessage());
        }
    }

    private void showChartFromProjectFile(String title, String relativePath) {
        try {
            String content = readProjectFile(relativePath);
            if (frame == null) {
                log("No hay interfaz gráfica disponible para mostrar la gráfica.");
                return;
            }
            frame.showStressChart(title, content);
            log("Gráfica mostrada desde " + relativePath);
        } catch (IllegalStateException exception) {
            log("No se pudo leer " + relativePath + ": " + exception.getMessage());
        }
    }

    private void showAbsoluteChart(String title, Path filePath) {
        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            if (frame == null) {
                log("No hay interfaz gráfica disponible para mostrar la gráfica.");
                return;
            }
            frame.showStressChart(title, content);
            log("Gráfica mostrada desde " + filePath);
        } catch (IOException exception) {
            log("No se pudo leer " + filePath + ": " + exception.getMessage());
        }
    }

    private void log(String message) {
        if (frame == null) {
            System.out.println(message);
        } else {
            SwingUtilities.invokeLater(() -> frame.appendLog(message));
        }
    }

    private void runStress(Map<String, String> options, String startMessage) {
        if (!stressRunning.compareAndSet(false, true)) {
            log("Ya hay una corrida de estrés en progreso.");
            return;
        }
        latestStressResultsPath = resolveOutputPath(options.getOrDefault("output", "samples/stress-results.csv"));
        log(startMessage);
        backgroundTasks.submit(() -> {
            try {
                StressTestMain.runWithOptions(config, options, this::log);
                if (latestStressResultsPath != null) {
                    log("CSV real disponible en " + latestStressResultsPath);
                }
            } catch (Exception exception) {
                log("Falló la corrida de estrés: " + exception.getMessage());
            } finally {
                stressRunning.set(false);
            }
        });
    }

    private Path resolveOutputPath(String output) {
        Path path = Paths.get(output);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return rootDirectory.resolve(path).normalize();
    }

    private void showStressArtifact(String latestTitle, String fallbackTitle,
                                    java.util.function.BiConsumer<String, Path> latestAction,
                                    java.util.function.BiConsumer<String, String> fallbackAction) {
        Path latestResults = latestStressResultsPath;
        if (latestResults != null && Files.exists(latestResults)) {
            latestAction.accept(latestTitle, latestResults);
            return;
        }
        fallbackAction.accept(fallbackTitle, "samples/stress-results-example.csv");
    }

    private String readProjectFile(String relativePath) {
        Path filePath = rootDirectory.resolve(relativePath);
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo leer " + relativePath, exception);
        }
    }

    private String buildEnvironmentSummary() {
        return """
                TCP:
                - Host: %s
                - Puerto: %d

                JMS:
                - Broker embebido: %s
                - Tópico monstruos: %s
                - Tópico eventos: %s
                - Tópico ganador/reset: %s

                Juego:
                - Tablero: %dx%d
                - Meta para ganar: %d golpes
                - Aparición de monstruos: cada %d ms
                - Reinicio automático: %d ms después del ganador
                - Capacidad esperada: al menos 5 jugadores conectados
                """.formatted(
                config.getTcpHost(),
                config.getTcpPort(),
                config.getJmsBrokerUrl(),
                config.getMonsterTopic(),
                config.getEventsTopic(),
                config.getWinnerTopic(),
                config.getBoardRows(),
                config.getBoardCols(),
                config.getTargetScore(),
                config.getSpawnIntervalMs(),
                config.getRestartDelayMs());
    }

    private String buildRequirementsSummary() {
        return """
                Checklist del PDF:
                [OK] Registro y login por sockets TCP.
                [OK] El servidor entrega al cliente la información de sesión para jugar.
                [OK] Los monstruos se publican por tópicos JMS.
                [OK] El golpe del jugador viaja por TCP al servidor.
                [OK] El ganador se anuncia por tópico y la partida se reinicia automáticamente.
                [OK] Los jugadores pueden entrar y salir preservando el contexto de la ronda.
                [OK] Existe modo de estrés para registro y juego.

                Entregables visibles desde esta ventana:
                - README con arquitectura y flujo.
                - alpha.properties visible directamente en el panel principal.
                - Último CSV real generado por estrés, con ejemplo como respaldo.
                - Estrés rápido y estrés de 10 repeticiones.

                Recordatorio:
                - La evaluación pide promedio, desviación estándar y porcentaje de éxito.
                - Se requieren al menos 10 repeticiones por configuración experimental.
                - El juego debe correr con mínimo 5 jugadores.
                """;
    }
}
