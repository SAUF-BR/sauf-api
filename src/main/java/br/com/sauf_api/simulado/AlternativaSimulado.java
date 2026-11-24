package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.Alternativa;

/** Alternativa sem o gabarito. */
public record AlternativaSimulado(String rotulo, String texto) {

    static AlternativaSimulado de(Alternativa alternativa) {
        return new AlternativaSimulado(alternativa.getRotulo(), alternativa.getTexto());
    }
}
