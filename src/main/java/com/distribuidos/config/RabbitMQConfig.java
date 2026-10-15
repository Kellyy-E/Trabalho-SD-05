package com.distribuidos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class RabbitMQConfig {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConfig.class);

    // Exchanges
    public static final String EXCHANGE_TOPIC = "messages.topic";
    public static final String EXCHANGE_DLX = "messages.dlx";

    // Routing Keys
    public static final String ROUTING_KEY_REVIEW = "review.product";
    public static final String ROUTING_KEY_SUPPORT = "support.request";
    public static final String ROUTING_KEY_REVIEW_DLQ = "review.dead";
    public static final String ROUTING_KEY_SUPPORT_DLQ = "support.dead";
    public static final String ROUTING_KEY_DLQ_ALL = "#";

    // Filas
    public static final String QUEUE_REVIEWS = "reviews.queue";
    public static final String QUEUE_SUPPORT = "support.queue";
    public static final String QUEUE_DLQ = "dead_letter.queue";

    // JSON Mapper compartilhado
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Retorna a ConnectionFactory com os parâmetros configurados via variáveis de ambiente.
     */
    public static ConnectionFactory createConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(System.getenv().getOrDefault("RABBITMQ_HOST", "localhost"));
        factory.setPort(Integer.parseInt(System.getenv().getOrDefault("RABBITMQ_PORT", "5672")));
        factory.setUsername(System.getenv().getOrDefault("RABBITMQ_USER", "guest"));
        factory.setPassword(System.getenv().getOrDefault("RABBITMQ_PASSWORD", "guest"));
        factory.setVirtualHost(System.getenv().getOrDefault("RABBITMQ_VHOST", "/"));
        factory.setAutomaticRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(5000);
        return factory;
    }

    /**
     * Estabelece conexão com RabbitMQ aplicando política de retentativa resiliente.
     */
    public static Connection connectWithRetry(ConnectionFactory factory, int maxAttempts, long waitMs) 
            throws InterruptedException, IOException, TimeoutException {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                logger.info("Tentando conectar ao RabbitMQ ({}:{}) - Tentativa {}/{}", 
                        factory.getHost(), factory.getPort(), attempt, maxAttempts);
                return factory.newConnection();
            } catch (IOException | TimeoutException e) {
                if (attempt >= maxAttempts) {
                    logger.error("Falha ao conectar no RabbitMQ após {} tentativas.", maxAttempts, e);
                    throw e;
                }
                logger.warn("RabbitMQ ainda não disponível. Aguardando {}ms antes da próxima tentativa...", waitMs);
                Thread.sleep(waitMs);
            }
        }
    }

    /**
     * Inicializa a topologia completa do RabbitMQ:
     * - Topic Exchange principal
     * - Dead Letter Exchange (DLX)
     * - Fila de Dead Letter (DLQ)
     * - Fila de Avaliações (com encaminhamento DLQ)
     * - Fila de Suporte (com encaminhamento DLQ)
     */
    public static void initTopology(Channel channel) throws IOException {
        logger.info("Declarando topologia de mensageria no RabbitMQ...");

        // 1. Declarar Dead Letter Exchange (DLX) do tipo Topic
        channel.exchangeDeclare(EXCHANGE_DLX, BuiltinExchangeType.TOPIC, true);

        // 2. Declarar Fila de Dead Letter (DLQ)
        channel.queueDeclare(QUEUE_DLQ, true, false, false, null);

        // 3. Vincular fila DLQ à DLX para capturar todas as mensagens mortas (#)
        channel.queueBind(QUEUE_DLQ, EXCHANGE_DLX, ROUTING_KEY_DLQ_ALL);

        // 4. Declarar Topic Exchange Principal
        channel.exchangeDeclare(EXCHANGE_TOPIC, BuiltinExchangeType.TOPIC, true);

        // 5. Argumentos para vincular as filas principais à DLQ
        Map<String, Object> reviewQueueArgs = new HashMap<>();
        reviewQueueArgs.put("x-dead-letter-exchange", EXCHANGE_DLX);
        reviewQueueArgs.put("x-dead-letter-routing-key", ROUTING_KEY_REVIEW_DLQ);

        // Declarar fila de reviews com DLQ e vincular à exchange
        channel.queueDeclare(QUEUE_REVIEWS, true, false, false, reviewQueueArgs);
        channel.queueBind(QUEUE_REVIEWS, EXCHANGE_TOPIC, ROUTING_KEY_REVIEW);

        // 6. Argumentos para a fila de suporte com DLQ
        Map<String, Object> supportQueueArgs = new HashMap<>();
        supportQueueArgs.put("x-dead-letter-exchange", EXCHANGE_DLX);
        supportQueueArgs.put("x-dead-letter-routing-key", ROUTING_KEY_SUPPORT_DLQ);

        // Declarar fila de suporte com DLQ e vincular à exchange
        channel.queueDeclare(QUEUE_SUPPORT, true, false, false, supportQueueArgs);
        channel.queueBind(QUEUE_SUPPORT, EXCHANGE_TOPIC, ROUTING_KEY_SUPPORT);

        logger.info("Topologia configurada com sucesso: Exchange={}, DLX={}, Fila Avaliações={}, Fila Suporte={}, DLQ={}",
                EXCHANGE_TOPIC, EXCHANGE_DLX, QUEUE_REVIEWS, QUEUE_SUPPORT, QUEUE_DLQ);
    }
}
