package plastephi.messaging.service;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import plastephi.messaging.strategy.IPartitionStrategy;
import plastephi.messaging.helper.JsonHelper;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

@Component
public class MqSeriesListener implements MessageListener {
    private static final Logger _logger = LoggerFactory.getLogger(MqSeriesListener.class);

    private final KafkaMessageSink kafkaMessageSink;
    private final IPartitionStrategy partitionChooser;

    public MqSeriesListener(KafkaMessageSink kafkaMessageSink, IPartitionStrategy partitionChooser) {
        this.kafkaMessageSink = kafkaMessageSink;
        this.partitionChooser = partitionChooser;
    }


    /**
     * Consume each MqSeries text messages and write it into Kafka sink.
     * @param message the text message passed to the listener
     */
    public void onMessage(Message message) {
        if (message instanceof TextMessage) {
            try {
                String text = ((TextMessage) message).getText();

                if (JsonHelper.isValidJson(text)) {
                    // todo -> cofigure key
                    final Long jsonUniqueValue = JsonHelper
                            .getSortedFirstElement(new JSONObject(text), "key", Long.class)
                            .orElse(0L);
                    int partition = partitionChooser.apply(kafkaMessageSink.getTopicPartitions(), jsonUniqueValue);
                    _logger.info("value {} for partition {}", jsonUniqueValue, partition);
                    kafkaMessageSink.sendMessage(text, partition);
                } else {
                    kafkaMessageSink.sendMessage(text);
                }
            } catch (JMSException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            String errorMessage = "Message type must be a TextMessage";
            _logger.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
    }

}
