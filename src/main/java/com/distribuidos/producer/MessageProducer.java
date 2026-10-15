package com.distribuidos.producer;

import com.distribuidos.config.RabbitMQConfig;
import com.distribuidos.model.Message;
import com.distribuidos.model.MessageType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class MessageProducer {
    private static final Logger logger = LoggerFactory.getLogger(MessageProducer.class);

    // Banco de mensagens simuladas de Avaliação de Produtos (Positivas, Negativas e Neutras)
    private static final String[] REVIEW_TEMPLATES = {
            "Gostei muito do produto, a qualidade do acabamento é excepcional e a bateria dura o dia todo!",
            "Excelente aquisição! Chegou bem antes do prazo esperado e atendeu plenamente minhas expectativas.",
            "Produto maravilhoso, fácil de usar e com ótimo custo-benefício. Super recomendo!",
            "Péssima compra, o produto quebrou na primeira utilização e o material é de baixíssima qualidade.",
            "Muito ruim! Não funcionou conforme anunciado e o suporte não respondeu minha solicitação.",
            "Decepcionante, veio com defeito de fábrica e embalagem totalmente amassada. Não recomendo.",
            "O produto foi entregue dentro da data prevista pelos correios, sem nenhuma novidade.",
            "Produto mediano. Funciona conforme a descrição básica do anúncio, atende para tarefas simples.",
            "Qualidade regular compatível com o preço pago. Nada de especial, cumpre o básico.",
            "Sensacional! Um dos melhores produtos que já comprei, design moderno e alta performance.",
            "Horrível, parou de ligar após três dias. Dinheiro completamente jogado fora.",
            "Pacote recebido em ordem. Embalagem padrão e manual de instruções incluso."
    };

    // Banco de mensagens simuladas de Solicitação de Suporte Técnico
    // (Acesso, Desempenho, Conexão e mensagens com erro simulado para teste de DLQ)
    private static final String[] SUPPORT_TEMPLATES = {
            "Não consigo fazer login na plataforma corporativa, o sistema informa que a senha é inválida.",
            "Esqueci minha senha de acesso e o link de recuperação de conta não é enviado para meu e-mail.",
            "Minha conta foi bloqueada após tentativas de login, necessito de desbloqueio urgente.",
            "Acesso negado com erro 401 não autorizado ao tentar visualizar o módulo financeiro.",
            "O sistema está extremamente lento e travando toda vez que clico em gerar relatórios em PDF.",
            "Tempo de resposta insuportável no painel administrativo, a página demora minutos para carregar.",
            "Queda drástica de desempenho no processamento de pedidos após a última atualização.",
            "A tela congela constantemente ao tentar salvar novas informações no banco de dados.",
            "Erro de conexão com o banco de dados remoto, conexão foi terminada inesperadamente pelo host.",
            "Timeout ao tentar se comunicar com o gateway da API externa de pagamento.",
            "Perda constante de pacotes na rede e interrupção na conexão TCP com o servidor.",
            "DNS não resolve o endereço do servidor de produção, gerando erro 504 Gateway Timeout."
    };

    // Mensagens propositalmente defeituosas para simular envio para DLQ
    private static final String[] DLQ_ERROR_TEMPLATES = {
            "[ERRO_SIMULADO] Payload corrompido: Falha de integridade criptográfica no token de autenticação.",
            "[ERRO_SIMULADO] Formato binário ilegível recebido no campo de dados do suporte técnico.",
            "[ERRO_SIMULADO] Erro crítico de transação não recuperável no pipeline de atendimento."
    };

    public static void main(String[] args) {
        logger.info("=== Iniciando Gerador Contínuo de Mensagens (Producer) ===");

        long sleepIntervalMs = Long.parseLong(System.getenv().getOrDefault("MESSAGE_RATE_MS", "150"));
        logger.info("Taxa de envio configurada: intervalo de {} ms (~{:.1f} mensagens/segundo)", 
                sleepIntervalMs, (1000.0 / sleepIntervalMs));

        ConnectionFactory factory = RabbitMQConfig.createConnectionFactory();
        Random random = new Random();

        try (Connection connection = RabbitMQConfig.connectWithRetry(factory, 20, 3000);
             Channel channel = connection.createChannel()) {

            // Inicializa a topologia no broker
            RabbitMQConfig.initTopology(channel);

            long count = 0;
            long errorCount = 0;
            long startTime = System.currentTimeMillis();

            while (true) {
                count++;
                // Alterna entre Avaliação (review.product) e Suporte (support.request)
                boolean isReview = random.nextBoolean();

                String routingKey;
                Message message;

                if (isReview) {
                    routingKey = RabbitMQConfig.ROUTING_KEY_REVIEW;
                    String content = REVIEW_TEMPLATES[random.nextInt(REVIEW_TEMPLATES.length)];
                    message = new Message(MessageType.REVIEW, content);
                } else {
                    routingKey = RabbitMQConfig.ROUTING_KEY_SUPPORT;
                    // A cada ~8 mensagens de suporte, força uma mensagem com erro para testar a DLQ
                    boolean triggerError = (count % 8 == 0);
                    String content;
                    if (triggerError) {
                        content = DLQ_ERROR_TEMPLATES[random.nextInt(DLQ_ERROR_TEMPLATES.length)];
                        errorCount++;
                    } else {
                        content = SUPPORT_TEMPLATES[random.nextInt(SUPPORT_TEMPLATES.length)];
                    }
                    message = new Message(MessageType.SUPPORT, content, triggerError);
                }

                byte[] body = RabbitMQConfig.OBJECT_MAPPER.writeValueAsBytes(message);

                channel.basicPublish(
                        RabbitMQConfig.EXCHANGE_TOPIC,
                        routingKey,
                        null,
                        body
                );

                if (message.isSimulatedError()) {
                    logger.warn("[PRODUCER -> DLQ CANDIDATE] #{} [{}] RoutingKey={} ID={} | Conteúdo='{}'",
                            count, message.getType(), routingKey, message.getId(), message.getContent());
                } else {
                    logger.info("[PRODUCER] #{} [{}] RoutingKey={} ID={} | Conteúdo='{}'",
                            count, message.getType(), routingKey, message.getId(),
                            message.getContent().length() > 60 ? message.getContent().substring(0, 57) + "..." : message.getContent());
                }

                if (count % 50 == 0) {
                    double elapsedSec = (System.currentTimeMillis() - startTime) / 1000.0;
                    double rate = count / elapsedSec;
                    logger.info(">>> ESTATÍSTICAS PRODUCER: {} mensagens enviadas ({} msgs de erro para DLQ) em {:.1f}s | Taxa média: {:.2f} msgs/s <<<",
                            count, errorCount, elapsedSec, rate);
                }

                Thread.sleep(sleepIntervalMs);
            }

        } catch (InterruptedException e) {
            logger.info("Gerador de mensagens interrompido.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Erro fatal no MessageProducer: ", e);
        }
    }
}
