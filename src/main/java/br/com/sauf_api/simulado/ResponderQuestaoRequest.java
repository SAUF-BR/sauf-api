package br.com.sauf_api.simulado;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** {@code tempoSegundos} é o total acumulado na questão, não o que passou desde o último envio. */
public record ResponderQuestaoRequest(
        @Size(max = 2, message = "Alternativa inválida") String alternativa,
        @NotNull(message = "Informe a situação da questão") SituacaoQuestao situacao,
        @NotNull(message = "Informe o tempo") @PositiveOrZero(message = "O tempo não pode ser negativo")
                Integer tempoSegundos) {}
