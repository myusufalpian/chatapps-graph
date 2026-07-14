package id.xyz.chatapps_graph;

import id.xyz.chatapps_graph.integration.RedisIntegrationBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("integration")
class ChatappsGraphApplicationTests extends RedisIntegrationBase {

	@Test
	void contextLoads() {
	}

}
