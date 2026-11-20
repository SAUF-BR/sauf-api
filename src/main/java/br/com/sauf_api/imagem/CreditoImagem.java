package br.com.sauf_api.imagem;

import io.swagger.v3.oas.annotations.media.Schema;

/** Quem fez a imagem, sob qual licença e onde está a página do arquivo (ex.: no Wikimedia Commons). */
@Schema(description = "Crédito que a licença da imagem pede: mostrar como \"Foto: autor, licença\", "
        + "com link para fonteUrl (D-100)")
public record CreditoImagem(
        String autor,
        @Schema(description = "Ex.: CC BY-SA 4.0, CC0, Public domain") String licenca,
        @Schema(description = "Link da licença; null em domínio público") String licencaUrl,
        @Schema(description = "Página do arquivo no Wikimedia Commons") String fonteUrl) {}
