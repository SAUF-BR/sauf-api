package br.com.sauf_api.calendario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record EventoRequest(
        @NotBlank @Size(max = 200) String titulo,
        @NotNull TipoEvento tipo,
        @NotNull LocalDate inicio,
        @Schema(description = "Último dia do período; omitir quando o evento é de um dia só") LocalDate fim,
        @Schema(description = "Código e-MEC da universidade; omitir nos eventos nacionais (ENEM, ProUni)")
        @Positive Integer codigoEmecInstituicao,
        @Size(max = 1000) String descricao,
        @NotBlank @Size(max = 500) @Pattern(regexp = "https?://\\S+", message = "deve ser um link http(s)")
        String fonteUrl,
        @Schema(description = "Ano da edição (ex.: 2027 no Vestibular 2027, 2026 no ENEM 2026)")
        @NotNull @Min(2000) @Max(2100) Integer anoReferencia) {}
