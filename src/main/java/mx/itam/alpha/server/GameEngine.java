package mx.itam.alpha.server;

import mx.itam.alpha.common.config.AppConfig;
import mx.itam.alpha.common.model.GameStateSnapshot;
import mx.itam.alpha.common.model.GlobalEventType;
import mx.itam.alpha.common.model.GlobalGameEvent;
import mx.itam.alpha.common.model.MonsterSpawnEvent;
import mx.itam.alpha.common.model.PlayerState;
import mx.itam.alpha.common.model.TcpResponse;
import mx.itam.alpha.jms.JmsGamePublisher;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coordina el estado global de la partida.
 * Aquí se decide cuándo aparece un monstruo, qué golpe gana la carrera,
 * cuándo alguien completa la meta y en qué momento se reinicia la ronda.
 */
public class GameEngine implements AutoCloseable {

    private final AppConfig config;
    private final PlayerRegistry playerRegistry;
    private final JmsGamePublisher publisher;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final AtomicLong monsterSequence = new AtomicLong();
    private final Random random = new Random();

    private volatile MonsterState activeMonster;
    private volatile boolean resetting;
    private volatile String gameId = UUID.randomUUID().toString();

    public GameEngine(AppConfig config, PlayerRegistry playerRegistry, JmsGamePublisher publisher) {
        this.config = config;
        this.playerRegistry = playerRegistry;
        this.publisher = publisher;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::spawnMonsterSafely, 0L,
                config.getSpawnIntervalMs(), TimeUnit.MILLISECONDS);
    }

    private void spawnMonsterSafely() {
        try {
            spawnMonster();
        } catch (Exception exception) {
            System.err.println("Error al generar monstruo: " + exception.getMessage());
        }
    }

    private synchronized void spawnMonster() {
        if (resetting) {
            return;
        }
        // Solo existe un monstruo activo a la vez; el último reemplaza al anterior.
        int row = random.nextInt(config.getBoardRows());
        int col = random.nextInt(config.getBoardCols());
        long monsterId = monsterSequence.incrementAndGet();
        activeMonster = new MonsterState(monsterId, row, col);

        MonsterSpawnEvent event = new MonsterSpawnEvent();
        event.setGameId(gameId);
        event.setMonsterId(monsterId);
        event.setRow(row);
        event.setCol(col);
        event.setSpawnedAt(System.currentTimeMillis());
        publisher.publishMonster(event);
    }

    public synchronized TcpResponse processHit(String username, long monsterId, int row, int col) {
        // La sincronización evita que dos golpes válidos reclamen el mismo monstruo.
        if (resetting) {
            return errorResponse("La partida se está reiniciando");
        }
        MonsterState monster = activeMonster;
        if (monster == null) {
            return errorResponse("Todavía no hay monstruo activo");
        }
        if (monster.getMonsterId() != monsterId) {
            return errorResponse("Golpe fuera de tiempo");
        }
        if (monster.getRow() != row || monster.getCol() != col) {
            return errorResponse("Golpe en posición incorrecta");
        }
        if (!monster.claim()) {
            return errorResponse("Ese monstruo ya fue golpeado");
        }

        PlayerState playerState = playerRegistry.incrementScore(username);
        Map<String, Integer> scoreboard = playerRegistry.snapshotScores();
        GlobalGameEvent scoreEvent = baseEvent(GlobalEventType.SCORE_UPDATE,
                username + " golpeó al monstruo", username);
        scoreEvent.setScore(playerState.getScore());
        scoreEvent.setScores(scoreboard);
        publisher.publishEvent(scoreEvent);

        TcpResponse response = TcpResponse.ok("Golpe válido");
        response.setPlayerState(playerState);
        response.setGameState(buildSnapshot());

        if (playerState.getScore() >= config.getTargetScore()) {
            announceWinner(playerState, scoreboard);
        }
        return response;
    }

    private TcpResponse errorResponse(String message) {
        TcpResponse response = TcpResponse.error(message);
        response.setGameState(buildSnapshot());
        return response;
    }

    private void announceWinner(PlayerState winnerState, Map<String, Integer> scoreboard) {
        resetting = true;
        // El ganador se publica primero y el reinicio se difiere para que los clientes
        // alcancen a ver el cierre de la ronda actual.
        GlobalGameEvent winnerEvent = baseEvent(GlobalEventType.WINNER,
                winnerState.getUsername() + " ganó la partida", winnerState.getUsername());
        winnerEvent.setWinner(winnerState.getUsername());
        winnerEvent.setScore(winnerState.getScore());
        winnerEvent.setScores(scoreboard);
        publisher.publishWinner(winnerEvent);
        scheduler.schedule(this::resetGameSafely, config.getRestartDelayMs(), TimeUnit.MILLISECONDS);
    }

    private void resetGameSafely() {
        try {
            resetGame();
        } catch (Exception exception) {
            System.err.println("Error al reiniciar la partida: " + exception.getMessage());
        }
    }

    private synchronized void resetGame() {
        // La ronda nueva conserva a los jugadores registrados, pero arranca puntajes desde cero.
        playerRegistry.resetScores();
        activeMonster = null;
        gameId = UUID.randomUUID().toString();
        resetting = false;

        GlobalGameEvent resetEvent = baseEvent(GlobalEventType.RESET,
                "Nueva partida iniciada", null);
        resetEvent.setScores(playerRegistry.snapshotScores());
        publisher.publishWinner(resetEvent);
    }

    public void notifyPlayerJoined(String username) {
        GlobalGameEvent event = baseEvent(GlobalEventType.PLAYER_JOINED,
                username + " entró al juego", username);
        event.setScores(playerRegistry.snapshotScores());
        publisher.publishEvent(event);
    }

    public void notifyPlayerLeft(String username) {
        GlobalGameEvent event = baseEvent(GlobalEventType.PLAYER_LEFT,
                username + " salió del juego", username);
        event.setScores(playerRegistry.snapshotScores());
        publisher.publishEvent(event);
    }

    public GameStateSnapshot buildSnapshot() {
        GameStateSnapshot snapshot = new GameStateSnapshot();
        snapshot.setGameId(gameId);
        MonsterState monster = activeMonster;
        // Durante el reinicio el cliente debe ver un tablero sin monstruo visible.
        if (monster == null || resetting) {
            snapshot.setMonsterVisible(false);
            snapshot.setMonsterId(-1L);
            snapshot.setActiveRow(-1);
            snapshot.setActiveCol(-1);
        } else {
            snapshot.setMonsterVisible(!monster.isClaimed());
            snapshot.setMonsterId(monster.getMonsterId());
            snapshot.setActiveRow(monster.getRow());
            snapshot.setActiveCol(monster.getCol());
        }
        snapshot.setScores(playerRegistry.snapshotScores());
        return snapshot;
    }

    private GlobalGameEvent baseEvent(GlobalEventType type, String message, String username) {
        GlobalGameEvent event = new GlobalGameEvent();
        event.setType(type);
        event.setGameId(gameId);
        event.setMessage(message);
        event.setUsername(username);
        event.setTimestamp(System.currentTimeMillis());
        return event;
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
