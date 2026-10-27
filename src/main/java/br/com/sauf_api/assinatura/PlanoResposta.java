package br.com.sauf_api.assinatura;

import java.math.BigDecimal;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public record PlanoResposta(
    @Schema(description = "Código do plano: basic, plus ou premium") String id,
    String nome,
    BigDecimal precoMensal,
    String descricao,
    List<RecursoPlano> recursos,
    String selo){

    public static PlanoResposta de(Plano plano){
        return new PlanoResposta(
            plano.valor(),
            plano.getNome(),
            plano.getPrecoMensal(),
            plano.getDescricao(),
            plano.getRecursos(),
            plano.getSelo());
        }
    }
