# Proyecto Alpha - Pégale al monstruo

Sistema distribuido en Java donde varios clientes compiten por golpear un monstruo que aparece en un tablero compartido. El proyecto usa dos canales de comunicación:

- TCP para registro, login, consulta de estado, golpes y logout.
- JMS con ActiveMQ para publicar monstruos, eventos globales, ganador y reinicio.

El servidor coordina el estado global de la partida. Los clientes reciben el monstruo por tópicos, dibujan el tablero en Swing y mandan el golpe por TCP. El primer jugador que llega al puntaje objetivo gana y el servidor reinicia automáticamente el juego.

## 1. Estado actual del proyecto

Después de la limpieza, el proyecto quedó:

- compilable con Java 17,
- con estructura de paquetes consistente,
- sin clases duplicadas,
- sin paquetes vacíos,
- sin artefactos generados dentro del repositorio,
- con una UI más ordenada,
- con el servidor funcionando tanto con interfaz gráfica como en modo headless.

No se rehízo la arquitectura. Se mantuvieron los paquetes principales, la lógica del juego y el modelo TCP + JMS.

## 2. Qué hace el sistema

El sistema resuelve cinco responsabilidades principales:

1. registrar y autenticar jugadores,
2. compartir el estado global de la partida,
3. coordinar golpes concurrentes sobre el mismo monstruo,
4. anunciar eventos globales a todos los clientes,
5. permitir reconexión del jugador mientras el servidor siga vivo y la partida actual no haya sido reiniciada.

## 3. Arquitectura

### Servidor

El servidor está formado por estos componentes:

- `GameServerMain`
  - punto de entrada del servidor.

- `AlphaServerRuntime`
  - ensambla broker, publicador, registro de jugadores, motor del juego y servidor TCP.

- `EmbeddedBrokerManager`
  - levanta ActiveMQ embebido dentro del mismo proceso.

- `PlayerRegistry`
  - guarda usuarios, contraseñas hasheadas, sesiones activas y puntajes.

- `GameEngine`
  - genera monstruos, valida golpes, publica eventos, detecta ganador y reinicia la partida.

- `TcpGameServer`
  - abre el socket TCP y delega cada conexión a `ClientSessionHandler`.

- `ClientSessionHandler`
  - atiende una sesión TCP persistente por cliente.

### Cliente

El cliente está formado por:

- `GameClientMain`
  - punto de entrada del cliente.

- `GameClientController`
  - conecta UI, TCP y JMS.

- `ClientConnection`
  - conexión TCP persistente del cliente.

- `GameFrame`
  - ventana Swing del juego.

### JMS

La capa JMS quedó reducida a lo necesario:

- `JmsGamePublisher`
- `TopicMessageSender`
- `AsyncTopicReceiver`

Con esto se cubre la publicación del servidor y la suscripción de los clientes sin clases auxiliares redundantes.

### Estrés

La parte experimental se conserva en:

- `StressTestMain`
- `StressClientWorker`
- `StressSummary`
- `StressWorkerResult`

## 4. Flujo de comunicación

### Registro y login

1. El cliente abre una conexión TCP.
2. Envía `REGISTER` o `LOGIN`.
3. El servidor responde con:
   - `PlayerState`
   - `SessionInfo`
   - `GameStateSnapshot`
4. Con `SessionInfo`, el cliente conoce broker, tópicos y parámetros del tablero.
5. El cliente abre suscripciones JMS.

### Aparición del monstruo

1. `GameEngine` genera un monstruo en una posición aleatoria.
2. El servidor publica `MonsterSpawnEvent` en el tópico de monstruos.
3. Todos los clientes actualizan la celda activa del tablero.

### Golpe

1. El usuario hace clic en una celda.
2. El cliente envía `HIT` por TCP con fila, columna y `monsterId`.
3. El servidor valida:
   - que la partida no esté reiniciando,
   - que exista monstruo activo,
   - que el `monsterId` siga siendo vigente,
   - que la posición sea correcta,
   - que nadie más lo haya reclamado antes.
4. Si el golpe es válido, el servidor incrementa el score, responde por TCP y publica actualización por JMS.

### Ganador y reinicio

1. Cuando un jugador alcanza `alpha.game.targetScore`, el servidor publica `WINNER`.
2. Durante `alpha.game.restartDelayMs`, se bloquean nuevos golpes.
3. El servidor reinicia puntajes, limpia el monstruo activo y publica `RESET`.
4. La siguiente partida comienza sin reiniciar el proceso.

## 5. TCP y JMS

### Operaciones TCP

Las operaciones disponibles son:

- `REGISTER`
- `LOGIN`
- `HIT`
- `GAME_STATE`
- `LOGOUT`
- `PING`

Los mensajes se envían como JSON, una línea por solicitud.

### Tópicos JMS

Se usan tres tópicos:

- `alpha.game.monsters`
  - aparición del monstruo activo.

- `alpha.game.events`
  - entrada de jugadores, salida de jugadores y cambios de marcador.

- `alpha.game.winner`
  - ganador y reinicio de partida.

No hay que arrancar un broker externo para uso normal. El servidor inicia ActiveMQ embebido automáticamente.

## 6. Estructura final del proyecto

```text
Alpha/
├── README.md
├── resumen.md
├── pom.xml
├── samples/
│   └── stress-results-example.csv
├── scripts/
│   ├── repeat-stress.sh
│   ├── run-client.sh
│   ├── run-server.sh
│   └── run-stress.sh
└── src/
    └── main/
        ├── alpha/
        │   ├── client/
        │   │   ├── ClientConnection.java
        │   │   ├── GameClientController.java
        │   │   ├── GameClientMain.java
        │   │   └── ui/
        │   │       └── GameFrame.java
        │   ├── common/
        │   │   ├── config/
        │   │   │   └── AppConfig.java
        │   │   ├── model/
        │   │   │   ├── GameStateSnapshot.java
        │   │   │   ├── GlobalEventType.java
        │   │   │   ├── GlobalGameEvent.java
        │   │   │   ├── MonsterSpawnEvent.java
        │   │   │   ├── PlayerState.java
        │   │   │   ├── RequestType.java
        │   │   │   ├── ResponseStatus.java
        │   │   │   ├── SessionInfo.java
        │   │   │   ├── TcpRequest.java
        │   │   │   └── TcpResponse.java
        │   │   └── util/
        │   │       ├── CsvUtils.java
        │   │       ├── JsonUtils.java
        │   │       ├── PasswordUtils.java
        │   │       └── StatsUtils.java
        │   ├── jms/
        │   │   ├── AsyncTopicReceiver.java
        │   │   ├── JmsGamePublisher.java
        │   │   └── TopicMessageSender.java
        │   ├── server/
        │   │   ├── AlphaServerRuntime.java
        │   │   ├── ClientSessionHandler.java
        │   │   ├── EmbeddedBrokerManager.java
        │   │   ├── GameEngine.java
        │   │   ├── GameServerMain.java
        │   │   ├── MonsterState.java
        │   │   ├── PlayerRecord.java
        │   │   ├── PlayerRegistry.java
        │   │   ├── ServerLauncherController.java
        │   │   ├── TcpGameServer.java
        │   │   └── ui/
        │   │       └── ServerControlFrame.java
        │   └── stress/
        │       ├── StressClientWorker.java
        │       ├── StressSummary.java
        │       ├── StressTestMain.java
        │       └── StressWorkerResult.java
        └── resources/
            └── alpha.properties
```

## 7. Qué se eliminó

Durante la limpieza se eliminaron estos elementos:

- duplicado de `PlayerRecord`
  - existía una copia redundante fuera del árbol canónico.

- `TopicDeployer`
  - monitor auxiliar no usado por el sistema principal.

- `TopicMessageReceiver`
  - consumidor síncrono auxiliar no usado por servidor ni cliente.

- `TopicTextListener`
  - su lógica quedó integrada en `AsyncTopicReceiver`.

- `src/main/java/`
  - volvió a ser la raíz canónica del código para que la estructura física coincida con los paquetes Java.

- subdirectorios físicos `mx/itam/alpha`
  - se conservan porque reflejan correctamente los paquetes `mx.itam.alpha.*`.

- directorios vacíos y rutas redundantes que habían quedado después de movimientos previos.

- `target/`
  - artefactos generados de compilación.

- `samples/generated/stress-results.csv`
  - archivo generado; ahora se crea bajo demanda.

- `.Rhistory`
  - archivo ajeno al código del proyecto.

## 8. Configuración

Archivo central:

`src/main/resources/alpha.properties`

Valores actuales:

```properties
alpha.tcp.host=127.0.0.1
alpha.tcp.port=5050
alpha.jms.brokerUrl=tcp://127.0.0.1:61616
alpha.jms.topic.monsters=alpha.game.monsters
alpha.jms.topic.events=alpha.game.events
alpha.jms.topic.winner=alpha.game.winner
alpha.game.boardRows=4
alpha.game.boardCols=4
alpha.game.targetScore=5
alpha.game.spawnIntervalMs=1500
alpha.game.restartDelayMs=2500
alpha.server.workerThreads=32
```

También puedes sobreescribir cualquier valor con `-Dclave=valor`.

Ejemplo:

```bash
mvn -DskipTests compile exec:java \
  -Dexec.mainClass=mx.itam.alpha.server.GameServerMain \
  -Dalpha.tcp.port=6060 \
  -Dalpha.game.targetScore=10
```

## 9. Cómo compilar

Requisitos:

- Java 17
- Maven 3.9 o compatible

Compilación:

```bash
mvn clean package
```

Si solo quieres compilar sin empaquetar:

```bash
mvn clean compile
```

Maven toma las clases fuente desde la ruta estándar `src/main/java`.

## 10. Cómo correr ActiveMQ

No hay que instalar ni arrancar ActiveMQ aparte para el flujo normal.

Cuando ejecutas el servidor:

- se levanta un broker ActiveMQ Classic embebido,
- escucha en `alpha.jms.brokerUrl`,
- publica los tres tópicos del juego.

Solo necesitarías un broker externo si quisieras reemplazar el embebido por uno propio y mantener las mismas propiedades del proyecto.

## 11. Cómo correr el servidor

### Con script

```bash
./scripts/run-server.sh
```

### Con Maven directo

```bash
mvn -q -DskipTests compile exec:java \
  -Dexec.mainClass=mx.itam.alpha.server.GameServerMain
```

### Qué hace al iniciar

1. carga configuración,
2. levanta ActiveMQ embebido,
3. arranca el servidor TCP,
4. inicia el scheduler de monstruos,
5. abre la ventana de control si hay entorno gráfico.

### Modo headless

Si corres el servidor sin entorno gráfico:

- no intenta abrir la ventana Swing,
- imprime el estado en consola,
- se mantiene activo hasta que lo interrumpas.

## 12. Cómo correr el cliente

### Con script

```bash
./scripts/run-client.sh
```

### Con Maven directo

```bash
mvn -q -DskipTests compile exec:java \
  -Dexec.mainClass=mx.itam.alpha.client.GameClientMain
```

Puedes abrir múltiples instancias para probar concurrencia real entre jugadores.

## 13. Cómo usar el sistema

### Registro inicial

1. arranca el servidor,
2. abre un cliente,
3. escribe usuario y contraseña,
4. pulsa `Registrar`.

### Reingreso

1. cierra sesión con `Salir` o cierra la ventana,
2. abre otro cliente,
3. entra con las mismas credenciales usando `Entrar`.

Mientras la partida actual siga activa, el servidor conserva el score del jugador en memoria.

### Juego

1. espera el monstruo en el tablero,
2. haz clic en la celda activa,
3. el cliente enviará el golpe por TCP,
4. el servidor decidirá si fue válido,
5. el marcador se actualizará para todos.

## 14. Cómo correr la versión de estrés

### Corrida por defecto

```bash
./scripts/run-stress.sh
```

Valores por defecto:

- `clients=10,50,100`
- `hits=10`
- `repetitions=10`
- `output=samples/generated/stress-results.csv`

La carpeta `samples/generated/` no se versiona; se crea automáticamente cuando se ejecuta la prueba.

### Corrida personalizada

```bash
./scripts/run-stress.sh \
  --clients=10,50,100,150,200 \
  --hits=12 \
  --repetitions=10 \
  --output=samples/generated/stress-results.csv
```

### Wrapper para repetir

```bash
./scripts/repeat-stress.sh 10,50,100,150,200 12 10 samples/generated/stress-results.csv
```

## 15. Qué mide la herramienta de estrés

Cada cliente simulado:

1. se registra,
2. hace logout,
3. vuelve a hacer login,
4. consulta `GAME_STATE`,
5. intenta golpear varias veces,
6. cierra sesión.

Las métricas exportadas son:

- `register_avg_ms`
- `register_stddev_ms`
- `register_success_pct`
- `game_avg_ms`
- `game_stddev_ms`
- `game_success_pct`

## 16. Simplificaciones aplicadas sin romper la arquitectura

La limpieza mantuvo el comportamiento, pero dejó el código más directo:

- `AsyncTopicReceiver` absorbió la lógica del listener JMS simple.
- `GameClientController` dejó de pasar identificadores de suscripción que no se usaban.
- `ClientSessionHandler` simplificó el cierre y liberación de sesión.
- `GameEngine` eliminó construcción repetida de respuestas de error.
- `PlayerRecord` perdió métodos no utilizados.
- `GameFrame` y `ServerControlFrame` se reorganizaron con métodos de construcción más claros y menos repetición.
- la ruta física del código ahora vuelve a coincidir con los paquetes Java bajo `src/main/java/mx/itam/alpha`.

## 17. Dependencias

El proyecto conserva únicamente dependencias que siguen siendo necesarias para el estado actual:

- `activemq-broker`
- `activemq-client`
- `javax.jms-api`
- `jackson-databind`
- `slf4j-simple`

No se agregó ninguna dependencia nueva durante la limpieza.

## 18. Limitaciones

- los jugadores y puntajes viven en memoria;
- si el servidor se apaga, ese estado se pierde;
- no hay base de datos;
- no hay pruebas automatizadas dentro de `src/test`;
- el estrés mide la parte TCP y no intenta simular cientos de ventanas Swing.

## 19. Resumen

El proyecto quedó más corto, más consistente y más simple sin cambiar la arquitectura base:

- servidor con broker embebido,
- cliente Swing,
- TCP para acciones críticas,
- JMS para eventos globales,
- herramienta de estrés,
- estructura de paquetes limpia,
- documentación alineada con el árbol actual.
