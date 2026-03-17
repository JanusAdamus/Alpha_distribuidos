package mx.unam.alpha.client;

import mx.unam.alpha.client.ui.GameFrame;
import mx.unam.alpha.common.config.AppConfig;
import mx.unam.alpha.common.model.GameStateSnapshot;
import mx.unam.alpha.common.model.GlobalEventType;
import mx.unam.alpha.common.model.GlobalGameEvent;
import mx.unam.alpha.common.model.MonsterSpawnEvent;
import mx.unam.alpha.common.model.PlayerState;
import mx.unam.alpha.common.model.SessionInfo;
import mx.unam.alpha.common.model.TcpRequest;
import mx.unam.alpha.common.model.TcpResponse;
import mx.unam.alpha.common.util.JsonUtils;
import mx.unam.alpha.jms.AsyncTopicReceiver;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.util.Map;

public class GameClientController implements GameFrame.Listener {

    private final AppConfig config;
    private final GameFrame frame;
    private final ClientConnection connection = new ClientConnection();

    private AsyncTopicReceiver monstersReceiver;
    private AsyncTopicReceiver eventsReceiver;
    private AsyncTopicReceiver winnerReceiver;
    private SessionInfo sessionInfo;
    private long currentMonsterId = -1L;

    public GameClientController(AppConfig config) {
        this.config = config;
        this.frame = new GameFrame(config.getBoardRows(), config.getBoardCols());
        this.frame.setListener(this);
    }

    public void show() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    @Override
    public void onRegister(String username, String password) {
        authenticate(TcpRequest.register(username, password));
    }

    @Override
    public void onLogin(String username, String password) {
        authenticate(TcpRequest.login(username, password));
    }

    private void authenticate(TcpRequest request) {
        try {
            connection.connect(config.getTcpHost(), config.getTcpPort());
            TcpResponse response = connection.send(request);
            if (!response.isAccepted() || response.getSessionInfo() == null) {
                frame.appendStatus(response.getMessage());
                safeCloseConnection();
                return;
            }
            sessionInfo = response.getSessionInfo();
            openSubscriptions();
            frame.setConnected(true);
            frame.applySessionInfo(sessionInfo);
            frame.appendStatus(response.getMessage());
            updatePlayer(response.getPlayerState());
            updateGameState(response.getGameState());
        } catch (Exception exception) {
            frame.appendStatus("No se pudo autenticar: " + exception.getMessage());
            safeCloseConnection();
        }
    }

    @Override
    public void onLogout() {
        try {
            if (sessionInfo != null) {
                connection.send(TcpRequest.logout());
            }
        } catch (Exception ignored) {
        } finally {
            closeSubscriptions();
            safeCloseConnection();
            sessionInfo = null;
            currentMonsterId = -1L;
            frame.setConnected(false);
            frame.applySessionInfo(null);
            frame.clearMonster();
            frame.appendStatus("Sesión cerrada");
        }
    }

    @Override
    public void onCellClicked(int row, int col) {
        if (sessionInfo == null || currentMonsterId < 0L) {
            frame.appendStatus("No hay monstruo activo");
            return;
        }
        try {
            TcpResponse response = connection.send(TcpRequest.hit(row, col, currentMonsterId));
            frame.appendStatus(response.getMessage());
            updatePlayer(response.getPlayerState());
            updateGameState(response.getGameState());
        } catch (IOException exception) {
            frame.appendStatus("Error enviando golpe: " + exception.getMessage());
            onLogout();
        }
    }

    private void openSubscriptions() {
        closeSubscriptions();
        monstersReceiver = new AsyncTopicReceiver(sessionInfo.getJmsBrokerUrl(), sessionInfo.getMonsterTopic());
        eventsReceiver = new AsyncTopicReceiver(sessionInfo.getJmsBrokerUrl(), sessionInfo.getEventsTopic());
        winnerReceiver = new AsyncTopicReceiver(sessionInfo.getJmsBrokerUrl(), sessionInfo.getWinnerTopic());

        monstersReceiver.start(payload -> {
            MonsterSpawnEvent event = JsonUtils.fromJson(payload, MonsterSpawnEvent.class);
            SwingUtilities.invokeLater(() -> {
                currentMonsterId = event.getMonsterId();
                frame.highlightMonster(event.getRow(), event.getCol());
                frame.appendStatus("Monstruo en [" + event.getRow() + "," + event.getCol() + "]");
            });
        });
        eventsReceiver.start(payload -> {
            GlobalGameEvent event = JsonUtils.fromJson(payload, GlobalGameEvent.class);
            SwingUtilities.invokeLater(() -> handleGlobalEvent(event));
        });
        winnerReceiver.start(payload -> {
            GlobalGameEvent event = JsonUtils.fromJson(payload, GlobalGameEvent.class);
            SwingUtilities.invokeLater(() -> handleWinnerEvent(event));
        });
    }

    private void handleGlobalEvent(GlobalGameEvent event) {
        frame.appendStatus(event.getMessage());
        updateScoreboard(event.getScores());
        if (sessionInfo != null && event.getScores().containsKey(sessionInfo.getUsername())) {
            frame.updatePlayer(sessionInfo.getUsername(), event.getScores().get(sessionInfo.getUsername()), true);
        }
    }

    private void handleWinnerEvent(GlobalGameEvent event) {
        frame.appendStatus(event.getMessage());
        if (event.getType() == GlobalEventType.WINNER) {
            frame.clearMonster();
            currentMonsterId = -1L;
            frame.appendStatus("Esperando reinicio automático de la partida...");
        }
        if (event.getType() == GlobalEventType.RESET) {
            frame.clearMonster();
            currentMonsterId = -1L;
            updateScoreboard(event.getScores());
            if (sessionInfo != null) {
                int score = event.getScores().getOrDefault(sessionInfo.getUsername(), 0);
                frame.updatePlayer(sessionInfo.getUsername(), score, true);
            }
        }
    }

    private void updateGameState(GameStateSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        updateScoreboard(snapshot.getScores());
        if (!snapshot.isMonsterVisible()) {
            currentMonsterId = -1L;
            frame.clearMonster();
            return;
        }
        currentMonsterId = snapshot.getMonsterId();
        frame.highlightMonster(snapshot.getActiveRow(), snapshot.getActiveCol());
    }

    private void updatePlayer(PlayerState playerState) {
        if (playerState == null) {
            return;
        }
        frame.updatePlayer(playerState.getUsername(), playerState.getScore(), playerState.isConnected());
    }

    private void updateScoreboard(Map<String, Integer> scoreboard) {
        if (scoreboard == null) {
            return;
        }
        frame.updateScoreboard(scoreboard);
    }

    private void closeSubscriptions() {
        if (monstersReceiver != null) {
            monstersReceiver.close();
            monstersReceiver = null;
        }
        if (eventsReceiver != null) {
            eventsReceiver.close();
            eventsReceiver = null;
        }
        if (winnerReceiver != null) {
            winnerReceiver.close();
            winnerReceiver = null;
        }
    }

    private void safeCloseConnection() {
        try {
            connection.close();
        } catch (IOException ignored) {
        }
    }
}
