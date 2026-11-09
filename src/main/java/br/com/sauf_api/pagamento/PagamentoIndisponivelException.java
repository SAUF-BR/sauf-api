package br.com.sauf_api.pagamento;

public class PagamentoIndisponivelException extends RuntimeException {

    public PagamentoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
