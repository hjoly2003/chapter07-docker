package microservices.book.gamification.game;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import microservices.book.gamification.challenge.ChallengeSolvedEvent;

/**
 * The <em>event consumer</em>.
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class GameEventHandler {

    private final GameService gameService;

    /**
     * Uses the {@code @RabbitListener} annotation to act as the processing logic of a <em>message</em> when it arrives.<p/>
     * In this annotation, we only need to specify the queue name to subscribe to, since we already declared all RabbitMQ entities in a separate configuration file.
     * @param event A {@code ChallengeSolvedEvent} object. Spring automatically configures a deserializer to transform the message from the broker in to this object type. We configured the deserializer to use JSON in {@code AMQPConfiguration}. 
     * @see <a href="https://docs.spring.io/spring-amqp/docs/current/reference/html/#async-annotation-driven">AMQP Annotation-driven Listener Endpoints</a>
     * @see org.springframework.amqp.rabbit.annotation.RabbitListener
     */
    @RabbitListener(queues = "${amqp.queue.gamification}") // [N]:rabbitmq - amqp.queue.gamification is picked up from  application.properties.
    void handleMultiplicationSolved(final ChallengeSolvedEvent event) {
        log.info("Challenge Solved Event received: {}", event.getAttemptId());
        try {
            gameService.newAttemptForUser(event);
        } catch (final Exception e) {
            log.error("Error when trying to process ChallengeSolvedEvent", e);
            // Avoids the event to be re-queued and reprocessed.
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }

}
