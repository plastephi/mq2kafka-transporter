package plastephi.messaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

@SpringBootApplication
@EnableJms
public class Mq2KafkaTransportApplication {

	public static void main(String[] args) {
		SpringApplication.run(Mq2KafkaTransportApplication.class, args).start();
	}

}
