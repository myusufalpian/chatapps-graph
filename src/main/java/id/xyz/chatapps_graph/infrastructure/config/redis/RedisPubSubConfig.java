package id.xyz.chatapps_graph.infrastructure.config.redis;

import id.xyz.chatapps_graph.infrastructure.service.RedisChatEventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisPubSubConfig {

  public static final String CHAT_EVENTS_TOPIC = "pubsub:chat-events";

  @Bean
  public ChannelTopic channelTopic() {
    return new ChannelTopic(CHAT_EVENTS_TOPIC);
  }

  @Bean
  public RedisMessageListenerContainer redisMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      RedisChatEventListener chatEventListener,
      ChannelTopic channelTopic) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(chatEventListener, channelTopic);
    return container;
  }
}
