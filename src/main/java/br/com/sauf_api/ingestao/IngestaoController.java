package br.com.sauf_api.ingestao;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingestao")
public class IngestaoController {

    private final IngestaoService service;

    public IngestaoController(IngestaoService service) {
        this.service = service;
    }

    @PostMapping("/censo")
    public RespostaIngestao censo(@Valid @RequestBody LoteCensoRequest lote) {
        return service.importarCenso(lote);
    }

    @PostMapping("/lotes")
    public RespostaIngestao lote(@Valid @RequestBody LoteScraperRequest lote) {
        return service.importarLote(lote);
    }
}
