package plastephi.messaging.config;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import plastephi.messaging.strategy.IPartitionStrategy;
import plastephi.messaging.service.KafkaMessageSink;
import plastephi.messaging.service.MqSeriesListener;

import javax.jms.MessageListener;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

@Configuration
public class JmsKafkaConfig {
    private static final Logger _logger = LoggerFactory.getLogger(JmsKafkaConfig.class);

    @Value("${servers.mq.host}")
    private String host;
    @Value("${servers.mq.port}")
    private Integer port;
    @Value("${servers.mq.queue-manager}")
    private String queueManager;
    @Value("${servers.mq.channel}")
    private String channel;
    @Value("${servers.mq.queue}")
    private String queue;
    @Value("${servers.mq.timeout}")
    private long timeout;

    @Value("${servers.kafka.topic-name}")
    private String topicName;
    @Value("${servers.kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    @Value("${servers.kafka.security-protocol}")
    private String securtiyProtocol;
    @Value("${servers.kafka.sasl-mechanism}")
    private String saslMechanism;
    @Value("${servers.kafka.secAdmUser}")
    private String secAdmUser;
    @Value("${servers.kafka.secAdmPass}")
    private String secAdmPass;

    @Value("${listener.partition-delegate-class}")
    private String partitionDelegateClass;

    @Bean
    public Collection<String> getConsumerTopicNames() {
        return Collections.singletonList(topicName);
    }

    private Map<String, Object> getKafkaSecurityConfig(Map<String, Object> configProps) {
        configProps.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, securtiyProtocol);
        final String saslJaasText = format("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";", secAdmUser, secAdmPass);
        configProps.put(SaslConfigs.SASL_MECHANISM, saslMechanism);
        configProps.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasText);
        return configProps;
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps = getKafkaSecurityConfig(configProps);
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }


    @Bean
    public KafkaConsumer<String, String> mgmtInfoConsumer() {
        final String groupId = format("mgmt-info-consumer-group-%s", LocalDate.now());
        Map<String, Object> configProps = new HashMap<>();
        configProps = getKafkaSecurityConfig(configProps);
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(configProps);
        _logger.info("Management information consumer for topic '{}' established (no subscribing)", topicName);
        return consumer;
    }


    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(producerFactory());
        kafkaTemplate.setDefaultTopic(topicName);
        return kafkaTemplate;
    }


    /*
    ###############  IBM MQ  ########
     */
    @Bean
    @Primary
    public MQQueueConnectionFactory mqQueueConnectionFactory() {
        MQQueueConnectionFactory mqQueueConnectionFactory = new MQQueueConnectionFactory();
        try {
            mqQueueConnectionFactory.setHostName(host);
            mqQueueConnectionFactory.setQueueManager(queueManager);
            mqQueueConnectionFactory.setPort(port);
            mqQueueConnectionFactory.setChannel(channel);
            mqQueueConnectionFactory.setAppName(System.getProperty("user.name"));
            mqQueueConnectionFactory.setTransportType(1); // Client
            mqQueueConnectionFactory.setCCSID(1208);
        } catch (Exception e) {
            _logger.error("MQ connection parameter cannot set", e);
        }
        return mqQueueConnectionFactory;
    }

    @Bean
    public DefaultMessageListenerContainer queueContainer(MQQueueConnectionFactory mqQueueConnectionFactory) {
        MessageListener listener = mqSeriesListener();
        DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
        container.setConnectionFactory(mqQueueConnectionFactory);
        container.setDestinationName(queue);
        container.setMessageListener(listener);
        container.start();
        return container;
    }


    @Bean
    public JmsTemplate queueTemplate(MQQueueConnectionFactory mqQueueConnectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(mqQueueConnectionFactory);
        jmsTemplate.setPriority(1);
        return jmsTemplate;
    }


    /*
    ###############  Project beans  ########
    */
    @Bean
    public KafkaMessageSink kafkaMessageSink() {
        return new KafkaMessageSink(kafkaTemplate(), mgmtInfoConsumer(), getConsumerTopicNames());
    }

    @Bean
    public MqSeriesListener mqSeriesListener() {
        return new MqSeriesListener(kafkaMessageSink(), getPartitionStrategy());
    }


    @Bean
    public IPartitionStrategy getPartitionStrategy() {
        try {
            return (IPartitionStrategy) Class.forName(partitionDelegateClass).newInstance();
        } catch (Exception e) {
            _logger.error("using default strategy", e);
            return (partitions, value) -> 1;
        }
    }
}
