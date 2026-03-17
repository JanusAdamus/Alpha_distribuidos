package mx.itam.alpha.server;

import mx.itam.alpha.common.config.AppConfig;
import org.apache.activemq.broker.BrokerService;

/**
 * Levanta y apaga el broker ActiveMQ embebido del proyecto.
 * Esto evita depender de una instalación externa durante la ejecución normal.
 */
public class EmbeddedBrokerManager implements AutoCloseable {

    private final AppConfig config;
    private BrokerService brokerService;

    public EmbeddedBrokerManager(AppConfig config) {
        this.config = config;
    }

    public synchronized void start() {
        if (brokerService != null) {
            return;
        }
        try {
            BrokerService broker = new BrokerService();
            // El broker es deliberadamente liviano porque se usa solo como canal interno del juego.
            broker.setBrokerName("alpha-broker");
            broker.setPersistent(false);
            broker.setUseJmx(false);
            broker.setAdvisorySupport(false);
            broker.addConnector(config.getJmsBrokerUrl());
            broker.start();
            broker.waitUntilStarted();
            brokerService = broker;
            System.out.println("ActiveMQ embebido listo en " + config.getJmsBrokerUrl());
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo iniciar ActiveMQ embebido", exception);
        }
    }

    @Override
    public synchronized void close() {
        if (brokerService == null) {
            return;
        }
        try {
            brokerService.stop();
            brokerService.waitUntilStopped();
        } catch (Exception ignored) {
        } finally {
            brokerService = null;
        }
    }
}
