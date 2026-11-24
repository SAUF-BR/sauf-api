package br.com.sauf_api.assinatura;

/** O plano da conta não inclui o recurso pedido (D-109): responde 403. */
public class RecursoForaDoPlanoException extends RuntimeException {

    public RecursoForaDoPlanoException() {
        super("Seu plano não inclui este recurso");
    }
}
