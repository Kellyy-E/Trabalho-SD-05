package com.distribuidos.consumer.support;

import com.distribuidos.ai.SupportModel;
import com.distribuidos.ai.TextClassifier;
import com.distribuidos.config.RabbitMQConfig;
import com.distribuidos.model.Message;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class SupportConsumer {
    private static final Logger logger = LoggerFactory.getLogger(SupportConsumer.class);

    public static void main(String[] args) {
        logger.info("=== Iniciando Consumidor 2: Classificação de Solicitações de Suporte (Smile IA) ===");

        long delayMs = Long.parseLong(System.getenv().getOrDefault("PROCESSING_DELAY_MS", "800"));
        logger.info("Atraso intencional por mensagem: {} ms (Permite visualizar crescimento da fila no RabbitMQ)", delayMs);

        // Inicializa e treina o modelo de IA Smile
        SupportModel supportModel = new SupportModel();

        ConnectionFactory factory = RabbitMQConfig.createConnectionFactory();

        try {
            Connection connection = RabbitMQConfig.connectWithRetry(factory, 20, 3000);
            Channel channel = connection.createChannel();

            // Assegura topologia no broker
            RabbitMQConfig.initTopology(channel);

            // Controla a taxa de entrega de mensagens (QoS = 1)
            channel.basicQos(1);

            logger.info("Aguardando mensagens na fila '{}'...", RabbitMQConfig.QUEUE_SUPPORT);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                long deliveryTag = delivery.getEnvelope().getDeliveryTag();
                long startTime = System.currentTimeMillis();

                try {
                    String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    Message message = RabbitMQConfig.OBJECT_MAPPER.readValue(json, Message.class);

                    // Atraso proposital no processamento
                    if (delayMs > 0) {
                        Thread.sleep(delayMs);
                    }

                    // REGRA DE DEMONSTRAÇÃO DA DLQ:
                    // Se a mensagem possui flag de erro simulado ou prefixo de erro forçado, disparamos a exceção!
                    if (message.isSimulatedError() || message.getContent().contains("[ERRO_SIMULADO]")) {
                        throw new RuntimeException("FALHA INTENCIONAL: Payload corrompido ou erro fatal de processamento detectado para teste da DLQ.");
                    }

                    // Classificação via biblioteca Smile
                    TextClassifier.ClassificationResult result = supportModel.classify(message.getContent());

                    long duration = System.currentTimeMillis() - startTime;

                    System.out.println("================================================================================");
                    System.out.println(" [CONSUMIDOR-SUPORTE] Solicitação Técnica Classificada com Sucesso!");
                    System.out.println("  * ID Mensagem:    " + message.getId());
                    System.out.println("  * Data/Hora:      " + message.getTimestamp());
                    System.out.println("  * Conteúdo:       \"" + message.getContent() + "\"");
                    System.out.println("  * Categoria:      [" + result.getLabel().toUpperCase() + "] (" + String.format("%.1f", result.getConfidence() * 100) + "% confiança)");
                    System.out.println("  * Tempo de proc:  " + duration + " ms");
                    System.out.println("  * Confirmação:    Manual ACK enviado -> DeliveryTag=" + deliveryTag);
                    System.out.println("================================================================================");

                    // Manual ACK após sucesso
                    channel.basicAck(deliveryTag, false);

                } catch (Exception e) {
                    long duration = System.currentTimeMillis() - startTime;
                    System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    System.err.println(" [ALERTA DLQ - ERRO FORÇADO NO CONSUMIDOR DE SUPORTE]");
                    System.err.println("  * DeliveryTag:    " + deliveryTag);
                    System.err.println("  * Motivo:         " + e.getMessage());
                    System.err.println("  * Ação tomada:    channel.basicNack(deliveryTag, false, requeue=false)");
                    System.err.println("  * Roteamento DLQ: O RabbitMQ moverá a mensagem para a fila 'dead_letter.queue' via 'messages.dlx'!");
                    System.err.println("  * Tempo decorrido: " + duration + " ms");
                    System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

                    // Manual NACK SEM REQUEUE (requeue = false) -> O RabbitMQ redireciona para a Dead Letter Exchange configurada!
                    try {
                        channel.basicNack(deliveryTag, false, false);
                    } catch (IOException ex) {
                        logger.error("Erro ao enviar basicNack: ", ex);
                    }
                }
            };

            // autoAck = false -> Exige confirmação manual (manual ACK)
            channel.basicConsume(RabbitMQConfig.QUEUE_SUPPORT, false, deliverCallback, consumerTag -> {
                logger.warn("Consumo cancelado pelo broker: {}", consumerTag);
            });

            // Mantém thread principal ativa
            Thread.currentThread().join();

        } catch (InterruptedException e) {
            logger.info("Consumidor de suporte interrompido.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Erro fatal no SupportConsumer: ", e);
        }
    }
}
