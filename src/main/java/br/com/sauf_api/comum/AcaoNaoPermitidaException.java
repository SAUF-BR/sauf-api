package br.com.sauf_api.comum;

public class AcaoNaoPermitidaException extends RuntimeException {

    public AcaoNaoPermitidaException(String mensagem) {
        super(mensagem);
    }
}
