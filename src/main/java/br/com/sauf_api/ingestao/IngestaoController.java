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
    private final IngestaoIgcService igc;
    private final IngestaoSisuService sisu;

    public IngestaoController(IngestaoService service, IngestaoIgcService igc, IngestaoSisuService sisu) {
        this.service = service;
        this.igc = igc;
        this.sisu = sisu;
    }

    @PostMapping("/censo")
    public RespostaIngestao censo(@Valid @RequestBody LoteCensoRequest lote) {
        return service.importarCenso(lote);
    }

    @PostMapping("/lotes")
    public RespostaIngestao lote(@Valid @RequestBody LoteScraperRequest lote) {
        return service.importarLote(lote);
    }

    @PostMapping("/igc")
    public RespostaIngestao igc(@Valid @RequestBody LoteIgcRequest lote) {
        return igc.importar(lote);
    }

    @PostMapping("/sisu")
    public RespostaSisu sisu(@Valid @RequestBody LoteSisuRequest lote) {
        return sisu.importar(lote);
    }
}
