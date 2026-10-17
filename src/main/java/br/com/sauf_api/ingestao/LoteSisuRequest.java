package br.com.sauf_api.ingestao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** Lote do importador do Sisu (sauf-scraper sisu): só ampla concorrência; nota null = sem corte (D-50). */
public record LoteSisuRequest(
        @NotNull @Min(2000) Integer ano,
        @NotNull List<@Valid Linha> linhas) {

    public record Linha(
            @NotNull @Positive Integer codigoEmecInstituicao,
            @NotNull @Positive Integer codigoEmecCurso,
            @NotBlank @Size(max = 20) String turno,
            @PositiveOrZero Integer vagas,
            @DecimalMin(value = "0", inclusive = false) @DecimalMax("1000") BigDecimal nota,
            @PositiveOrZero Integer inscricoes) {}
}
