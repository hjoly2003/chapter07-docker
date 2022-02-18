package microservices.book.gamification.configuration;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

/**
 * Defines an AMQP <em>queue</em> for an <em>event subscriber</em>.<p/>
 * [N]:rabbitmq - Configures RabbitMQ via AMQP abstraction to use events in our application.
 */
@Configuration
public class AMQPConfiguration {

    /**
     * Declares the <em>exchange</em> on the <em>consumer</em>'s side.<p/>
     * Even though this <em>subscriber</em> doesn’t own the <em>exchange</em> conceptually, we want our microservices to be able to start in any given order: this is to support the case where the Gamification service would start before the Multiplication service, since the start of the <em>queue</em> requires the existence of the <em>exchange</em>.<p/>
     * RabbitMQ is smart enough to handle the context of multiple instances of the same service: for both the <em>exchange</em> and the <em>queue</em>, the first service will create them and the other instances will refer to them. RabbitMQ will do the load balancing between the instances.
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
     * @param queueName [N]:rabbitmq - The name is picked up from configuration  (i.e. application.properties: amqp.queue.gamification) thanks to the @Value annotation.
     * @return
     */
    @Bean
    public Queue gamificationQueue(
            @Value("${amqp.queue.gamification}") final String queueName) {
/* 
        return QueueBuilder.durable(queueName)
            // Configures the messages to be kept in queue for 6 hours before discarding them (time-to-live, TTL).
            .ttl((int) Duration.ofHours(6).toMillis())
            // Keeps a max length of 25000 messages. 
            .maxLength(25000)
            .build();
 */
            return QueueBuilder.durable(queueName).build();
    }

    /**
     * Binds the <em>exchange</em> with the <em>queue</em>.<p/>
     * The Bean’s declaration method for the Binding uses the two other beans, injected by Spring, and links them with the value {@code attempt.correct}.
     * @param gamificationQueue
     * @param attemptsExchange
     * @return
     */
    @Bean
    public Binding correctAttemptsBinding(final Queue gamificationQueue,
                                          final TopicExchange attemptsExchange) {
        return BindingBuilder.bind(gamificationQueue)
                .to(attemptsExchange)
                .with("attempt.correct");
    }

    /**
     * Deserializes the <em>messages</em> using JSON.<p/>
     * We actually use the default factory as a baseline but then replace its <em>message converter</em> by a {@code MappingJackson2MessageConverter} instance, which handles the message deserialization from JSON to Java classes.
     * @return
     */
    @Bean
    public MessageHandlerMethodFactory messageHandlerMethodFactory() {
        DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();

        final MappingJackson2MessageConverter jsonConverter =
                new MappingJackson2MessageConverter();

        // [N]:json - We fine-tune the jsonConverter's ObjectMapper and add the ParameterNamesModule to avoid having to use empty constructors for our event classes. 
        // [N]:json - The JsonCreator.Mode.PROPERTIES indicates that the argument(s) for the Object's creator are to be bound from matching properties of incoming Object value, using creator argument names (explicit or implicit) to match incoming Object properties to arguments.
        jsonConverter.getObjectMapper().registerModule(
                new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));

        factory.setMessageConverter(jsonConverter);
        return factory;
    }

    /**
     * Configures the <em>Rabbit Listeners</em> to use a JSON deserialization.<p\>
     * For doing this, we have to override the bean {@code RabbitListenerConfigurer} with an implementation that uses our custom {@code MessageHandlerMethodFactory}.
     * @param messageHandlerMethodFactory
     * @return
     */
    @Bean
    public RabbitListenerConfigurer rabbitListenerConfigurer(
            final MessageHandlerMethodFactory messageHandlerMethodFactory) {
        return (c) -> c.setMessageHandlerMethodFactory(messageHandlerMethodFactory);
    }

}
