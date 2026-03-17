package mx.unam.alpha.jms;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.util.function.Consumer;

public class AsyncTopicReceiver implements AutoCloseable {

    private final String brokerUrl;
    private final String topicName;
    private Connection connection;
    private Session session;
    private MessageConsumer messageConsumer;

    public AsyncTopicReceiver(String brokerUrl, String topicName) {
        this.brokerUrl = brokerUrl;
        this.topicName = topicName;
    }

    public void start(Consumer<String> consumer) {
        try {
            close();
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(topicName);
            messageConsumer = session.createConsumer(topic);
            messageConsumer.setMessageListener(message -> onMessage(message, consumer));
        } catch (JMSException exception) {
            close();
            throw new IllegalStateException("No se pudo abrir la suscripción " + topicName, exception);
        }
    }

    private void onMessage(Message message, Consumer<String> consumer) {
        if (!(message instanceof TextMessage textMessage)) {
            return;
        }
        try {
            consumer.accept(textMessage.getText());
        } catch (JMSException exception) {
            throw new IllegalStateException("No se pudo consumir el mensaje JMS", exception);
        }
    }

    @Override
    public void close() {
        try {
            if (messageConsumer != null) {
                messageConsumer.close();
                messageConsumer = null;
            }
        } catch (JMSException ignored) {
        }
        try {
            if (session != null) {
                session.close();
                session = null;
            }
        } catch (JMSException ignored) {
        }
        try {
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (JMSException ignored) {
        }
    }
}
