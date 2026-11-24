package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;
import br.com.sauf_api.questao.Banca;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * O total vem de um destes, nesta ordem (D-97): {@code quantidades} por área, {@code quantidade} livre
 * dividida entre as {@code areas}, ou um {@code tamanho} pronto dividido entre as {@code areas}.
 */
public record CriarSimuladoRequest(
        @NotNull(message = "Escolha a banca") Banca banca,
        TamanhoSimulado tamanho,
        @Schema(description = "Total livre de 1 a 100, dividido entre as áreas como o tamanho (D-97)")
        @Min(value = 1, message = "O simulado precisa ter ao menos 1 questão")
        @Max(value = SimuladoService.MAXIMO_QUESTOES, message = "O simulado pode ter até 100 questões")
        Integer quantidade,
        List<@NotNull(message = "Área inválida") AreaQuestao> areas,
        @Schema(description = "Questões por área, ex.: {\"lc\": 5, \"mt\": 10}; o total é a soma (D-97)")
        Map<String, Integer> quantidades,
        @NotNull(message = "Informe se o simulado é cronometrado") Boolean cronometrado,
        Integer segundosPorQuestao) {}
