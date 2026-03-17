package mx.unam.alpha.server;

import mx.unam.alpha.common.config.AppConfig;
import mx.unam.alpha.common.model.PlayerState;
import mx.unam.alpha.common.model.RequestType;
import mx.unam.alpha.common.model.SessionInfo;
import mx.unam.alpha.common.model.TcpRequest;
import mx.unam.alpha.common.model.TcpResponse;
import mx.unam.alpha.common.util.JsonUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientSessionHandler implements Runnable {

    private final Socket socket;
    private final AppConfig config;
    private final PlayerRegistry playerRegistry;
    private final GameEngine gameEngine;
    private String currentUsername;
    private long currentSessionId;

    public ClientSessionHandler(Socket socket, AppConfig config, PlayerRegistry playerRegistry, GameEngine gameEngine) {
        this.socket = socket;
        this.config = config;
        this.playerRegistry = playerRegistry;
        this.gameEngine = gameEngine;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                TcpRequest request = JsonUtils.fromJson(line, TcpRequest.class);
                TcpResponse response = handleRequest(request);
                writer.write(JsonUtils.toJson(response));
                writer.newLine();
                writer.flush();
                if (request.getType() == RequestType.LOGOUT) {
                    break;
                }
            }
        } catch (Exception exception) {
            System.err.println("Conexión cerrada: " + exception.getMessage());
        } finally {
            releaseCurrentUser();
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private TcpResponse handleRequest(TcpRequest request) {
        try {
            return switch (request.getType()) {
                case REGISTER -> authenticate(request, true);
                case LOGIN -> authenticate(request, false);
                case HIT -> handleHit(request);
                case GAME_STATE -> handleGameState();
                case LOGOUT -> handleLogout();
                case PING -> TcpResponse.ok("pong");
            };
        } catch (IllegalArgumentException exception) {
            return TcpResponse.error(exception.getMessage());
        } catch (Exception exception) {
            return TcpResponse.error("Error interno del servidor");
        }
    }

    private TcpResponse authenticate(TcpRequest request, boolean register) {
        releaseCurrentUser();
        PlayerRegistry.SessionActivation activation = register
                ? playerRegistry.register(request.getUsername(), request.getPassword())
                : playerRegistry.login(request.getUsername(), request.getPassword());
        PlayerState playerState = activation.getPlayerState();
        currentUsername = playerState.getUsername();
        currentSessionId = activation.getSessionId();
        gameEngine.notifyPlayerJoined(currentUsername);

        TcpResponse response = TcpResponse.ok(register ? "Registro exitoso" : "Login exitoso");
        response.setPlayerState(playerState);
        response.setSessionInfo(buildSessionInfo(playerState));
        response.setGameState(gameEngine.buildSnapshot());
        return response;
    }

    private TcpResponse handleHit(TcpRequest request) {
        if (!hasActiveSession()) {
            return TcpResponse.unauthorized("Debes iniciar sesión");
        }
        if (request.getMonsterId() == null || request.getRow() == null || request.getCol() == null) {
            return TcpResponse.error("El golpe está incompleto");
        }
        return gameEngine.processHit(currentUsername, request.getMonsterId(), request.getRow(), request.getCol());
    }

    private TcpResponse handleGameState() {
        if (!hasActiveSession()) {
            return TcpResponse.unauthorized("Debes iniciar sesión");
        }
        TcpResponse response = TcpResponse.ok("Estado actual");
        response.setPlayerState(playerRegistry.findPlayer(currentUsername));
        response.setGameState(gameEngine.buildSnapshot());
        return response;
    }

    private TcpResponse handleLogout() {
        if (currentUsername == null) {
            return TcpResponse.ok("Sesión cerrada");
        }
        PlayerState playerState = releaseCurrentUser();
        TcpResponse response = TcpResponse.ok("Sesión cerrada");
        response.setPlayerState(playerState);
        return response;
    }

    private boolean hasActiveSession() {
        if (currentUsername == null) {
            return false;
        }
        boolean active = playerRegistry.isSessionActive(currentUsername, currentSessionId);
        if (!active) {
            currentUsername = null;
            currentSessionId = 0L;
        }
        return active;
    }

    private PlayerState releaseCurrentUser() {
        if (currentUsername == null) {
            return null;
        }
        String username = currentUsername;
        long sessionId = currentSessionId;
        currentUsername = null;
        currentSessionId = 0L;
        PlayerState playerState = playerRegistry.disconnect(username, sessionId);
        if (playerState != null) {
            gameEngine.notifyPlayerLeft(username);
        }
        return playerState;
    }

    private SessionInfo buildSessionInfo(PlayerState playerState) {
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setPlayerId(playerState.getPlayerId());
        sessionInfo.setUsername(playerState.getUsername());
        sessionInfo.setTcpHost(config.getTcpHost());
        sessionInfo.setTcpPort(config.getTcpPort());
        sessionInfo.setJmsBrokerUrl(config.getJmsBrokerUrl());
        sessionInfo.setMonsterTopic(config.getMonsterTopic());
        sessionInfo.setEventsTopic(config.getEventsTopic());
        sessionInfo.setWinnerTopic(config.getWinnerTopic());
        sessionInfo.setBoardRows(config.getBoardRows());
        sessionInfo.setBoardCols(config.getBoardCols());
        sessionInfo.setTargetScore(config.getTargetScore());
        sessionInfo.setSpawnIntervalMs(config.getSpawnIntervalMs());
        return sessionInfo;
    }
}
