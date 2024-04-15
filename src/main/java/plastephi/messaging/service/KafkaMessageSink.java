package plastephi.messaging.service;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class KafkaMessageSink {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaConsumer<String,String> mgmtInfoConsumer;
    public final String topicName;

    public KafkaMessageSink(
            KafkaTemplate<String, String> kafkaTemplate
            , KafkaConsumer<String,String> mgmtInfoConsumer
            , Collection<String> topicNames) {
        this.kafkaTemplate = kafkaTemplate;
        this.mgmtInfoConsumer = mgmtInfoConsumer;
        this.topicName = topicNames.stream().findFirst().orElseThrow();
    }

    public void sendMessage(String msg) {
        this.sendMessage(msg, 0);
    }

    public void sendMessage(String msg, Integer partition){
        kafkaTemplate.send(kafkaTemplate.getDefaultTopic(),  partition, null,  msg);
        kafkaTemplate.flush();
    }

    public int getTopicPartitions(){
        return mgmtInfoConsumer.partitionsFor(topicName).size();
    }
}
