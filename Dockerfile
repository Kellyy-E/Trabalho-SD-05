# Estágio 1: Build da aplicação com Maven e Java 17
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copia arquivo de configuração do Maven para cache de dependências
COPY pom.xml .
RUN mvn dependency:resolve -B

# Copia código-fonte e recursos
COPY src ./src

# Compila e gera o fat JAR com todas as dependências incluídas (via maven-shade-plugin)
RUN mvn clean package -DskipTests

# Estágio 2: Imagem final leve para execução (JRE 17 Alpine)
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copia o JAR compilado do estágio de build
COPY --from=builder /build/target/app.jar /app/app.jar

# Variáveis padrão
ENV JAVA_OPTS="-Xms64m -Xmx256m"
ENV MAIN_CLASS="com.distribuidos.producer.MessageProducer"

# Permite iniciar com classe especificada por variável MAIN_CLASS ou via argumento de linha de comando
ENTRYPOINT ["sh", "-c", "TARGET_CLASS=${MAIN_CLASS:-$1}; exec java $JAVA_OPTS -cp /app/app.jar ${TARGET_CLASS:-com.distribuidos.producer.MessageProducer}", "--"]

