package br.com.sauf_api.ingestao;

import br.com.sauf_api.imagem.CreditoImagem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Arquivo (JPG, PNG ou WebP até 2 MB, em base64) e o crédito que a licença pede (D-100). */
public record ImagemRequest(
        @NotBlank(message = "Envie o arquivo da imagem em base64") String conteudoBase64,
        @Schema(example = "Fulano de Tal") @NotBlank(message = "Informe o autor") @Size(max = 300) String autor,
        @Schema(example = "CC BY-SA 4.0") @NotBlank(message = "Informe a licença") @Size(max = 100) String licenca,
        @Size(max = 500) @Pattern(regexp = "https?://\\S+", message = "Link da licença inválido") String licencaUrl,
        @Schema(description = "Página do arquivo no Wikimedia Commons")
        @NotBlank(message = "Informe a página de onde a imagem veio")
        @Size(max = 500)
        @Pattern(regexp = "https?://\\S+", message = "Link da fonte inválido")
        String fonteUrl) {

    CreditoImagem credito() {
        return new CreditoImagem(autor.strip(), licenca.strip(), licencaUrl, fonteUrl);
    }
}
