package br.com.sauf_api.pagamento;

public class FalhaNoGatewayException extends RuntimeException {

    public FalhaNoGatewayException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
