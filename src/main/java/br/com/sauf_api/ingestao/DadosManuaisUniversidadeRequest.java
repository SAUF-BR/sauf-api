package br.com.sauf_api.ingestao;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Bloco manual de uma universidade (sauf-scraper/dados/ies/<sigla>.toml, D-98). Substitui tudo a cada envio. */
public record DadosManuaisUniversidadeRequest(
        @Schema(description = "Ano da criação legal (D-102)", example = "1969")
        @Min(value = 1500, message = "Ano de fundação inválido")
        @Max(value = 2100, message = "Ano de fundação inválido")
        Integer anoFundacao,
        @Schema(description = "De 1 a 5 parágrafos, texto puro (D-103)")
        @Size(max = 5, message = "O sobre pode ter até 5 parágrafos")
        List<@NotBlank(message = "Parágrafo vazio") @Size(max = 2000, message = "Parágrafo com mais de 2000 caracteres") String> sobre,
        @Size(max = 500)
        @Pattern(regexp = "https?://\\S+", message = "Informe o endereço completo do site, com http:// ou https://")
        String siteUrl,
        @Schema(description = "null apaga o logo atual") @Valid ImagemRequest logo,
        @Schema(description = "null apaga a capa atual") @Valid ImagemRequest capa) {}
