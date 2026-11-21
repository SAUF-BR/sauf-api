package br.com.sauf_api.simulado;

import br.com.sauf_api.assinatura.AcessoRecursos;
import br.com.sauf_api.assinatura.RecursoPlano;
import br.com.sauf_api.comum.Pagina;
import br.com.sauf_api.comum.UsuarioLogado;
import br.com.sauf_api.questao.Banca;
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
    private final AcompanhamentoService acompanhamento;
    private final RankingService ranking;
    private final UsuarioLogado usuarioLogado;
    private final AcessoRecursos acesso;

    public SimuladoController(
            SimuladoService service,
            CronometroService cronometro,
            AcompanhamentoService acompanhamento,
            RankingService ranking,
            UsuarioLogado usuarioLogado,
            AcessoRecursos acesso) {
        this.service = service;
        this.cronometro = cronometro;
        this.acompanhamento = acompanhamento;
        this.ranking = ranking;
        this.usuarioLogado = usuarioLogado;
        this.acesso = acesso;
    }

    @GetMapping("/opcoes")
    public OpcoesSimulado opcoes() {
        assinante(RecursoPlano.SIMULADOS_ENEM);
        return service.opcoes();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SimuladoDetalhe criar(@Valid @RequestBody CriarSimuladoRequest request) {
        return service.criar(assinante(RecursoPlano.SIMULADOS_ENEM), request);
    }

    @GetMapping
    public Pagina<SimuladoResumo> listar(
            @RequestParam(required = false) StatusSimulado status,
            @RequestParam(defaultValue = "recente") OrdemSimulados ordem,
            @PageableDefault(size = 12) Pageable pageable) {
        return service.listar(assinante(RecursoPlano.SIMULADOS_ENEM), status, ordem, pageable);
    }

    @GetMapping("/resumo")
    public ResumoDesempenho resumo() {
        return service.resumo(assinante(RecursoPlano.SIMULADOS_ENEM));
    }

    @GetMapping("/cronometro")
    public DadosCronometro cronometro(@RequestParam(defaultValue = "semana") PeriodoCronometro periodo) {
        return cronometro.dados(assinante(RecursoPlano.TIMER), periodo);
    }

    @GetMapping("/acompanhamento")
    public DadosAcompanhamento acompanhamento(
            @RequestParam(defaultValue = "mes") PeriodoCronometro periodo,
            @RequestParam(required = false) Banca vestibular) {
        return acompanhamento.dados(assinante(RecursoPlano.DASHBOARD), periodo, vestibular);
    }

    @GetMapping("/ranking")
    public DadosRanking ranking(
            @RequestParam(defaultValue = "mes") PeriodoCronometro periodo,
            @RequestParam(required = false) Banca vestibular) {
        return ranking.dados(assinante(RecursoPlano.RANKING), periodo, vestibular);
    }

    @GetMapping("/{id}")
    public SimuladoDetalhe detalhar(@PathVariable UUID id) {
        return service.detalhar(assinante(RecursoPlano.SIMULADOS_ENEM), id);
    }

    @PutMapping("/{id}/questoes/{ordem}")
    public QuestaoSimulado responder(
            @PathVariable UUID id, @PathVariable int ordem, @Valid @RequestBody ResponderQuestaoRequest request) {
        return service.responder(assinante(RecursoPlano.SIMULADOS_ENEM), id, ordem, request);
    }

    @PostMapping("/{id}/finalizar")
    public RevisaoSimulado finalizar(@PathVariable UUID id) {
        return service.finalizar(assinante(RecursoPlano.SIMULADOS_ENEM), id);
    }

    @GetMapping("/{id}/revisao")
    public RevisaoSimulado revisao(@PathVariable UUID id) {
        return service.revisao(assinante(RecursoPlano.SIMULADOS_ENEM), id);
    }

    @PostMapping("/{id}/refazer-erros")
    @ResponseStatus(HttpStatus.CREATED)
    public SimuladoDetalhe refazerErros(@PathVariable UUID id) {
        return service.refazerErros(assinante(RecursoPlano.SIMULADOS_ENEM), id);
    }

    // Simulados são do Basic; cronômetro, acompanhamento e ranking, do Plus (D-98, D-109)
    private UUID assinante(RecursoPlano recurso) {
        UUID usuarioId = usuarioLogado.id();
        acesso.exigir(usuarioId, recurso);
        return usuarioId;
    }
}
