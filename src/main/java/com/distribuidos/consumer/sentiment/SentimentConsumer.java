package com.distribuidos.consumer.sentiment;

import com.distribuidos.ai.SentimentModel;
import com.distribuidos.ai.TextClassifier;
import com.distribuidos.config.RabbitMQConfig;
import com.distribuidos.model.Message;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SentimentConsumer {
    private static final Logger logger = LoggerFactory.getLogger(SentimentConsumer.class);

    public static void main(String[] args) {
        logger.info("=== Iniciando Consumidor 1: Análise de Sentimento (Smile IA) ===");

        long delayMs = Long.parseLong(System.getenv().getOrDefault("PROCESSING_DELAY_MS", "800"));
        logger.info("Atraso intencional por mensagem: {} ms (Gera acúmulo visível na fila RabbitMQ)", delayMs);

        // Inicializa e treina o modelo de IA Smile
        SentimentModel sentimentModel = new SentimentModel();

        ConnectionFactory factory = RabbitMQConfig.createConnectionFactory();

        try {
            Connection connection = RabbitMQConfig.connectWithRetry(factory, 20, 3000);
            Channel channel = connection.createChannel();

            // Assegura topologia no broker
            RabbitMQConfig.initTopology(channel);

            // Garante que o consumidor receba apenas 1 mensagem por vez antes de enviar o ACK manual,
            // permitindo que o backlog acumule visivelmente na fila do broker
            channel.basicQos(1);

            logger.info("Aguardando mensagens na fila '{}'...", RabbitMQConfig.QUEUE_REVIEWS);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                long deliveryTag = delivery.getEnvelope().getDeliveryTag();
                long startTime = System.currentTimeMillis();

                try {
                    String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    Message message = RabbitMQConfig.OBJECT_MAPPER.readValue(json, Message.class);

                    // Atraso proposital no processamento para provocar acúmulo na fila
                    if (delayMs > 0) {
                        Thread.sleep(delayMs);
                    }

                    // Classificação via biblioteca Smile
                    TextClassifier.ClassificationResult result = sentimentModel.classify(message.getContent());

                    long duration = System.currentTimeMillis() - startTime;

                    System.out.println("--------------------------------------------------------------------------------");
                    System.out.println(" [CONSUMIDOR-SENTIMENTO] Avaliação Processada com Sucesso!");
                    System.out.println("  * ID Mensagem:    " + message.getId());
                    System.out.println("  * Data/Hora:      " + message.getTimestamp());
                    System.out.println("  * Conteúdo:       \"" + message.getContent() + "\"");
                    System.out.println("  * Classificação:  [" + result.getLabel().toUpperCase() + "] (" + String.format("%.1f", result.getConfidence() * 100) + "% confiança)");
                    System.out.println("  * Tempo de proc:  " + duration + " ms");
                    System.out.println("  * Confirmação:    Manual ACK enviado -> DeliveryTag=" + deliveryTag);
                    System.out.println("--------------------------------------------------------------------------------");

                    // Envio do ACK MANUAL após sucesso
                    channel.basicAck(deliveryTag, false);

                } catch (Exception e) {
                    logger.error("Erro ao processar mensagem de avaliação: ", e);
                    // Em caso de falha não esperada, rejeita e envia para DLQ
                    try {
                        channel.basicNack(deliveryTag, false, false);
                    } catch (IOException ex) {
                        logger.error("Erro ao rejeitar mensagem: ", ex);
                    }
                }
            };

            // autoAck = false -> Exige confirmação manual (manual ACK)
            channel.basicConsume(RabbitMQConfig.QUEUE_REVIEWS, false, deliverCallback, consumerTag -> {
                logger.warn("Consumo cancelado pelo broker: {}", consumerTag);
            });

            // Mantém thread principal viva
            Thread.currentThread().join();

        } catch (InterruptedException e) {
            logger.info("Consumidor de sentimento interrompido.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Erro fatal no SentimentConsumer: ", e);
        }
    }
}
