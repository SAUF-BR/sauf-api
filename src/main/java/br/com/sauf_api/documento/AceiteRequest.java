package br.com.sauf_api.documento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Um documento aceito, na versão que a tela mostrou (D-93). Ex.: {"tipo": "termos-uso", "versao": "1.0.0"}. */
public record AceiteRequest(
        @NotNull(message = "Informe o documento") TipoDocumento tipo,
        @NotBlank(message = "Informe a versão") String versao) {}
