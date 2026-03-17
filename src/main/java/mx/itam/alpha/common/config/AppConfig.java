package mx.itam.alpha.common.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Centraliza la configuración del proyecto y permite sobreescribir cualquier
 * propiedad con -Dclave=valor para pruebas o despliegues rápidos.
 */
public class AppConfig {

    private final String tcpHost;
    private final int tcpPort;
    private final String jmsBrokerUrl;
    private final String monsterTopic;
    private final String eventsTopic;
    private final String winnerTopic;
    private final int boardRows;
    private final int boardCols;
    private final int targetScore;
    private final long spawnIntervalMs;
    private final long restartDelayMs;
    private final int workerThreads;

    private AppConfig(Properties properties) {
        tcpHost = readString(properties, "alpha.tcp.host", "127.0.0.1");
        tcpPort = readInt(properties, "alpha.tcp.port", 5050);
        jmsBrokerUrl = readString(properties, "alpha.jms.brokerUrl", "tcp://127.0.0.1:61616");
        monsterTopic = readString(properties, "alpha.jms.topic.monsters", "alpha.game.monsters");
        eventsTopic = readString(properties, "alpha.jms.topic.events", "alpha.game.events");
        winnerTopic = readString(properties, "alpha.jms.topic.winner", "alpha.game.winner");
        boardRows = readInt(properties, "alpha.game.boardRows", 4);
        boardCols = readInt(properties, "alpha.game.boardCols", 4);
        targetScore = readInt(properties, "alpha.game.targetScore", 5);
        spawnIntervalMs = readLong(properties, "alpha.game.spawnIntervalMs", 1500L);
        restartDelayMs = readLong(properties, "alpha.game.restartDelayMs", 2500L);
        workerThreads = readInt(properties, "alpha.server.workerThreads", 32);
    }

    public static AppConfig load() {
        Properties properties = new Properties();
        try (InputStream inputStream = AppConfig.class.getClassLoader().getResourceAsStream("alpha.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo cargar alpha.properties", exception);
        }
        return new AppConfig(properties);
    }

    private static String readString(Properties properties, String key, String defaultValue) {
        return System.getProperty(key, properties.getProperty(key, defaultValue)).trim();
    }

    private static int readInt(Properties properties, String key, int defaultValue) {
        return Integer.parseInt(readString(properties, key, String.valueOf(defaultValue)));
    }

    private static long readLong(Properties properties, String key, long defaultValue) {
        return Long.parseLong(readString(properties, key, String.valueOf(defaultValue)));
    }

    public String getTcpHost() {
        return tcpHost;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public String getJmsBrokerUrl() {
        return jmsBrokerUrl;
    }

    public String getMonsterTopic() {
        return monsterTopic;
    }

    public String getEventsTopic() {
        return eventsTopic;
    }

    public String getWinnerTopic() {
        return winnerTopic;
    }

    public int getBoardRows() {
        return boardRows;
    }

    public int getBoardCols() {
        return boardCols;
    }

    public int getTargetScore() {
        return targetScore;
    }

    public long getSpawnIntervalMs() {
        return spawnIntervalMs;
    }

    public long getRestartDelayMs() {
        return restartDelayMs;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }
}
