package br.com.sauf_api.usuario;

public class FotoInvalidaException extends RuntimeException {

    public FotoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
