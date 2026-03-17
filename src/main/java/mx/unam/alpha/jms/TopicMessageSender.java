package mx.unam.alpha.jms;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TopicMessageSender implements AutoCloseable {

    private final Connection connection;
    private final Session session;
    private final Map<String, MessageProducer> producers = new ConcurrentHashMap<>();

    public TopicMessageSender(String brokerUrl) {
        try {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException exception) {
            throw new IllegalStateException("No se pudo abrir el productor JMS en " + brokerUrl, exception);
        }
    }

    public synchronized void publish(String topicName, String payload) {
        try {
            MessageProducer producer = producers.computeIfAbsent(topicName, this::createProducer);
            TextMessage message = session.createTextMessage();
            message.setText(payload);
            producer.send(message);
        } catch (JMSException exception) {
            throw new IllegalStateException("No se pudo publicar en el tópico " + topicName, exception);
        }
    }

    private MessageProducer createProducer(String topicName) {
        try {
            Topic topic = session.createTopic(topicName);
            return session.createProducer(topic);
        } catch (JMSException exception) {
            throw new IllegalStateException("No se pudo crear el productor del tópico " + topicName, exception);
        }
    }

    @Override
    public void close() {
        for (MessageProducer producer : producers.values()) {
            try {
                producer.close();
            } catch (JMSException ignored) {
            }
        }
        try {
            session.close();
        } catch (JMSException ignored) {
        }
        try {
            connection.close();
        } catch (JMSException ignored) {
        }
    }
}
