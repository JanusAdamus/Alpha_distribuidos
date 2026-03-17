package mx.itam.alpha.server;

import mx.itam.alpha.common.model.PlayerState;
import mx.itam.alpha.common.util.PasswordUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerRegistry {

    public static class SessionActivation {

        private final PlayerState playerState;
        private final long sessionId;

        public SessionActivation(PlayerState playerState, long sessionId) {
            this.playerState = playerState;
            this.sessionId = sessionId;
        }

        public PlayerState getPlayerState() {
            return playerState;
        }

        public long getSessionId() {
            return sessionId;
        }
    }

    private final Map<String, PlayerRecord> players = new ConcurrentHashMap<>();

    public SessionActivation register(String username, String password) {
        validateCredentials(username, password);
        String passwordHash = PasswordUtils.hash(password);
        PlayerRecord candidate = new PlayerRecord(username, passwordHash);
        PlayerRecord previous = players.putIfAbsent(username, candidate);
        if (previous != null) {
            throw new IllegalArgumentException("El usuario ya existe");
        }
        long sessionId = candidate.activateSession();
        return new SessionActivation(candidate.toPlayerState(), sessionId);
    }

    public SessionActivation login(String username, String password) {
        validateCredentials(username, password);
        PlayerRecord record = players.get(username);
        if (record == null) {
            throw new IllegalArgumentException("El usuario no existe");
        }
        if (!record.getPasswordHash().equals(PasswordUtils.hash(password))) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        long sessionId = record.activateSession();
        return new SessionActivation(record.toPlayerState(), sessionId);
    }

    public PlayerState disconnect(String username, long sessionId) {
        PlayerRecord record = players.get(username);
        if (record == null) {
            return null;
        }
        return record.disconnectIfMatches(sessionId) ? record.toPlayerState() : null;
    }

    public PlayerState incrementScore(String username) {
        PlayerRecord record = players.get(username);
        if (record == null) {
            throw new IllegalArgumentException("Jugador no encontrado");
        }
        record.incrementScore();
        return record.toPlayerState();
    }

    public PlayerState findPlayer(String username) {
        PlayerRecord record = players.get(username);
        return record == null ? null : record.toPlayerState();
    }

    public boolean isSessionActive(String username, long sessionId) {
        PlayerRecord record = players.get(username);
        return record != null && record.hasSession(sessionId);
    }

    public Map<String, Integer> snapshotScores() {
        return players.values().stream()
                .sorted(Comparator.comparingInt(PlayerRecord::getScore).reversed()
                        .thenComparing(PlayerRecord::getUsername))
                .collect(LinkedHashMap::new,
                        (map, record) -> map.put(record.getUsername(), record.getScore()),
                        LinkedHashMap::putAll);
    }

    public void resetScores() {
        players.values().forEach(PlayerRecord::resetScore);
    }

    private void validateCredentials(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
    }
}
