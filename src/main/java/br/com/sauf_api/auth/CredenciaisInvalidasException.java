package br.com.sauf_api.auth;

/** Login recusado. Mensagem única para não revelar quais e-mails têm conta (D-84). */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha incorretos");
    }
}
