package microservices.book.multiplication.configuration;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The <em>Exchange</em> declaration for an <em>ement publisher</em>.
 * [N]:rabbitmq - Configures RabbitMQ via AMQP abstraction to use events in our application.
 */
@Configuration
public class AMQPConfiguration {

    /**
     * Declares the <em>exchange</em> owned by this <em>publisher</em>.
     * RabbitMQ is smart enough to handle the context of multiple instances of the same service: the first service will create the <em>exchange</em> and the other instances will refer to it. RabbitMQ will do the load balancing between the instances.
     * @param exchangeName [N]:rabbitmq - The name is picked up from configuration  (i.e. application.properties: amqp.exchange.attemps) thanks to the @Value annotation.
     * @return
     */
    @Bean
    public TopicExchange challengesTopicExchange(
            @Value("${amqp.exchange.attempts}") final String exchangeName) {
        return ExchangeBuilder
            // [N]:rabbitmq - We declare it as a "topic exchange" (see Exchange Types and Routing - chap.7 in Learn Microservices with Spring Boot) since that’s the solution we envisioned in our event-driven system.
            .topicExchange(exchangeName)
            // [N]:rabbitmq - We make the topic durable, so it’ll remain in the broker after RabbitMQ restarts. 
            .durable(true).build();
    }

    /**
     * To switch the predefined serialization format to JSON
     * @return [N]:rabbitmq - a JSON object serializer to avoid various pitfalls of the Java object serialization.
     */
    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
