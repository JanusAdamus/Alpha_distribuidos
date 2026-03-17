# Resumen del proyecto Alpha

## 1. Qué es

Alpha es un juego distribuido en Java llamado "Pégale al monstruo". Un servidor genera monstruos en un tablero 4x4, los clientes los reciben en tiempo real y compiten por golpearlos antes que los demás.

## 2. Tecnologías y enfoque

- Java 17
- Maven
- Swing para la interfaz
- TCP para autenticación y golpes
- JMS con ActiveMQ para eventos globales

La arquitectura mezcla comunicación punto a punto y publicación/suscripción:

- TCP se usa cuando el cliente necesita una validación directa del servidor.
- JMS se usa cuando el mismo evento debe llegar a todos los clientes.

## 3. Cómo funciona en pocas líneas

1. El usuario se registra o inicia sesión por TCP.
2. El servidor devuelve la configuración necesaria para conectarse al broker JMS.
3. El cliente se suscribe a los tópicos del juego.
4. El servidor publica monstruos y eventos globales.
5. Cuando el jugador golpea, el cliente manda la jugada por TCP.
6. El servidor decide si el golpe fue válido.
7. El primero en llegar al puntaje objetivo gana.
8. El servidor anuncia al ganador y reinicia la partida.

## 4. Piezas clave

### Servidor

- `GameServerMain`: punto de arranque.
- `AlphaServerRuntime`: levanta broker, publicador, engine y TCP.
- `EmbeddedBrokerManager`: inicia ActiveMQ embebido.
- `TcpGameServer`: acepta conexiones concurrentes.
- `ClientSessionHandler`: atiende una conexión por cliente.
- `GameEngine`: genera monstruos, valida golpes y reinicia partidas.
- `PlayerRegistry`: guarda usuarios, sesiones y puntajes.

### Cliente

- `GameClientMain`: arranca el cliente.
- `GameClientController`: coordina UI, TCP y JMS.
- `ClientConnection`: canal TCP persistente.
- `GameFrame`: interfaz Swing.

### Estrés

- `StressTestMain`: ejecuta escenarios de carga.
- `StressClientWorker`: simula clientes.
- `StressSummary`: calcula métricas agregadas.

## 5. Qué lo hace valioso

- Tiene separación clara entre responsabilidades TCP y JMS.
- Arranca el broker ActiveMQ dentro del mismo servidor.
- Soporta múltiples clientes concurrentes.
- Permite reconexión del jugador durante la partida actual.
- Incluye una herramienta de evaluación experimental con salida CSV.
- Es fácil de defender porque la arquitectura está explícita y bien separada.

## 6. Archivos importantes

- `README.md`: explicación detallada del funcionamiento completo.
- `src/main/resources/alpha.properties`: configuración central.
- `scripts/run-server.sh`: levanta el servidor.
- `scripts/run-client.sh`: abre un cliente.
- `scripts/run-stress.sh`: ejecuta la prueba de estrés.
- `samples/stress-results-example.csv`: ejemplo de resultados.

## 7. Cómo se ejecuta

Servidor:

```bash
./scripts/run-server.sh
```

Cliente:

```bash
./scripts/run-client.sh
```

Estrés:

```bash
./scripts/run-stress.sh
```

## 8. Limitaciones principales

- No hay persistencia si el servidor se apaga.
- No hay base de datos.
- No hay pruebas automatizadas incluidas.
- La seguridad es básica y suficiente solo para contexto académico.

## 9. Idea central para explicar el proyecto

Si hubiera que describir Alpha en una sola frase:

Es un juego distribuido multicliente donde el servidor mantiene el estado global, TCP valida acciones críticas, JMS sincroniza eventos compartidos y una interfaz Swing permite demostrar el comportamiento en tiempo real.
