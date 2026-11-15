package br.com.sauf_api.auth;

import java.time.Duration;

/**
 * Para que serve o código enviado por e-mail e por quanto tempo ele vale (D-90).
 * A verificação de e-mail no cadastro ficou para depois (D-89) e deve entrar aqui.
 */
public enum TipoTokenEmail {
    // Nome mantido (o banco guarda o texto): hoje é o código de acesso do "esqueci minha senha"
    REDEFINICAO_SENHA(Duration.ofMinutes(10));

    private final Duration validade;

    TipoTokenEmail(Duration validade) {
        this.validade = validade;
    }

    public Duration validade() {
        return validade;
    }
}
