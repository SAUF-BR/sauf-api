package br.com.sauf_api.simulado;

import br.com.sauf_api.comum.Pagina;
import br.com.sauf_api.comum.UsuarioLogado;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/simulados")
public class SimuladoController {

    private final SimuladoService service;
    private final CronometroService cronometro;
    private final UsuarioLogado usuarioLogado;

    public SimuladoController(SimuladoService service, CronometroService cronometro, UsuarioLogado usuarioLogado) {
        this.service = service;
        this.cronometro = cronometro;
        this.usuarioLogado = usuarioLogado;
    }

    @GetMapping("/opcoes")
    public OpcoesSimulado opcoes() {
        return service.opcoes();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SimuladoDetalhe criar(@Valid @RequestBody CriarSimuladoRequest request) {
        return service.criar(usuarioLogado.id(), request);
    }

    @GetMapping
    public Pagina<SimuladoResumo> listar(
            @RequestParam(required = false) StatusSimulado status,
            @RequestParam(defaultValue = "recente") OrdemSimulados ordem,
            @PageableDefault(size = 12) Pageable pageable) {
        return service.listar(usuarioLogado.id(), status, ordem, pageable);
    }

    @GetMapping("/resumo")
    public ResumoDesempenho resumo() {
        return service.resumo(usuarioLogado.id());
    }

    @GetMapping("/cronometro")
    public DadosCronometro cronometro(@RequestParam(defaultValue = "semana") PeriodoCronometro periodo) {
        return cronometro.dados(usuarioLogado.id(), periodo);
    }

    @GetMapping("/{id}")
    public SimuladoDetalhe detalhar(@PathVariable UUID id) {
        return service.detalhar(usuarioLogado.id(), id);
    }

    @PutMapping("/{id}/questoes/{ordem}")
    public QuestaoSimulado responder(
            @PathVariable UUID id, @PathVariable int ordem, @Valid @RequestBody ResponderQuestaoRequest request) {
        return service.responder(usuarioLogado.id(), id, ordem, request);
    }

    @PostMapping("/{id}/finalizar")
    public RevisaoSimulado finalizar(@PathVariable UUID id) {
        return service.finalizar(usuarioLogado.id(), id);
    }

    @GetMapping("/{id}/revisao")
    public RevisaoSimulado revisao(@PathVariable UUID id) {
        return service.revisao(usuarioLogado.id(), id);
    }

    @PostMapping("/{id}/refazer-erros")
    @ResponseStatus(HttpStatus.CREATED)
    public SimuladoDetalhe refazerErros(@PathVariable UUID id) {
        return service.refazerErros(usuarioLogado.id(), id);
    }
}
