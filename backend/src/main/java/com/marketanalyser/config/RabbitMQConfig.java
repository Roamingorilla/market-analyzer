package com.marketanalyser.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queue names
    public static final String QUEUE_MARKET_DATA_REFRESH  = "market.data.refresh";
    public static final String QUEUE_HISTORICAL_INGEST    = "market.historical.ingest";
    public static final String QUEUE_CACHE_WARMING        = "market.cache.warming";
    public static final String QUEUE_QUOTA_MONITOR        = "market.quota.monitor";
    public static final String EXCHANGE_MARKET            = "market.exchange";
    public static final String DLQ_SUFFIX                 = ".dlq";

    // Queues
    @Bean
    public Queue marketDataRefreshQueue() {
        return QueueBuilder.durable(QUEUE_MARKET_DATA_REFRESH)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_MARKET_DATA_REFRESH + DLQ_SUFFIX)
                .build();
    }

    @Bean
    public Queue historicalIngestQueue() {
        return QueueBuilder.durable(QUEUE_HISTORICAL_INGEST)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", QUEUE_HISTORICAL_INGEST + DLQ_SUFFIX)
                .build();
    }

    @Bean
    public Queue cacheWarmingQueue() {
        return QueueBuilder.durable(QUEUE_CACHE_WARMING).build();
    }

    @Bean
    public Queue quotaMonitorQueue() {
        return QueueBuilder.durable(QUEUE_QUOTA_MONITOR).build();
    }

    // Dead letter queues
    @Bean public Queue marketDataRefreshDlq() { return new Queue(QUEUE_MARKET_DATA_REFRESH + DLQ_SUFFIX, true); }
    @Bean public Queue historicalIngestDlq()  { return new Queue(QUEUE_HISTORICAL_INGEST + DLQ_SUFFIX, true); }

    // Exchange
    @Bean
    public TopicExchange marketExchange() {
        return new TopicExchange(EXCHANGE_MARKET, true, false);
    }

    // Bindings
    @Bean
    public Binding marketDataRefreshBinding() {
        return BindingBuilder.bind(marketDataRefreshQueue()).to(marketExchange()).with("market.refresh.#");
    }

    @Bean
    public Binding historicalIngestBinding() {
        return BindingBuilder.bind(historicalIngestQueue()).to(marketExchange()).with("market.ingest.#");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
