package mx.unam.alpha.server;

import mx.unam.alpha.common.config.AppConfig;
import mx.unam.alpha.jms.JmsGamePublisher;

public class AlphaServerRuntime implements AutoCloseable {

    private final AppConfig config;
    private EmbeddedBrokerManager brokerManager;
    private JmsGamePublisher publisher;
    private PlayerRegistry playerRegistry;
    private GameEngine gameEngine;
    private TcpGameServer tcpGameServer;
    private boolean started;

    public AlphaServerRuntime(AppConfig config) {
        this.config = config;
    }

    public synchronized void start() throws Exception {
        if (started) {
            return;
        }
        try {
            brokerManager = new EmbeddedBrokerManager(config);
            brokerManager.start();

            publisher = new JmsGamePublisher(config);
            playerRegistry = new PlayerRegistry();
            gameEngine = new GameEngine(config, playerRegistry, publisher);
            tcpGameServer = new TcpGameServer(config, playerRegistry, gameEngine);

            gameEngine.start();
            tcpGameServer.start();
            started = true;
        } catch (Exception exception) {
            close();
            throw exception;
        }
    }

    public synchronized boolean isStarted() {
        return started;
    }

    @Override
    public synchronized void close() {
        started = false;
        if (tcpGameServer != null) {
            tcpGameServer.close();
            tcpGameServer = null;
        }
        if (gameEngine != null) {
            gameEngine.close();
            gameEngine = null;
        }
        if (publisher != null) {
            publisher.close();
            publisher = null;
        }
        if (brokerManager != null) {
            brokerManager.close();
            brokerManager = null;
        }
    }
}
