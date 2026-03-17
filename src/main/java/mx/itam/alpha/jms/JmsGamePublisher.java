package mx.itam.alpha.jms;

import mx.itam.alpha.common.config.AppConfig;
import mx.itam.alpha.common.model.GlobalGameEvent;
import mx.itam.alpha.common.model.MonsterSpawnEvent;
import mx.itam.alpha.common.util.JsonUtils;

public class JmsGamePublisher implements AutoCloseable {

    private final AppConfig config;
    private final TopicMessageSender sender;

    public JmsGamePublisher(AppConfig config) {
        this.config = config;
        this.sender = new TopicMessageSender(config.getJmsBrokerUrl());
    }

    public void publishMonster(MonsterSpawnEvent event) {
        sender.publish(config.getMonsterTopic(), JsonUtils.toJson(event));
    }

    public void publishEvent(GlobalGameEvent event) {
        sender.publish(config.getEventsTopic(), JsonUtils.toJson(event));
    }

    public void publishWinner(GlobalGameEvent event) {
        sender.publish(config.getWinnerTopic(), JsonUtils.toJson(event));
    }

    @Override
    public void close() {
        sender.close();
    }
}
