package com.distribuidos.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;

    private MessageType type;
    private String content;
    private boolean simulatedError;

    // Construtor padrão para o Jackson
    public Message() {
    }

    public Message(MessageType type, String content) {
        this(type, content, false);
    }

    public Message(MessageType type, String content, boolean simulatedError) {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.type = type;
        this.content = content;
        this.simulatedError = simulatedError;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isSimulatedError() {
        return simulatedError;
    }

    public void setSimulatedError(boolean simulatedError) {
        this.simulatedError = simulatedError;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", timestamp=" + timestamp +
                ", type=" + type +
                ", content='" + content + '\'' +
                ", simulatedError=" + simulatedError +
                '}';
    }
}

