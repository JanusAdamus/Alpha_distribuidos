package mx.unam.alpha.server;

import mx.unam.alpha.common.config.AppConfig;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpGameServer implements AutoCloseable {

    private final AppConfig config;
    private final PlayerRegistry playerRegistry;
    private final GameEngine gameEngine;
    private final ExecutorService workers;

    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public TcpGameServer(AppConfig config, PlayerRegistry playerRegistry, GameEngine gameEngine) {
        this.config = config;
        this.playerRegistry = playerRegistry;
        this.gameEngine = gameEngine;
        this.workers = Executors.newFixedThreadPool(config.getWorkerThreads());
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(config.getTcpPort());
        running = true;
        acceptThread = new Thread(this::acceptLoop, "tcp-acceptor");
        acceptThread.start();
        System.out.println("Servidor TCP escuchando en " + config.getTcpHost() + ":" + config.getTcpPort());
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                workers.submit(new ClientSessionHandler(socket, config, playerRegistry, gameEngine));
            } catch (IOException exception) {
                if (running) {
                    System.err.println("Error en accept: " + exception.getMessage());
                }
            }
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        workers.shutdownNow();
        if (acceptThread != null) {
            try {
                acceptThread.join(1000L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                acceptThread = null;
            }
        }
    }
}
