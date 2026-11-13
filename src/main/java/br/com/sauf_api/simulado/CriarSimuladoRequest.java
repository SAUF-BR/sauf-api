package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;
import br.com.sauf_api.questao.Banca;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CriarSimuladoRequest(
        @NotNull(message = "Escolha a banca") Banca banca,
        @NotNull(message = "Escolha o tamanho do simulado") TamanhoSimulado tamanho,
        @NotEmpty(message = "Escolha ao menos uma área") List<@NotNull(message = "Área inválida") AreaQuestao> areas,
        @NotNull(message = "Informe se o simulado é cronometrado") Boolean cronometrado) {}
