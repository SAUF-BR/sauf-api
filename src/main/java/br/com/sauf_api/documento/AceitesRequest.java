package br.com.sauf_api.documento;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Corpo do POST /api/v1/me/aceites (D-93): os documentos aceitos na tela de atualização. */
public record AceitesRequest(
        @NotEmpty(message = "Informe os documentos aceitos") List<@Valid AceiteRequest> aceites) {}
