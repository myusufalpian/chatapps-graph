package id.xyz.chatapps_graph.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class MetricsFacade {
  private final MeterRegistry registry;
  private final AtomicInteger activeConnections = new AtomicInteger(0);

  public MetricsFacade(MeterRegistry registry) {
    this.registry = registry;
    Gauge.builder("chat.websocket.active.connections", activeConnections, AtomicInteger::get)
        .description("Number of active WebSocket connections")
        .register(registry);
  }

  public void incrementMessagesSent(String conversationType) {
    Counter.builder("chat.messages.sent.total")
        .tag("conversation_type", conversationType)
        .register(registry)
        .increment();
  }

  public void recordConnection() {
    activeConnections.incrementAndGet();
  }

  public void recordDisconnect() {
    activeConnections.updateAndGet(value -> Math.max(0, value - 1));
  }

  public void incrementErrors(String exceptionClass) {
    Counter.builder("chat.errors.total")
        .tag("exception_class", exceptionClass)
        .register(registry)
        .increment();
  }

  public void recordRabbitMQLatency(String taskType, String status, long durationMs) {
    Timer.builder("chat.rabbitmq.processing.duration")
        .tag("task_type", taskType)
        .tag("status", status)
        .register(registry)
        .record(java.time.Duration.ofMillis(durationMs));
  }

  public void incrementExports(String status) {
    Counter.builder("chat.exports.status.total")
        .tag("status", status)
        .register(registry)
        .increment();
  }
}
