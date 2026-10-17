package br.com.sauf_api.ingestao;

/** Resposta da ingestão do Sisu: a de sempre mais as linhas cujo curso não está em nenhuma oferta. */
public class RespostaSisu extends RespostaIngestao {

    private int semOferta;

    void semOferta() {
        semOferta++;
    }

    public int getSemOferta() {
        return semOferta;
    }
}
