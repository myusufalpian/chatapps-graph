package id.xyz.chatapps_graph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ChatappsGraphApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatappsGraphApplication.class, args);
	}

}