package mx.itam.alpha.stress;

import mx.itam.alpha.client.ClientConnection;
import mx.itam.alpha.common.model.GameStateSnapshot;
import mx.itam.alpha.common.model.TcpRequest;
import mx.itam.alpha.common.model.TcpResponse;

import java.util.concurrent.Callable;

/**
 * Simula un cliente completo para el experimento.
 * Mide por separado el costo del registro y el costo de intentar golpes
 * sobre el juego real usando el mismo protocolo TCP del sistema.
 */
public class StressClientWorker implements Callable<StressWorkerResult> {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final int hitsPerClient;
    private final long thinkTimeMs;

    public StressClientWorker(String host, int port, String username, String password, int hitsPerClient, long thinkTimeMs) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.hitsPerClient = hitsPerClient;
        this.thinkTimeMs = thinkTimeMs;
    }

    @Override
    public StressWorkerResult call() {
        StressWorkerResult result = new StressWorkerResult();
        try (ClientConnection registerConnection = new ClientConnection()) {
            // El registro se mide en una conexión aparte para no mezclar su latencia
            // con el flujo posterior de juego.
            registerConnection.connect(host, port);
            long registerStart = System.nanoTime();
            TcpResponse registerResponse = registerConnection.send(TcpRequest.register(username, password));
            long registerEnd = System.nanoTime();
            result.getRegisterTimes().add(toMillis(registerEnd - registerStart));
            result.setRegisterSuccess(registerResponse.isAccepted());
            if (registerResponse.isAccepted()) {
                registerConnection.send(TcpRequest.logout());
            }
        } catch (Exception ignored) {
            return result;
        }

        if (!result.isRegisterSuccess()) {
            return result;
        }

        try (ClientConnection gameConnection = new ClientConnection()) {
            gameConnection.connect(host, port);
            TcpResponse loginResponse = gameConnection.send(TcpRequest.login(username, password));
            if (!loginResponse.isAccepted()) {
                return result;
            }
            for (int hitIndex = 0; hitIndex < hitsPerClient; hitIndex++) {
                // Cada intento consulta primero el snapshot actual para usar fila, columna
                // y monsterId válidos en ese instante.
                TcpResponse snapshotResponse = gameConnection.send(TcpRequest.gameState());
                GameStateSnapshot snapshot = snapshotResponse.getGameState();
                if (snapshot == null || !snapshot.isMonsterVisible() || snapshot.getMonsterId() < 0L) {
                    sleepQuietly(thinkTimeMs);
                    continue;
                }
                long hitStart = System.nanoTime();
                TcpResponse hitResponse = gameConnection.send(TcpRequest.hit(
                        snapshot.getActiveRow(), snapshot.getActiveCol(), snapshot.getMonsterId()));
                long hitEnd = System.nanoTime();
                result.getHitTimes().add(toMillis(hitEnd - hitStart));
                if (hitResponse.isAccepted()) {
                    result.incrementHitSuccesses();
                }
                sleepQuietly(thinkTimeMs);
            }
            gameConnection.send(TcpRequest.logout());
        } catch (Exception ignored) {
        }
        return result;
    }

    private void sleepQuietly(long millis) {
        if (millis <= 0L) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private long toMillis(long nanos) {
        return nanos / 1_000_000L;
    }
}
