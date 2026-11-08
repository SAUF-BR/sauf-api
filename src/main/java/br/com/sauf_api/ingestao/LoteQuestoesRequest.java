package br.com.sauf_api.ingestao;

import br.com.sauf_api.questao.AreaQuestao;
import br.com.sauf_api.questao.Banca;
import br.com.sauf_api.questao.Lingua;
import br.com.sauf_api.questao.StatusQuestao;
import br.com.sauf_api.questao.TipoQuestao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Lote do importador de questões (sauf-scraper questoes): uma prova (caderno de um dia) por lote (D-68). */
public record LoteQuestoesRequest(
        @NotNull @Valid Prova prova,
        @NotNull List<@Valid Questao> questoes) {

    public record Prova(
            @NotNull Banca banca,
            @NotNull @Min(1990) Integer ano,
            @NotBlank @Size(max = 20) String edicao,
            @NotBlank @Size(max = 30) String caderno,
            String urlOrigem,
            @NotBlank String credito) {}

    public record Questao(
            @NotNull @Positive Integer numero,
            Lingua lingua,
            @NotNull TipoQuestao tipo,
            @NotNull AreaQuestao area,
            @Size(max = 50) String materia,
            @Size(max = 100) String assunto,
            Integer habilidade,
            String textoBase,
            @NotBlank String comando,
            Integer gabaritoSoma,
            BigDecimal dificuldade,
            boolean anulada,
            @NotNull StatusQuestao status,
            @Size(max = 500) String motivoPendencia,
            Integer codigoOrigem,
            @NotBlank String credito,
            @NotEmpty List<@Valid Alternativa> alternativas) {}

    public record Alternativa(
            @NotBlank @Size(max = 2) String rotulo,
            Integer valor,
            @NotNull String texto,
            boolean correta) {}
}
