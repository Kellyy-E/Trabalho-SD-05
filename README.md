# Sistema Distribuído de Classificação Inteligente de Mensagens

Sistema distribuído desenvolvido em **Java 17**, **Docker Compose**, **RabbitMQ** (Topic Exchange, Dead Letter Queue e Manual ACK) e **Inteligência Artificial (Smile)** para classificação automática de mensagens via NLP.

---

## 🚀 Como Executar o Projeto (Passo a Passo)

### Pré-requisito
- Ter apenas o **Docker Desktop** instalado e em execução (não é necessário instalar Java ou Maven na máquina host).

### 1. Iniciar os Containers
Na raiz do projeto, execute o comando:
```bash
docker compose up --build
```
> O Docker compilará a aplicação em Java 17, empacotará o JAR com as dependências do RabbitMQ e Smile, e iniciará os 4 containers na mesma rede.

### 2. Acessar o Painel de Gerenciamento do RabbitMQ
Abra o navegador no endereço:
- **URL:** [http://localhost:15672](http://localhost:15672)
- **Usuário:** `guest`
- **Senha:** `guest`

Na aba **Queues**, acompanhe em tempo real:
- `reviews.queue`: Mensagens de avaliações acumulando e sendo consumidas.
- `support.queue`: Mensagens de suporte técnico sendo processadas.
- `dead_letter.queue`: Mensagens com falha forçada sendo encaminhadas para a **DLQ**.

### 3. Comandos Úteis
- **Executar em segundo plano (modo detached):**
  ```bash
  docker compose up --build -d
  ```
- **Acompanhar os logs em tempo real:**
  ```bash
  docker compose logs -f
  ```
- **Ver logs apenas de um serviço específico:**
  ```bash
  docker compose logs -f consumer-sentiment
  docker compose logs -f consumer-support
  docker compose logs -f message-generator
  ```
- **Parar e remover todos os containers:**
  ```bash
  docker compose down
  ```


## 📦 Descrição dos 4 Containers

1. **`message-generator` (Container 1 - Produtor Java)**
   - Publica continuamente a uma taxa $\ge$ 5 msgs/s (~6.6 msgs/s, intervalo de 150ms).
   - Envia dois tipos de mensagens com identificador UUID, timestamp e conteúdo:
     - `review.product`: comentários simulados de avaliações de produtos.
     - `support.request`: relatos simulados de problemas técnicos (com injeção periódica de erros propositais para teste da DLQ).

2. **`rabbitmq-broker` (Container 2 - Broker RabbitMQ)**
   - Imagem oficial `rabbitmq:3-management` com painel web na porta `15672` e porta AMQP `5672`.
   - Contém a Topic Exchange `messages.topic`, a Dead Letter Exchange `messages.dlx` e as filas `reviews.queue`, `support.queue` e `dead_letter.queue`.

3. **`consumer-sentiment` (Container 3 - Consumidor 1: Sentimento)**
   - Consome da fila `reviews.queue`.
   - Classifica o sentimento em: **Positiva**, **Negativa** ou **Neutra** usando a biblioteca **Smile**.
   - Contém atraso proposital de 800ms por mensagem (`basicQos(1)`), sendo mais lento que o produtor para forçar acúmulo visível da fila no RabbitMQ.
   - Envia **Manual ACK** (`channel.basicAck`) após processar cada mensagem.

4. **`consumer-support` (Container 4 - Consumidor 2: Suporte & DLQ)**
   - Consome da fila `support.queue`.
   - Classifica o problema em: **Problema de acesso**, **Problema de desempenho** ou **Problema de conexão** usando a biblioteca **Smile**.
   - Processamento lento (800ms) com **Manual ACK**.
   - **Demonstração da DLQ**: detecta mensagens de erro forçado e executa `channel.basicNack(deliveryTag, false, requeue=false)`. O RabbitMQ encaminha automaticamente essas mensagens para a `dead_letter.queue`.

---

## 🧠 Modelo de Inteligência Artificial (Smile)

Para a classificação de texto em linguagem natural, foi utilizada a biblioteca **Smile (Statistical Machine Intelligence and Learning Engine)**:

1. **Pré-processamento (`TextClassifier.java`)**:
   - Conversão para minúsculas, remoção de acentos via normalização Unicode (NFD) e remoção de pontuações via Regex.
   - Tokenização e filtragem de termos curtos.
2. **Vetorização (Bag-of-Words Normalizado)**:
   - Construção de vocabulário e contagem de termos por mensagem.
   - Aplicação de **Normalização Euclidiana ($L_2$)** no vetor de frequências para eliminar distorções de tamanho do texto.
3. **Classificação Supervisionada K-NN (`smile.classification.KNN`)**:
   - O algoritmo calcula a distância euclidiana para os vizinhos mais próximos ($k=3$), inferindo a categoria com precisão e rapidez.

---

## 📂 Estrutura de Pastas

```text
Trabalho-SD-05/
├── docker-compose.yml              # Orquestração dos 4 serviços
├── Dockerfile                      # Multi-stage build (Maven + JRE 17 Alpine)
├── pom.xml                         # Dependências (RabbitMQ, Smile, Jackson, Logback)
├── README.md                       # Instruções de execução e documentação
├── .dockerignore                   # Arquivos ignorados no build
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── distribuidos/
        │           ├── model/
        │           │   ├── Message.java          # Entidade de dados (UUID, timestamp, tipo, conteúdo)
        │           │   └── MessageType.java      # Enum REVIEW e SUPPORT
        │           ├── config/
        │           │   └── RabbitMQConfig.java   # Topologia (Topic, DLQ, Filas) e conexão resiliente
        │           ├── ai/
        │           │   ├── TextClassifier.java   # Vetorizador Bag-of-Words e Smile K-NN
        │           │   ├── SentimentModel.java   # Modelo de Análise de Sentimento
        │           │   └── SupportModel.java     # Modelo de Classificação de Suporte
        │           ├── producer/
        │           │   └── MessageProducer.java  # Gerador contínuo de mensagens
        │           └── consumer/
        │               ├── sentiment/
        │               │   └── SentimentConsumer.java  # Consumidor de Sentimento (Manual ACK)
        │               └── support/
        │                   └── SupportConsumer.java    # Consumidor de Suporte (DLQ + Manual ACK)
        └── resources/
            └── logback.xml         # Configuração de logs
```
