package br.com.sauf_api.ingestao;

import java.util.ArrayList;
import java.util.List;

public class RespostaIngestao {

    private int criados;
    private int atualizados;
    private int inativados;
    private final List<String> rejeitados = new ArrayList<>();

    void criado() {
        criados++;
    }

    void atualizado() {
        atualizados++;
    }

    void inativado() {
        inativados++;
    }

    void rejeitado(String motivo) {
        rejeitados.add(motivo);
    }

    public int getCriados() {
        return criados;
    }

    public int getAtualizados() {
        return atualizados;
    }

    public int getInativados() {
        return inativados;
    }

    public List<String> getRejeitados() {
        return rejeitados;
    }
}
