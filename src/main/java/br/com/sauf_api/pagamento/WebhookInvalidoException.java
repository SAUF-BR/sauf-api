package br.com.sauf_api.pagamento;

public class WebhookInvalidoException extends RuntimeException {

    public WebhookInvalidoException(String mensagem) {
        super(mensagem);
    }
}
