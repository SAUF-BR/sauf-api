package br.com.sauf_api.documento;

import br.com.sauf_api.comum.EnumComValor;

/** Documentos legais que a pessoa aceita no cadastro (D-93). No JSON: "termos-uso" e "politica-privacidade". */
public enum TipoDocumento implements EnumComValor {
    TERMOS_USO("termos-uso", "os Termos de Uso"),
    POLITICA_PRIVACIDADE("politica-privacidade", "a Política de Privacidade");

    private final String valor;
    private final String nomeNaFrase;

    TipoDocumento(String valor, String nomeNaFrase) {
        this.valor = valor;
        this.nomeNaFrase = nomeNaFrase;
    }

    @Override
    public String valor() {
        return valor;
    }

    /** Para mensagens: "Aceite os Termos de Uso…". */
    public String nomeNaFrase() {
        return nomeNaFrase;
    }
}
