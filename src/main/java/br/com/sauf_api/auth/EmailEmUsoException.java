package br.com.sauf_api.auth;

public class EmailEmUsoException extends RuntimeException {

    public EmailEmUsoException() {
        super("Já existe uma conta com este e-mail");
    }
}
