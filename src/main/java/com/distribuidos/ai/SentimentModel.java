package com.distribuidos.ai;

import java.util.ArrayList;
import java.util.List;

public class SentimentModel {
    public static final String POSITIVA = "Positiva";
    public static final String NEGATIVA = "Negativa";
    public static final String NEUTRA = "Neutra";

    public static final String[] LABELS = { POSITIVA, NEGATIVA, NEUTRA };

    private final TextClassifier classifier;

    public SentimentModel() {
        List<TextClassifier.TrainingSample> samples = new ArrayList<>();

        // Amostras Positivas (Classe 0)
        samples.add(new TextClassifier.TrainingSample("Produto excelente, superou todas as expectativas!", 0));
        samples.add(new TextClassifier.TrainingSample("Adorei a compra, qualidade maravilhosa e entrega muito rapida.", 0));
        samples.add(new TextClassifier.TrainingSample("Muito bom, funciona perfeitamente sem nenhum problema ou defeito.", 0));
        samples.add(new TextClassifier.TrainingSample("Melhor produto que ja comprei, recomendo fortemente a todos.", 0));
        samples.add(new TextClassifier.TrainingSample("Sensacional, acabamento impecavel e otimo custo beneficio.", 0));
        samples.add(new TextClassifier.TrainingSample("Estou muito satisfeito, nota dez para o atendimento e produto incrivel.", 0));
        samples.add(new TextClassifier.TrainingSample("Perfeito! Chegou antes do prazo e muito bem embalado e bonito.", 0));
        samples.add(new TextClassifier.TrainingSample("Excelente desempenho, muito rapido e bateria dura muito tempo.", 0));
        samples.add(new TextClassifier.TrainingSample("Simplesmente maravilhoso, recomendo de olhos fechados.", 0));
        samples.add(new TextClassifier.TrainingSample("Otima qualidade e durabilidade, compra fantastica.", 0));

        // Amostras Negativas (Classe 1)
        samples.add(new TextClassifier.TrainingSample("Pessimo produto, quebrou no primeiro dia de uso.", 1));
        samples.add(new TextClassifier.TrainingSample("Muito ruim, nao funciona e o suporte nao resolveu o problema.", 1));
        samples.add(new TextClassifier.TrainingSample("Horrivel, dinheiro jogado no lixo, totalmente decepcionado.", 1));
        samples.add(new TextClassifier.TrainingSample("Pior experiencia possivel, produto veio estragado e com defeito grave.", 1));
        samples.add(new TextClassifier.TrainingSample("Qualidade pessima, material fragil, plastico barato e nao recomendo.", 1));
        samples.add(new TextClassifier.TrainingSample("Propaganda enganosa, produto falso e nao condiz com as fotos.", 1));
        samples.add(new TextClassifier.TrainingSample("Terrivel, parou de funcionar em menos de uma semana de uso.", 1));
        samples.add(new TextClassifier.TrainingSample("Muito insatisfeito, demorou semanas e veio danificado e quebrado.", 1));
        samples.add(new TextClassifier.TrainingSample("Nao comprem, defeituoso, esquenta demais e nao liga.", 1));
        samples.add(new TextClassifier.TrainingSample("Decepcao total, estragou rapido e atendimento pos venda e pessimo.", 1));

        // Amostras Neutras (Classe 2)
        samples.add(new TextClassifier.TrainingSample("O produto foi entregue dentro da data prevista pelos correios.", 2));
        samples.add(new TextClassifier.TrainingSample("Produto razoavel, atende ao basico conforme a descricao do anuncio.", 2));
        samples.add(new TextClassifier.TrainingSample("Chegou em ordem, nada de extraordinario mas funciona sem problemas.", 2));
        samples.add(new TextClassifier.TrainingSample("Nem bom nem ruim, preco compativel com o material basico entregue.", 2));
        samples.add(new TextClassifier.TrainingSample("Produto comum para uso no dia a dia, desempenho regular e mediano.", 2));
        samples.add(new TextClassifier.TrainingSample("Embalagem padrao dos correios, recebido conforme anunciado na loja.", 2));
        samples.add(new TextClassifier.TrainingSample("Tudo nos conformes sem grandes destaques positivos ou observacoes negativas.", 2));
        samples.add(new TextClassifier.TrainingSample("Cumpre o que promete de forma simples e basica para a finalidade.", 2));
        samples.add(new TextClassifier.TrainingSample("Entrega no prazo regular, manual de instrucoes impresso em portugues.", 2));
        samples.add(new TextClassifier.TrainingSample("Equipamento dentro da media de mercado, operacao normal.", 2));

        this.classifier = new TextClassifier(LABELS, samples, 3);
    }

    public TextClassifier.ClassificationResult classify(String text) {
        return classifier.classify(text);
    }
}

