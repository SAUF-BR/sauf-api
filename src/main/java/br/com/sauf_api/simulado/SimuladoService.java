package br.com.sauf_api.simulado;

import br.com.sauf_api.comum.AcaoNaoPermitidaException;
import br.com.sauf_api.comum.DadosInvalidosException;
import br.com.sauf_api.comum.Pagina;
import br.com.sauf_api.comum.RecursoNaoEncontradoException;
import br.com.sauf_api.questao.Alternativa;
import br.com.sauf_api.questao.AreaQuestao;
import br.com.sauf_api.questao.Banca;
import br.com.sauf_api.questao.Lingua;
import br.com.sauf_api.questao.Questao;
import br.com.sauf_api.questao.QuestaoRepository;
import br.com.sauf_api.questao.StatusQuestao;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimuladoService {

    public static final int SEGUNDOS_POR_QUESTAO_PADRAO = 150;
    public static final List<Integer> TEMPOS_POR_QUESTAO = List.of(90, 120, 150, 180, 240);
    static final int SEGUNDOS_POR_QUESTAO_MINIMO = 60;
    static final int SEGUNDOS_POR_QUESTAO_MAXIMO = 600;
    static final int SEGUNDOS_POR_QUESTAO_PASSO = 30;

    private final SimuladoRepository simulados;
    private final SimuladoQuestaoRepository itens;
    private final QuestaoRepository questoes;
    private final Clock clock;

    public SimuladoService(
            SimuladoRepository simulados, SimuladoQuestaoRepository itens, QuestaoRepository questoes, Clock clock) {
        this.simulados = simulados;
        this.itens = itens;
        this.questoes = questoes;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OpcoesSimulado opcoes() {
        return new OpcoesSimulado(
                Arrays.stream(TamanhoSimulado.values())
                        .map(t -> new OpcoesSimulado.TamanhoOpcao(t, t.questoes()))
                        .toList(),
                SEGUNDOS_POR_QUESTAO_PADRAO,
                TEMPOS_POR_QUESTAO,
                List.of(new OpcoesSimulado.BancaOpcao(Banca.ENEM, "ENEM")),
                Arrays.stream(AreaQuestao.values())
                        .map(a -> new OpcoesSimulado.AreaOpcao(
                                a, a.nome(), questoes.contarSorteaveis(Banca.ENEM, a, StatusQuestao.DISPONIVEL, Lingua.ESPANHOL)))
                        .toList());
    }

    @Transactional
    public SimuladoDetalhe criar(UUID usuarioId, CriarSimuladoRequest request) {
        if (request.banca() != Banca.ENEM) {
            throw new DadosInvalidosException("banca", "Por enquanto só o ENEM está disponível");
        }
        boolean cronometrado = request.cronometrado();
        Integer segundosPorQuestao = cronometrado ? validarTempo(request.segundosPorQuestao()) : null;
        Set<AreaQuestao> areas = EnumSet.copyOf(request.areas());
        List<Questao> sorteadas = sortear(usuarioId, request.banca(), areas, request.tamanho().questoes());

        Simulado simulado = new Simulado(
                usuarioId,
                request.banca(),
                request.tamanho(),
                areas,
                cronometrado,
                segundosPorQuestao,
                null,
                clock.instant());
        return abrir(simulado, sorteadas);
    }

    @Transactional(readOnly = true)
    public SimuladoDetalhe detalhar(UUID usuarioId, UUID id) {
        Simulado simulado = buscar(usuarioId, id);
        return SimuladoDetalhe.de(simulado, titulo(simulado));
    }

    @Transactional
    public QuestaoSimulado responder(UUID usuarioId, UUID id, int ordem, ResponderQuestaoRequest request) {
        Simulado simulado = buscar(usuarioId, id);
        if (simulado.concluido()) {
            throw new AcaoNaoPermitidaException("O simulado já foi finalizado");
        }
        SimuladoQuestao item = simulado.questao(ordem)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Questão " + ordem + " não encontrada no simulado"));
        validarAlternativa(item, request);

        // O app manda o total acumulado; reenvios fora de ordem não podem diminuir o tempo.
        // O tempo por questão é só referência (D-95): passar dele não muda nada na resposta.
        int tempo = Math.max(item.getTempoSegundos(), request.tempoSegundos());
        item.registrar(request.situacao(), request.alternativa(), tempo, clock.instant());
        return QuestaoSimulado.de(item);
    }

    @Transactional
    public RevisaoSimulado finalizar(UUID usuarioId, UUID id) {
        Simulado simulado = buscar(usuarioId, id);
        if (!simulado.concluido()) {
            List<SimuladoQuestao> lista = simulado.questoesEmOrdem();
            lista.forEach(SimuladoQuestao::encerrar);
            simulado.concluir(
                    clock.instant(),
                    contar(lista, ResultadoQuestao.ACERTO),
                    contar(lista, ResultadoQuestao.ERRO),
                    contar(lista, ResultadoQuestao.EM_BRANCO));
        }
        return RevisaoSimulado.de(simulado, titulo(simulado));
    }

    @Transactional(readOnly = true)
    public RevisaoSimulado revisao(UUID usuarioId, UUID id) {
        Simulado simulado = buscar(usuarioId, id);
        if (!simulado.concluido()) {
            throw new AcaoNaoPermitidaException("O simulado ainda está em andamento");
        }
        return RevisaoSimulado.de(simulado, titulo(simulado));
    }

    @Transactional
    public SimuladoDetalhe refazerErros(UUID usuarioId, UUID id) {
        Simulado origem = buscar(usuarioId, id);
        if (!origem.concluido()) {
            throw new AcaoNaoPermitidaException("Finalize o simulado antes de refazer os erros");
        }
        // Erradas e em branco (escolhidas ou sem resposta), na ordem original (D-96)
        List<SimuladoQuestao> erradas = origem.questoesEmOrdem().stream()
                .filter(q -> q.resultado() != ResultadoQuestao.ACERTO)
                .toList();
        if (erradas.isEmpty()) {
            throw new AcaoNaoPermitidaException("Este simulado não teve erros nem questões em branco para refazer");
        }
        Set<AreaQuestao> areas = EnumSet.noneOf(AreaQuestao.class);
        erradas.forEach(q -> areas.add(q.getQuestao().getArea()));

        Simulado novo = new Simulado(
                usuarioId,
                origem.getBanca(),
                null,
                areas,
                origem.isCronometrado(),
                origem.getSegundosPorQuestao(),
                origem.getId(),
                clock.instant());
        return abrir(novo, erradas.stream().map(SimuladoQuestao::getQuestao).toList());
    }

    @Transactional(readOnly = true)
    public Pagina<SimuladoResumo> listar(
            UUID usuarioId, StatusSimulado status, OrdemSimulados ordem, Pageable pageable) {
        Set<StatusSimulado> filtro = status == null ? EnumSet.allOf(StatusSimulado.class) : EnumSet.of(status);
        Pageable pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Simulado> resultado =
                switch (ordem) {
                    case RECENTE -> simulados.recentes(usuarioId, filtro, pagina);
                    case ANTIGO -> simulados.antigos(usuarioId, filtro, pagina);
                    case MAIOR_NOTA -> simulados.maiorNota(usuarioId, filtro, pagina);
                    case MENOR_NOTA -> simulados.menorNota(usuarioId, filtro, pagina);
                };
        return Pagina.de(resultado, s -> SimuladoResumo.de(s, titulo(s)));
    }

    @Transactional(readOnly = true)
    public ResumoDesempenho resumo(UUID usuarioId) {
        List<Simulado> concluidos = simulados.findByUsuarioIdAndStatus(usuarioId, StatusSimulado.CONCLUIDO);
        long respondidas = itens.contar(usuarioId, SituacaoQuestao.RESPONDIDA);
        List<SimuladoQuestao> todas =
                concluidos.stream().flatMap(s -> s.questoesEmOrdem().stream()).toList();
        if (todas.isEmpty()) {
            return new ResumoDesempenho(concluidos.size(), respondidas, null, null);
        }
        int acertos = (int) todas.stream().filter(q -> q.resultado() == ResultadoQuestao.ACERTO).count();

        ResumoDesempenho.MelhorArea melhor = null;
        for (DesempenhoArea area : DesempenhoArea.de(todas)) {
            double porcentagem = Porcentagem.de(area.acertos(), area.total());
            if (melhor == null || porcentagem > melhor.porcentagem()) {
                melhor = new ResumoDesempenho.MelhorArea(area.area(), porcentagem);
            }
        }
        return new ResumoDesempenho(concluidos.size(), respondidas, Porcentagem.de(acertos, todas.size()), melhor);
    }

    private SimuladoDetalhe abrir(Simulado simulado, List<Questao> questoesDoSimulado) {
        simulados.save(simulado);
        questoesDoSimulado.forEach(simulado::adicionarQuestao);
        simulados.flush();
        return SimuladoDetalhe.de(simulado, titulo(simulado));
    }

    // Divide igual entre as áreas (o resto vai para as primeiras) e prefere o que o usuário nunca fez.
    private List<Questao> sortear(UUID usuarioId, Banca banca, Set<AreaQuestao> escolhidas, int total) {
        List<AreaQuestao> areas = escolhidas.stream().sorted().toList();
        Set<UUID> jaFeitas = new HashSet<>(itens.idsDoUsuario(usuarioId));
        List<Questao> sorteadas = new ArrayList<>();
        for (int i = 0; i < areas.size(); i++) {
            AreaQuestao area = areas.get(i);
            int quantidade = total / areas.size() + (i < total % areas.size() ? 1 : 0);

            List<UUID> inedidas = new ArrayList<>();
            List<UUID> repetidas = new ArrayList<>();
            for (UUID id : questoes.idsSorteaveis(banca, area, StatusQuestao.DISPONIVEL, Lingua.ESPANHOL)) {
                (jaFeitas.contains(id) ? repetidas : inedidas).add(id);
            }
            if (inedidas.size() + repetidas.size() < quantidade) {
                throw new AcaoNaoPermitidaException(
                        "Não há questões suficientes de " + area.nome() + " para este simulado");
            }
            Collections.shuffle(inedidas);
            Collections.shuffle(repetidas);
            List<UUID> escolhidos = new ArrayList<>(inedidas);
            escolhidos.addAll(repetidas);
            escolhidos = escolhidos.subList(0, quantidade);

            Map<UUID, Questao> porId = new LinkedHashMap<>();
            questoes.findAllById(escolhidos).forEach(q -> porId.put(q.getId(), q));
            escolhidos.forEach(id -> sorteadas.add(porId.get(id)));
        }
        return sorteadas;
    }

    private int validarTempo(Integer segundos) {
        if (segundos == null) {
            throw new DadosInvalidosException("segundosPorQuestao", "Escolha o tempo por questão");
        }
        if (segundos < SEGUNDOS_POR_QUESTAO_MINIMO
                || segundos > SEGUNDOS_POR_QUESTAO_MAXIMO
                || segundos % SEGUNDOS_POR_QUESTAO_PASSO != 0) {
            throw new DadosInvalidosException(
                    "segundosPorQuestao", "O tempo por questão vai de 1 a 10 minutos, de 30 em 30 segundos");
        }
        return segundos;
    }

    private void validarAlternativa(SimuladoQuestao item, ResponderQuestaoRequest request) {
        if (request.situacao() != SituacaoQuestao.RESPONDIDA) {
            return;
        }
        if (request.alternativa() == null || request.alternativa().isBlank()) {
            throw new DadosInvalidosException("alternativa", "Escolha uma alternativa");
        }
        boolean existe = item.getQuestao().getAlternativas().stream()
                .map(Alternativa::getRotulo)
                .anyMatch(request.alternativa()::equals);
        if (!existe) {
            throw new DadosInvalidosException("alternativa", "A questão não tem a alternativa " + request.alternativa());
        }
    }

    private int contar(List<SimuladoQuestao> lista, ResultadoQuestao resultado) {
        return (int) lista.stream().filter(q -> q.resultado() == resultado).count();
    }

    private Simulado buscar(UUID usuarioId, UUID id) {
        return simulados
                .findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Simulado", id));
    }

    private String titulo(Simulado simulado) {
        if (simulado.getOrigemId() != null) {
            String origem = simulados
                    .findById(simulado.getOrigemId())
                    .map(this::titulo)
                    .orElse("Simulado ENEM");
            return "Refazer erros e em branco · " + origem;
        }
        List<AreaQuestao> areas = simulado.areasEmOrdem();
        if (areas.size() == AreaQuestao.values().length) {
            return "Simulado ENEM · todas as áreas";
        }
        List<String> nomes = areas.stream().map(AreaQuestao::nome).toList();
        String lista = nomes.size() == 1
                ? nomes.get(0)
                : String.join(", ", nomes.subList(0, nomes.size() - 1)) + " e " + nomes.get(nomes.size() - 1);
        return "Simulado ENEM · " + lista;
    }
}
