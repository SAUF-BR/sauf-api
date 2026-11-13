package br.com.sauf_api.comum;

import java.util.Map;

public class DadosInvalidosException extends RuntimeException {

    private final Map<String, String> campos;

    public DadosInvalidosException(String campo, String mensagem) {
        super("Dados inválidos");
        this.campos = Map.of(campo, mensagem);
    }

    public Map<String, String> campos() {
        return campos;
    }
}
