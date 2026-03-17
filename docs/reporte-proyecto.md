# Reporte del Proyecto Alpha

## Objetivo

Alpha es un juego distribuido en Java, "Pégale al monstruo", donde varios clientes compiten por golpear un monstruo que aparece de forma periódica en un tablero compartido. El objetivo funcional es demostrar coordinación entre clientes concurrentes, estado global de partida, registro/login y publicación de eventos en tiempo real.

## Arquitectura General

El proyecto usa una arquitectura cliente-servidor con dos canales de comunicación:

- `TCP` para operaciones críticas: registro, login, consulta de estado, golpes y logout.
- `JMS` con ActiveMQ embebido para difusión de monstruos, eventos globales, ganador y reinicio.

El servidor mantiene el estado global de la partida y expone una interfaz Swing de control. Los clientes muestran el tablero, reciben los monstruos por tópico y envían los golpes por TCP.

## Módulos Principales

- `server`
  - `GameServerMain`: punto de entrada del servidor.
  - `AlphaServerRuntime`: ensambla broker, registro, motor de juego y servidor TCP.
  - `EmbeddedBrokerManager`: inicia ActiveMQ embebido.
  - `GameEngine`: genera monstruos, valida golpes, detecta ganador y reinicia la ronda.
  - `PlayerRegistry`: conserva usuarios, sesiones y puntajes.
  - `TcpGameServer` y `ClientSessionHandler`: atienden conexiones TCP.
  - `ServerLauncherController` y `ServerControlFrame`: interfaz de control del servidor.

- `client`
  - `GameClientMain`: punto de entrada del cliente.
  - `GameClientController`: conecta la UI con TCP y JMS.
  - `ClientConnection`: conexión TCP persistente.
  - `GameFrame`: tablero Swing del juego.

- `stress`
  - `StressTestMain`: ejecuta escenarios de carga.
  - `StressClientWorker`: simula clientes concurrentes.
  - `StressSummary` y `StressWorkerResult`: calculan métricas.

- `common`
  - contiene modelos, configuración y utilidades compartidas.

## Cómo Ejecutarlo

El proyecto está preparado para abrirse en IntelliJ IDEA como proyecto Maven. El código fuente vive en `src/main/java` y los recursos en `src/main/resources`.

### Servidor

```bash
./scripts/run-server.sh
```

### Cliente

```bash
./scripts/run-client.sh
```

### Estrés

```bash
./scripts/run-stress.sh
```

### Repetición de estrés

```bash
./scripts/repeat-stress.sh
```

La configuración base está en [`src/main/resources/alpha.properties`](../src/main/resources/alpha.properties):

- tablero `4x4`
- puntaje objetivo `5`
- spawn de monstruos cada `1500 ms`
- reinicio automático `2500 ms` después del ganador

## Requisitos Del PDF Y Estado Actual

- Registro y login por TCP: implementado en `ClientSessionHandler`, `TcpRequest` y `TcpResponse`.
- Información necesaria para jugar después del registro/login: entregada vía `SessionInfo`.
- Monstruos publicados por tópico: implementado en `GameEngine` y `JmsGamePublisher`.
- Golpe del jugador por TCP: implementado en `HIT` dentro del flujo TCP.
- Ganador al llegar a 5 puntos: configurado en `alpha.game.targetScore=5`.
- Reinicio automático de la partida: implementado en `GameEngine` con `RESET`.
- Entrada y salida dinámica de jugadores: soportada por `PlayerRegistry` y el manejo de sesiones.
- Evaluación experimental: soportada por `StressTestMain`, `StressClientWorker` y CSV de resultados.
- Juego con al menos 5 jugadores: el estrés y el diseño actual soportan concurrencia y la configuración está pensada para esa escala mínima.

## Evaluación Experimental Y CSV

La herramienta de estrés genera y acumula resultados en CSV para analizar:

- tiempo promedio de registro,
- desviación estándar del registro,
- porcentaje de éxito del registro,
- tiempo promedio de juego/golpe,
- desviación estándar del juego,
- porcentaje de éxito del juego.

Archivo generado por defecto:

- `samples/stress-results.csv`

Archivo de referencia incluido en el repositorio:

- `samples/stress-results-example.csv`

Cada corrida agrega filas con:

- timestamp,
- cantidad de clientes,
- repetición,
- métricas de registro,
- métricas de juego.

## Entregables Actuales

- código fuente completo del proyecto,
- versión ejecutable por servidor, cliente y estrés,
- resultados experimentales en CSV,
- documentación principal en `README.md`,
- este reporte en `docs/reporte-proyecto.md`.

## Resumen Final

Alpha demuestra un sistema distribuido sencillo pero completo: TCP coordina acciones críticas, JMS sincroniza eventos globales y Swing permite visualizar tanto la partida como la ejecución del servidor. La solución conserva el estado de los jugadores durante la ronda, reinicia automáticamente al terminar una partida y deja lista la evaluación experimental mediante CSV.
