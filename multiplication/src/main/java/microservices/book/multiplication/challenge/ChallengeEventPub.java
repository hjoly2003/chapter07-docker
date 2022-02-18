package microservices.book.multiplication.challenge;

import com.rabbitmq.client.MessageProperties;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * [N]:rabbitmq - A component to publish an event.
 */
@Service
public class ChallengeEventPub {

    private final AmqpTemplate amqpTemplate;
    private final String challengesTopicExchange;

    /**
     * [N]:rabbitmq - Constructor injection to wire an AmqpTemplate object and the name of the exchange.
     * @param amqpTemplate AmqpTemplate is just an interface defining the AMQP standards. The underlying implementation is RabbitTemplate, and it uses the JSON converter configured in AMQPConfiguration#producerJackson2MessageConverter
     * @param challengesTopicExchange The name of the exchange (picked from application.properties: amqp.exchange.attemps).
     * [?] How the AmqpTemplate is passed to this constructor?
     */
    public ChallengeEventPub(final AmqpTemplate amqpTemplate,
                             @Value("${amqp.exchange.attempts}")
                             final String challengesTopicExchange) {
        this.amqpTemplate = amqpTemplate;
        this.challengesTopicExchange = challengesTopicExchange;
    }

    /**
     * [N]:rabbitmq - Translates the domain object to the event object using the auxiliary method buildEvent, and it uses the amqpTemplate to convert (to JSON) and send the event with a given routing key. 
     * @param challengeAttempt
     */
    public void challengeSolved(final ChallengeAttempt challengeAttempt) {
        ChallengeSolvedEvent event = buildEvent(challengeAttempt);
        // Routing Key is 'attempt.correct' or 'attempt.wrong'
        String routingKey = "attempt." + (event.isCorrect() ?
                "correct" : "wrong");

        // [N]:rabbitmq - The default Spring implementation uses the persistent delivery mode while publishing all messages.     
        amqpTemplate.convertAndSend(challengesTopicExchange,
                    routingKey,
                    event);
/* 
        // [N]:rabbitmq - Here's an example of how we could change the delivery mode to make our messages not survive a broker restart.
        RabbitTemplate rabbitTemplate = (RabbitTemplate)amqpTemplate;        
        MessageProperties properties = MessagePropertiesBuilder.newInstance()
                                            .setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
                                            .build();
        rabbitTemplate.getMessageConverter().toMessage(challengeAttempt, properties);
        rabbitTemplate.convertAndSend(challengesTopicExchange, routingKey, event);
 */
    }

    private ChallengeSolvedEvent buildEvent(final ChallengeAttempt attempt) {
        return new ChallengeSolvedEvent(attempt.getId(),
                attempt.isCorrect(), attempt.getFactorA(),
                attempt.getFactorB(), attempt.getUser().getId(),
                attempt.getUser().getAlias());
    }
}
