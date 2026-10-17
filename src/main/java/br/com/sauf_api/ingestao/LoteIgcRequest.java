package br.com.sauf_api.ingestao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

/** Lote do importador geral do IGC (sauf-scraper igc). Faixa null = "SC", sem conceito (D-49). */
public record LoteIgcRequest(
        @NotNull @Min(2000) Integer ano,
        @NotNull List<@Valid Instituicao> instituicoes) {

    public record Instituicao(
            @NotNull @Positive Integer codigoEmec,
            @DecimalMin("0") @DecimalMax("5") BigDecimal igcContinuo,
            @Min(1) @Max(5) Integer igcFaixa) {}
}
