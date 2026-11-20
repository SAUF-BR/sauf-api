package br.com.sauf_api.ingestao;

public class AreaNaoCadastradaException extends RuntimeException {

    public AreaNaoCadastradaException(String codigoCine) {
        super("Área Cine " + codigoCine + " não está cadastrada. Rode o import do Censo antes.");
    }
}
