# Reporte del Proyecto Alpha

## Contexto general

Alpha es un juego distribuido en Java en el que varios clientes compiten por golpear un monstruo que aparece en un tablero compartido. El sistema combina dos mecanismos de comunicación que cumplen funciones distintas y complementarias. Por un lado, utiliza TCP para las operaciones críticas que requieren una respuesta directa del servidor, como el registro, el inicio de sesión, la consulta del estado del juego y el envío de golpes. Por otro lado, utiliza JMS con ActiveMQ embebido para difundir eventos globales, como la aparición del monstruo, las actualizaciones del marcador, el anuncio del ganador y el reinicio automático de la partida.

Aunque el proyecto completo incluye la lógica de juego, la interfaz gráfica y el servidor, el eje principal de este reporte es el estresador experimental. Esta parte del sistema es especialmente importante porque permite evaluar el desempeño del proyecto bajo carga y producir la evidencia cuantitativa que exige la entrega.

## Objetivo del estresador

El estresador fue diseñado para responder una pregunta central: cómo se comporta el sistema cuando aumenta la concurrencia. En otras palabras, no basta con saber que el juego funciona con pocos clientes; también interesa medir cómo cambian los tiempos de respuesta, qué tan estables son esos tiempos y qué porcentaje de operaciones siguen siendo exitosas cuando el número de jugadores crece.

Desde esa perspectiva, el experimento busca observar dos dimensiones principales. La primera es el costo del registro e inicio de sesión, ya que esas operaciones representan la puerta de entrada al sistema. La segunda es el desempeño del juego en sí, especialmente la capacidad del servidor para aceptar golpes válidos cuando muchos clientes intentan competir por el mismo monstruo casi al mismo tiempo. Para lograrlo, el estresador ejecuta clientes sintéticos que siguen el flujo real del sistema y registra los resultados en archivos CSV que luego pueden analizarse o graficarse.

## Diseño general del estresador

El diseño del estresador está dividido en tres clases principales: `StressTestMain`, `StressClientWorker` y `StressSummary`. Esta separación permite distinguir claramente entre la coordinación del experimento, la simulación de cada cliente y la agregación estadística de los resultados.

`StressTestMain` funciona como orquestador. Su trabajo consiste en leer la configuración del experimento, generar las distintas corridas, crear el archivo CSV de salida y ejecutar las combinaciones de clientes y repeticiones definidas por el usuario. Esta clase no simula clientes directamente, sino que se encarga de planear el escenario y consolidar el resultado de cada repetición.

`StressClientWorker` representa a un cliente sintético. Cada instancia ejecuta el comportamiento de un jugador real: se conecta al servidor, intenta registrarse, abre una sesión de juego, consulta el estado del tablero y trata de golpear al monstruo activo. La relevancia de esta clase es que no realiza un benchmark artificial aislado, sino que ejerce el protocolo real del sistema. Gracias a eso, las métricas obtenidas reflejan el comportamiento combinado del servidor TCP, la coordinación de sesión y la lógica del juego.

`StressSummary`, por último, toma los resultados individuales de muchos workers y los convierte en métricas agregadas. Su papel es importante porque separa la ejecución del experimento de la estadística, lo que hace que el diseño sea más limpio y que los resultados sean más fáciles de interpretar.

## Implementación paso a paso

El flujo de una corrida comienza en `StressTestMain`. Esta clase toma como base la configuración general del proyecto y permite modificar varios parámetros desde argumentos de línea de comandos. Entre esos parámetros se encuentran el host, el puerto, la lista de tamaños de carga, el número de golpes por cliente, la cantidad de repeticiones, el tiempo de espera entre intentos y la ruta del archivo de salida. Una vez reunidos esos datos, la clase prepara el CSV y recorre cada configuración de clientes. Para cada caso, ejecuta tantas repeticiones como se hayan solicitado y escribe una fila nueva por cada repetición terminada.

Dentro de cada repetición, `StressTestMain` crea un `ExecutorService` y lanza varios `StressClientWorker` en paralelo. Cada worker genera un nombre de usuario único para evitar colisiones y después sigue dos fases. La primera fase mide el registro. En ella, el worker abre una conexión TCP, envía la petición `REGISTER`, toma el tiempo con `System.nanoTime()` y guarda si el registro fue exitoso. Si el registro funciona, el worker cierra esa sesión inicial y pasa a la segunda fase.

La segunda fase simula el juego. El worker abre una nueva conexión, realiza `LOGIN` y empieza a consultar repetidamente el estado del juego. Cuando el snapshot indica que hay un monstruo visible, toma la fila, la columna y el `monsterId` activo y envía un `HIT`. También en este caso mide el tiempo de respuesta y registra si el golpe fue aceptado o rechazado. Entre intentos, el worker espera unos milisegundos para introducir una pausa corta que modele mejor el comportamiento de un usuario real y evite convertir el experimento en un simple envío continuo de peticiones.

Cuando todos los workers de una repetición terminan, `StressSummary` fusiona sus resultados. En ese momento se calculan promedios, desviaciones estándar y porcentajes de éxito para registro y juego. Finalmente, `StressTestMain` formatea esos valores y los escribe en una fila del archivo CSV.

## Métricas producidas por el CSV

El CSV contiene una fila por repetición y una serie de columnas que resumen el comportamiento del sistema. Cada fila incluye la marca de tiempo de la corrida, la cantidad de clientes usados, el número de repetición y cuatro tipos de métricas: latencia promedio de registro, desviación estándar del registro, porcentaje de éxito del registro y sus equivalentes para la fase de juego.

En términos prácticos, `register_avg_ms` indica cuánto tarda en promedio una operación de registro o login bajo una carga determinada. `register_stddev_ms` muestra cuánta variación existe entre esos tiempos. `register_success_pct` representa el porcentaje de intentos que fueron aceptados correctamente. De forma análoga, `game_avg_ms`, `game_stddev_ms` y `game_success_pct` describen el comportamiento de la fase de golpe.

Estas métricas no deben leerse de manera aislada. Un promedio alto puede indicar sobrecarga o simplemente más trabajo interno del servidor. Una desviación estándar alta sugiere que el comportamiento se vuelve menos estable conforme aumenta la concurrencia. Un porcentaje de éxito bajo en registro podría revelar problemas de disponibilidad o saturación, mientras que un porcentaje de éxito bajo en juego suele estar más relacionado con la naturaleza competitiva del sistema, donde muchos clientes llegan tarde al mismo monstruo o pierden la carrera contra otro jugador.

## Interpretación de los resultados

La lectura correcta del CSV consiste en observar tendencias. Si el tiempo promedio de registro aumenta conforme crece el número de clientes, eso indica que el servidor está absorbiendo el costo natural de atender más conexiones concurrentes. Si además la desviación estándar crece, el sistema se vuelve menos uniforme y algunos clientes comienzan a experimentar respuestas más lentas que otros.

En la fase de juego, el significado es todavía más interesante. El tiempo de golpe no depende solo del procesamiento interno del servidor, sino también del momento preciso en que el monstruo está visible y de cuántos clientes compiten por él. Por eso, una caída del porcentaje de éxito en golpes no necesariamente significa que exista un error de lógica. En muchos casos refleja un efecto esperado de contención: varios clientes intentan golpear el mismo monstruo, pero solo uno puede reclamarlo de forma válida.

Dicho de otra manera, el estresador no solo sirve para saber si el sistema sigue funcionando, sino también para entender de qué forma se degrada bajo carga y qué parte de esa degradación es aceptable dentro de la lógica del juego.

## Análisis del CSV incluido

El repositorio incluye una muestra en [`samples/stress-results-example.csv`](../samples/stress-results-example.csv). Esa muestra contiene tres configuraciones representativas: 10, 50 y 100 clientes. Aunque no sustituye una campaña experimental completa, sí permite ver una tendencia bastante clara.

En primer lugar, el tiempo promedio de registro crece de manera sostenida al pasar de 10 a 50 y luego a 100 clientes. Lo mismo ocurre con la desviación estándar del registro. Esto sugiere que el servidor maneja bien la carga baja y media, pero empieza a mostrar más variabilidad conforme aumenta la concurrencia. Aun así, el porcentaje de éxito del registro se mantiene muy alto, lo cual indica que el mecanismo de autenticación sigue siendo funcional incluso en la carga más alta del ejemplo.

En la fase de juego se aprecia un comportamiento todavía más marcado. El tiempo promedio de golpe también aumenta con la cantidad de clientes, y la dispersión crece en la misma dirección. Además, el porcentaje de éxito de los golpes disminuye de forma visible. Esta caída es consistente con la lógica del sistema: cuando hay más clientes compitiendo por un único monstruo, es natural que más intentos lleguen tarde o se vuelvan inválidos porque otro cliente ya reclamó ese objetivo.

La conclusión razonable es que el sistema conserva su funcionalidad, pero la contención del juego crece con la carga y eso se refleja tanto en la latencia como en el porcentaje de éxito. Desde el punto de vista experimental, esa es precisamente la clase de comportamiento que el estresador debía evidenciar.

## Salidas y archivos relevantes

El proyecto trabaja con varios archivos CSV según el tipo de corrida. El repositorio conserva un archivo de ejemplo para documentación y comparación rápida, mientras que las corridas reales generan sus propios resultados en rutas de salida configurables. Por defecto, `StressTestMain` escribe en `samples/stress-results.csv`, y las corridas disparadas desde la interfaz del servidor suelen guardar en `samples/generated/stress-results.csv`. Cada fila nueva representa una repetición concreta y por eso esos archivos van acumulando evidencia experimental corrida tras corrida.

Además, la interfaz del servidor ahora permite no solo abrir el último CSV generado, sino también graficarlo de manera sencilla. Esa gráfica resume por cantidad de clientes la latencia promedio y el porcentaje de éxito tanto del registro como del juego, lo que vuelve mucho más fácil identificar tendencias sin necesidad de revisar el archivo completo línea por línea.

## Relación con el sistema completo

Aunque el foco de este reporte es el estresador, conviene recordar que su valor depende del diseño del sistema que está ejerciendo. El servidor se inicia en `GameServerMain`, donde se levanta `AlphaServerRuntime`. Esa clase ensambla el broker JMS embebido, el publicador de eventos, el registro de jugadores, el motor del juego y el servidor TCP. Del lado cliente, `GameClientMain` crea `GameClientController`, que autentica por TCP, recibe `SessionInfo`, abre las suscripciones JMS y actualiza la interfaz. Gracias a esta arquitectura, el estresador no prueba una simulación simplificada, sino el flujo real de registro, login, consulta de estado y golpe.

## Cierre

El aporte más importante del proyecto no es únicamente que el juego funcione, sino que además pueda medirse de forma sistemática. El estresador convierte la ejecución del sistema en evidencia cuantitativa. Permite repetir experimentos, variar la carga, generar archivos CSV comparables y observar cómo responde el sistema conforme crece la concurrencia. Eso le da al proyecto una capa adicional de valor académico y técnico, porque no solo presenta una solución funcional, sino también una forma clara de analizar su desempeño.
