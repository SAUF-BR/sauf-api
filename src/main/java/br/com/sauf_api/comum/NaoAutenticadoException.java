package br.com.sauf_api.comum;

public class NaoAutenticadoException extends RuntimeException {

    public NaoAutenticadoException() {
        super("Entre na sua conta para continuar");
    }
}
