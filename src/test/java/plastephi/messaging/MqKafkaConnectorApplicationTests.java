package plastephi.messaging;

import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

//@SpringBootTest
class MqKafkaConnectorApplicationTests {

	//@Test
	void contextLoads(ApplicationContext context) {
		assertThat(context).isNotNull();
	}

}
