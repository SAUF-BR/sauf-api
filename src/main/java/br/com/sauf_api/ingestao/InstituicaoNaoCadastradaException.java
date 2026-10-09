package br.com.sauf_api.ingestao;

public class InstituicaoNaoCadastradaException extends RuntimeException {

    public InstituicaoNaoCadastradaException(Integer codigoEmec) {
        super("Instituição com código e-MEC " + codigoEmec + " não está cadastrada. Rode o import do Censo antes.");
    }
}
