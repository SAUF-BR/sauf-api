package br.com.sauf_api.ingestao;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingestao")
public class IngestaoController {

    private final IngestaoService service;
    private final IngestaoIgcService igc;
    private final IngestaoSisuService sisu;
    private final IngestaoQuestoesService questoes;
    private final IngestaoDadosManuaisService manuais;

    public IngestaoController(
            IngestaoService service,
            IngestaoIgcService igc,
            IngestaoSisuService sisu,
            IngestaoQuestoesService questoes,
            IngestaoDadosManuaisService manuais) {
        this.service = service;
        this.igc = igc;
        this.sisu = sisu;
        this.questoes = questoes;
        this.manuais = manuais;
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

    @PostMapping("/questoes")
    public RespostaIngestao questoes(@Valid @RequestBody LoteQuestoesRequest lote) {
        return questoes.importar(lote);
    }

    // Dados manuais (sauf-scraper/dados, D-98): PUT porque cada envio substitui o bloco inteiro
    @PutMapping("/universidades/{codigoEmec}")
    public RespostaIngestao universidade(
            @PathVariable Integer codigoEmec, @Valid @RequestBody DadosManuaisUniversidadeRequest dados) {
        return manuais.universidade(codigoEmec, dados);
    }

    @PutMapping("/areas/{codigoCine}/imagem")
    public RespostaIngestao imagemDaArea(
            @PathVariable String codigoCine,
            @Valid @RequestBody ImagemRequest imagem) {
        return manuais.imagemDaArea(codigoCine, imagem);
    }
}
