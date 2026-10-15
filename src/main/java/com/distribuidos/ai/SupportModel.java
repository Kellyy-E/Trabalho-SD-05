package com.distribuidos.ai;

import java.util.ArrayList;
import java.util.List;

public class SupportModel {
    public static final String ACESSO = "Problema de acesso";
    public static final String DESEMPENHO = "Problema de desempenho";
    public static final String CONEXAO = "Problema de conexão";

    public static final String[] LABELS = { ACESSO, DESEMPENHO, CONEXAO };

    private final TextClassifier classifier;

    public SupportModel() {
        List<TextClassifier.TrainingSample> samples = new ArrayList<>();

        // 0: Problema de acesso
        samples.add(new TextClassifier.TrainingSample("Nao consigo fazer login na plataforma, erro de credenciais invalidas.", 0));
        samples.add(new TextClassifier.TrainingSample("Esqueci minha senha e o email de recuperacao de conta nao chega.", 0));
        samples.add(new TextClassifier.TrainingSample("Usuario bloqueado apos tres tentativas incorretas de autenticacao.", 0));
        samples.add(new TextClassifier.TrainingSample("Acesso negado e permissao insuficiente ao tentar abrir o painel de administrador.", 0));
        samples.add(new TextClassifier.TrainingSample("Erro 401 nao autorizado ao tentar acessar a area restrita da conta.", 0));
        samples.add(new TextClassifier.TrainingSample("Falha na validacao do token de login duplo fator autenticacao MFA.", 0));
        samples.add(new TextClassifier.TrainingSample("Nao consigo entrar no sistema com minhas credenciais de usuario.", 0));
        samples.add(new TextClassifier.TrainingSample("Cadastro expirado ou desativado, solicitando redefinicao de acesso e senha.", 0));
        samples.add(new TextClassifier.TrainingSample("Tela de login com erro de autenticacao, usuario invalido.", 0));
        samples.add(new TextClassifier.TrainingSample("Permissao negada, precisa liberar meu acesso ao modulo financeiro.", 0));

        // 1: Problema de desempenho
        samples.add(new TextClassifier.TrainingSample("O sistema esta extremamente lento e travando durante as consultas.", 1));
        samples.add(new TextClassifier.TrainingSample("Tempo de resposta muito alto ao gerar relatorios e exportar dados.", 1));
        samples.add(new TextClassifier.TrainingSample("Pagina demorando varios minutos para carregar, alto consumo de memoria.", 1));
        samples.add(new TextClassifier.TrainingSample("Queda de performance brusca na aplicacao, lentidao geral no sistema.", 1));
        samples.add(new TextClassifier.TrainingSample("A tela congela toda vez que clico no botao de processamento pesado.", 1));
        samples.add(new TextClassifier.TrainingSample("Latencia insuportavel no dashboard, graficos demoram para carregar.", 1));
        samples.add(new TextClassifier.TrainingSample("Uso de CPU em cem por cento no servidor, processamento muito lento.", 1));
        samples.add(new TextClassifier.TrainingSample("Gargalo no processamento, operacoes levando dez vezes mais tempo.", 1));
        samples.add(new TextClassifier.TrainingSample("Aplicacao muito pesada e com atraso na resposta de todas as requisicoes.", 1));
        samples.add(new TextClassifier.TrainingSample("Lentidao extrema para abrir formulários e salvar cadastros.", 1));

        // 2: Problema de conexão
        samples.add(new TextClassifier.TrainingSample("Erro de conexao com o banco de dados remoto, socket fechado.", 2));
        samples.add(new TextClassifier.TrainingSample("Timeout ao tentar comunicar com o servidor da API externa.", 2));
        samples.add(new TextClassifier.TrainingSample("Perda constante de pacotes na rede e desconexao frequente da sessao.", 2));
        samples.add(new TextClassifier.TrainingSample("Erro 504 Gateway Timeout e falha ao estabelecer conexao TCP.", 2));
        samples.add(new TextClassifier.TrainingSample("Servidor inalcancavel, DNS nao consegue resolver o endereco IP.", 2));
        samples.add(new TextClassifier.TrainingSample("Queda repentina da conexao VPN e interrupcao de rede com a matriz.", 2));
        samples.add(new TextClassifier.TrainingSample("Host nao encontrado, conexao recusada na porta de rede 8080.", 2));
        samples.add(new TextClassifier.TrainingSample("Falha intermitente no link de internet e perda de conexao WebSocket.", 2));
        samples.add(new TextClassifier.TrainingSample("Nao conecta ao gateway de comunicacao, tempo limite esgotado de rede.", 2));
        samples.add(new TextClassifier.TrainingSample("Instabilidade na rota de rede, comunicacao offline com o servidor.", 2));

        this.classifier = new TextClassifier(LABELS, samples, 3);
    }

    public TextClassifier.ClassificationResult classify(String text) {
        return classifier.classify(text);
    }
}

