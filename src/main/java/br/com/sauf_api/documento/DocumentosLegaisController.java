package br.com.sauf_api.documento;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Textos vigentes dos Termos de Uso e da Política de Privacidade (D-93). Público. */
@RestController
@RequestMapping("/api/v1/documentos-legais")
public class DocumentosLegaisController {

    private final DocumentosLegaisService service;

    public DocumentosLegaisController(DocumentosLegaisService service) {
        this.service = service;
    }

    @GetMapping
    public List<DocumentoResposta> vigentes() {
        return service.vigentes();
    }
}
