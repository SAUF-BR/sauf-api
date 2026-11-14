package br.com.sauf_api.simulado;

import br.com.sauf_api.assinatura.AcessoRecursos;
import br.com.sauf_api.assinatura.RecursoPlano;
import br.com.sauf_api.comum.PerfilPublico;
import br.com.sauf_api.comum.PerfilPublicoProvider;
import br.com.sauf_api.questao.Banca;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ranking por tempo de estudo, na janela do calendário (semana de segunda a domingo, mês, ano). */
@Service
public class RankingService {

    static final int TAMANHO_PODIO = 3;
    // Com menos alunos na região, o percentil diria demais sobre poucos (D-92).
    static final int MINIMO_PARTICIPANTES_PERCENTIL = 20;
    static final String BRASIL = "Brasil";

    private final SimuladoRepository simulados;
    private final SimuladoQuestaoRepository itens;
    private final PerfilPublicoProvider perfis;
    private final AcessoRecursos acesso;
    private final Clock clock;

    public RankingService(
            SimuladoRepository simulados,
            SimuladoQuestaoRepository itens,
            PerfilPublicoProvider perfis,
            AcessoRecursos acesso,
            Clock clock) {
        this.simulados = simulados;
        this.itens = itens;
        this.perfis = perfis;
        this.acesso = acesso;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DadosRanking dados(UUID usuarioId, PeriodoCronometro periodo, Banca vestibular) {
        acesso.exigir(usuarioId, RecursoPlano.RANKING);
        Collection<String> bancas = (vestibular == null ? EnumSet.allOf(Banca.class) : EnumSet.of(vestibular))
                .stream()
                .map(Banca::name)
                .toList();
        LocalDate hoje = JanelaPeriodo.hoje(clock);
        JanelaPeriodo atual = JanelaPeriodo.calendario(periodo, hoje);
        JanelaPeriodo anterior = atual.anterior();

        List<LinhaRanking> linhas = ranking(bancas, atual);
        Optional<LinhaRanking> minha = linhaDe(linhas, usuarioId);
        Optional<LinhaRanking> minhaAnterior = linhaDe(ranking(bancas, anterior), usuarioId);

        Map<UUID, PerfilPublico> perfisDosParticipantes =
                perfis.perfis(linhas.stream().map(LinhaRanking::usuario).toList());

        DadosRanking.Colocacao colocacao = minha
                .map(l -> new DadosRanking.Colocacao(
                        l.getPosicao().intValue(), minhaAnterior.map(a -> a.getPosicao().intValue()).orElse(null)))
                .orElse(null);

        return new DadosRanking(
                periodo,
                new DadosRanking.Intervalo(atual.inicio(), atual.fim()),
                linhas.size(),
                podio(linhas, perfisDosParticipantes),
                colocacao,
                percentil(usuarioId, minha, linhas, perfisDosParticipantes),
                new DadosRanking.Estatisticas(
                        minha.map(l -> l.getSegundos().longValue()).orElse(0L),
                        minha.map(l -> l.getSimulados().longValue()).orElse(0L),
                        ofensiva(usuarioId, bancas, hoje)));
    }

    private List<LinhaRanking> ranking(Collection<String> bancas, JanelaPeriodo janela) {
        return simulados.ranking(bancas, janela.desde(), janela.ate(), SimuladoService.SEGUNDOS_POR_QUESTAO_PADRAO);
    }

    private Optional<LinhaRanking> linhaDe(List<LinhaRanking> linhas, UUID usuarioId) {
        return linhas.stream().filter(l -> l.usuario().equals(usuarioId)).findFirst();
    }

    // Só quem aceitou aparecer e é maior de idade; os demais deixam a posição vazia (D-88).
    private List<DadosRanking.Podio> podio(List<LinhaRanking> linhas, Map<UUID, PerfilPublico> perfisPorId) {
        List<DadosRanking.Podio> podio = new ArrayList<>();
        for (LinhaRanking linha : linhas) {
            if (linha.getPosicao().intValue() > TAMANHO_PODIO || podio.size() == TAMANHO_PODIO) {
                break;
            }
            PerfilPublico perfil = perfisPorId.get(linha.usuario());
            if (perfil == null || !perfil.podeAparecerNoPodio()) {
                continue;
            }
            podio.add(new DadosRanking.Podio(
                    linha.getPosicao().intValue(),
                    perfil.nomeAbreviado(),
                    perfil.cidade(),
                    perfil.uf(),
                    perfil.fotoUrl(),
                    linha.getSegundos().longValue(),
                    linha.getSimulados().longValue()));
        }
        return podio;
    }

    // Percentual a partir do topo entre os participantes da UF do aluno (ou do Brasil), arredondado para cima.
    private DadosRanking.Percentil percentil(
            UUID usuarioId,
            Optional<LinhaRanking> minha,
            List<LinhaRanking> linhas,
            Map<UUID, PerfilPublico> perfisPorId) {
        if (minha.isEmpty()) {
            return null;
        }
        String uf = perfis.perfil(usuarioId).map(PerfilPublico::uf).filter(u -> !u.isBlank()).orElse(null);
        List<LinhaRanking> daRegiao = uf == null
                ? linhas
                : linhas.stream()
                        .filter(l -> l.usuario().equals(usuarioId)
                                || Optional.ofNullable(perfisPorId.get(l.usuario()))
                                        .map(p -> uf.equals(p.uf()))
                                        .orElse(false))
                        .toList();
        if (daRegiao.size() < MINIMO_PARTICIPANTES_PERCENTIL) {
            return null;
        }
        int minhaPosicao = minha.get().getPosicao().intValue();
        long aFrente = daRegiao.stream().filter(l -> l.getPosicao().intValue() < minhaPosicao).count();
        int percentual = (int) Math.ceil((aFrente + 1) * 100.0 / daRegiao.size());
        return new DadosRanking.Percentil(percentual, uf == null ? BRASIL : uf);
    }

    private int ofensiva(UUID usuarioId, Collection<String> bancas, LocalDate hoje) {
        Set<Banca> bancasEnum = bancas.stream().map(Banca::valueOf).collect(Collectors.toSet());
        Set<LocalDate> ativos = itens
                .momentosDeEstudo(
                        usuarioId,
                        bancasEnum,
                        hoje.minusDays(CronometroService.DIAS_DO_ANO).atStartOfDay(JanelaPeriodo.FUSO).toInstant())
                .stream()
                .map(i -> i.atZone(JanelaPeriodo.FUSO).toLocalDate())
                .collect(Collectors.toSet());
        return ConstanciaCalculadora.ofensivaAtual(ativos, hoje);
    }
}
