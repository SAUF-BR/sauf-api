package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;
import br.com.sauf_api.questao.Banca;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tela "Meu acompanhamento": janela móvel até hoje comparada com a anterior do mesmo tamanho. */
@Service
public class AcompanhamentoService {

    static final int SIMULADOS_NA_EVOLUCAO = 12;
    // Abaixo disso a média da plataforma diria demais sobre poucos alunos (D-89).
    static final int MINIMO_ALUNOS_MEDIA = 5;
    // Habilidade com menos respostas que isso não basta para apontar foco; usa a área (D-90).
    static final int MINIMO_RESPOSTAS_HABILIDADE = 10;
    static final int MAXIMO_FOCOS = 3;

    private final SimuladoRepository simulados;
    private final SimuladoQuestaoRepository itens;
    private final HabilidadesEnem habilidades;
    private final Clock clock;

    public AcompanhamentoService(
            SimuladoRepository simulados, SimuladoQuestaoRepository itens, HabilidadesEnem habilidades, Clock clock) {
        this.simulados = simulados;
        this.itens = itens;
        this.habilidades = habilidades;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DadosAcompanhamento dados(UUID usuarioId, PeriodoCronometro periodo, Banca vestibular) {
        Collection<Banca> bancas = vestibular == null ? EnumSet.allOf(Banca.class) : EnumSet.of(vestibular);
        LocalDate hoje = JanelaPeriodo.hoje(clock);
        JanelaPeriodo atual = JanelaPeriodo.movel(periodo, hoje);
        JanelaPeriodo anterior = atual.anterior();

        List<ContagemResultado> resultadosAtual =
                itens.resultadosConcluidos(usuarioId, bancas, atual.desde(), atual.ate());
        List<ContagemResultado> resultadosAnterior =
                itens.resultadosConcluidos(usuarioId, bancas, anterior.desde(), anterior.ate());
        Totais totalAtual = Totais.de(resultadosAtual);
        Totais totalAnterior = Totais.de(resultadosAnterior);

        DadosAcompanhamento.Resumo resumo = new DadosAcompanhamento.Resumo(
                new DadosAcompanhamento.Porcentagens(totalAtual.aproveitamento(), totalAnterior.aproveitamento()),
                new DadosAcompanhamento.Contagens(
                        itens.respondidasEntre(usuarioId, bancas, atual.desde(), atual.ate()),
                        itens.respondidasEntre(usuarioId, bancas, anterior.desde(), anterior.ate())),
                new DadosAcompanhamento.Contagens(
                        simulados.concluidosEntre(usuarioId, bancas, atual.desde(), atual.ate()),
                        simulados.concluidosEntre(usuarioId, bancas, anterior.desde(), anterior.ate())),
                new DadosAcompanhamento.Contagens(totalAtual.acertos(), totalAnterior.acertos()));

        List<DadosAcompanhamento.Area> areas = areas(resultadosAtual, resultadosAnterior);

        return new DadosAcompanhamento(
                periodo,
                new DadosAcompanhamento.Intervalo(atual.inicio(), atual.fim()),
                resumo,
                evolucao(usuarioId, bancas, atual),
                new DadosAcompanhamento.Distribuicao(totalAtual.resultados(), totalAnterior.resultados()),
                areas,
                focos(resultadosAtual, areas),
                constancia(usuarioId, bancas, atual, hoje));
    }

    private record Totais(long acertos, long erros, long emBranco) {

        static Totais de(List<ContagemResultado> linhas) {
            long acertos = 0;
            long erros = 0;
            long emBranco = 0;
            for (ContagemResultado l : linhas) {
                if (l.acerto()) {
                    acertos += l.total();
                } else if (l.erro()) {
                    erros += l.total();
                } else {
                    emBranco += l.total();
                }
            }
            return new Totais(acertos, erros, emBranco);
        }

        long questoes() {
            return acertos + erros + emBranco;
        }

        Double aproveitamento() {
            return questoes() == 0 ? null : Porcentagem.de((int) acertos, (int) questoes());
        }

        DadosAcompanhamento.Resultados resultados() {
            return new DadosAcompanhamento.Resultados(acertos, erros, emBranco);
        }
    }

    private DadosAcompanhamento.Evolucao evolucao(UUID usuarioId, Collection<Banca> bancas, JanelaPeriodo janela) {
        List<DadosAcompanhamento.PontoEvolucao> pontos = new ArrayList<>(simulados
                .ultimosConcluidos(
                        usuarioId, bancas, janela.desde(), janela.ate(), PageRequest.of(0, SIMULADOS_NA_EVOLUCAO))
                .stream()
                .map(s -> new DadosAcompanhamento.PontoEvolucao(
                        s.getId(),
                        s.getFinalizadoEm().atZone(JanelaPeriodo.FUSO).toOffsetDateTime(),
                        Porcentagem.de(s.getAcertos(), s.getAcertos() + s.getErros() + s.getEmBranco())))
                .toList());
        pontos.sort(Comparator.comparing(DadosAcompanhamento.PontoEvolucao::finalizadoEm));

        MediaPlataforma media = simulados.mediaPlataforma(bancas, janela.desde(), janela.ate());
        Double mediaPlataforma = media.alunos() < MINIMO_ALUNOS_MEDIA || media.aproveitamento() == null
                ? null
                : Math.round(media.aproveitamento() * 10) / 10.0;
        return new DadosAcompanhamento.Evolucao(mediaPlataforma, pontos);
    }

    private List<DadosAcompanhamento.Area> areas(List<ContagemResultado> atual, List<ContagemResultado> anterior) {
        Map<AreaQuestao, List<ContagemResultado>> porAreaAtual = porArea(atual);
        Map<AreaQuestao, List<ContagemResultado>> porAreaAnterior = porArea(anterior);
        List<DadosAcompanhamento.Area> areas = new ArrayList<>();
        for (AreaQuestao area : AreaQuestao.values()) {
            Totais a = Totais.de(porAreaAtual.getOrDefault(area, List.of()));
            Totais b = Totais.de(porAreaAnterior.getOrDefault(area, List.of()));
            areas.add(new DadosAcompanhamento.Area(
                    area,
                    new DadosAcompanhamento.Porcentagens(a.aproveitamento(), b.aproveitamento()),
                    a.acertos() + a.erros()));
        }
        return areas;
    }

    private Map<AreaQuestao, List<ContagemResultado>> porArea(List<ContagemResultado> linhas) {
        return linhas.stream().collect(Collectors.groupingBy(ContagemResultado::area));
    }

    private record Habilidade(AreaQuestao area, int numero, long erros, long respondidas) {}

    // Habilidades com mais erros (D-90); se não houver respostas suficientes, completa com as áreas mais fracas.
    private List<DadosAcompanhamento.Foco> focos(
            List<ContagemResultado> resultados, List<DadosAcompanhamento.Area> areas) {
        Map<String, Habilidade> porHabilidade = new HashMap<>();
        for (ContagemResultado l : resultados) {
            if (l.habilidade() == null || l.situacao() != SituacaoQuestao.RESPONDIDA) {
                continue;
            }
            porHabilidade.merge(
                    l.area().valor() + l.habilidade(),
                    new Habilidade(l.area(), l.habilidade(), l.erro() ? l.total() : 0, l.total()),
                    (x, y) -> new Habilidade(x.area(), x.numero(), x.erros() + y.erros(), x.respondidas() + y.respondidas()));
        }

        List<DadosAcompanhamento.Foco> focos = new ArrayList<>();
        porHabilidade.values().stream()
                .filter(h -> h.respondidas() >= MINIMO_RESPOSTAS_HABILIDADE && h.erros() > 0)
                .sorted(Comparator.comparingLong(Habilidade::erros)
                        .thenComparingDouble(h -> (double) h.erros() / h.respondidas())
                        .reversed()
                        .thenComparing(Habilidade::area)
                        .thenComparingInt(Habilidade::numero))
                .limit(MAXIMO_FOCOS)
                .forEach(h -> focos.add(new DadosAcompanhamento.Foco(
                        focos.size() + 1,
                        h.area(),
                        HabilidadesEnem.codigo(h.numero()),
                        habilidades.descricao(h.area(), h.numero()),
                        h.erros(),
                        h.respondidas())));

        Set<AreaQuestao> jaNaLista = focos.stream().map(DadosAcompanhamento.Foco::area).collect(Collectors.toSet());
        Map<AreaQuestao, Totais> totaisPorArea = new HashMap<>();
        porArea(resultados).forEach((area, linhas) -> totaisPorArea.put(area, Totais.de(linhas)));
        areas.stream()
                .filter(a -> !jaNaLista.contains(a.area()))
                .filter(a -> totaisPorArea.containsKey(a.area()) && totaisPorArea.get(a.area()).erros() > 0)
                .sorted(Comparator.comparingDouble((DadosAcompanhamento.Area a) -> a.aproveitamento().atual())
                        .thenComparing(DadosAcompanhamento.Area::area))
                .limit(MAXIMO_FOCOS - focos.size())
                .forEach(a -> focos.add(new DadosAcompanhamento.Foco(
                        focos.size() + 1,
                        a.area(),
                        null,
                        "Seu menor aproveitamento: " + formatar(a.aproveitamento().atual())
                                + "% em " + a.area().nome() + " no período.",
                        totaisPorArea.get(a.area()).erros(),
                        a.respondidas())));
        return focos;
    }

    private String formatar(double porcentagem) {
        return porcentagem == Math.rint(porcentagem)
                ? String.valueOf((long) porcentagem)
                : String.valueOf(porcentagem).replace('.', ',');
    }

    private DadosAcompanhamento.Constancia constancia(
            UUID usuarioId, Collection<Banca> bancas, JanelaPeriodo janela, LocalDate hoje) {
        // A ofensiva pode ter começado antes da janela; um ano para trás basta (mesma regra do cronômetro).
        LocalDate desde = janela.inicio().isBefore(hoje.minusDays(CronometroService.DIAS_DO_ANO))
                ? janela.inicio()
                : hoje.minusDays(CronometroService.DIAS_DO_ANO);
        Set<LocalDate> ativos = itens.momentosDeEstudo(usuarioId, bancas, desde.atStartOfDay(JanelaPeriodo.FUSO).toInstant())
                .stream()
                .map(i -> i.atZone(JanelaPeriodo.FUSO).toLocalDate())
                .collect(Collectors.toSet());

        return new DadosAcompanhamento.Constancia(
                ConstanciaCalculadora.diasAtivos(ativos, janela.inicio(), janela.fim()),
                janela.dias(),
                ConstanciaCalculadora.ofensivaAtual(ativos, hoje),
                blocos(ativos, janela));
    }

    // Semana: os 7 dias. Mês: 4 blocos de 7 dias terminando hoje. Ano: os 12 meses do calendário até o atual.
    private List<DadosAcompanhamento.Bloco> blocos(Set<LocalDate> ativos, JanelaPeriodo janela) {
        List<DadosAcompanhamento.Bloco> blocos = new ArrayList<>();
        LocalDate fim = janela.fim();
        switch (janela.periodo()) {
            case SEMANA -> {
                for (LocalDate d = janela.inicio(); !d.isAfter(fim); d = d.plusDays(1)) {
                    blocos.add(new DadosAcompanhamento.Bloco(d, 1, ativos.contains(d) ? 1 : 0));
                }
            }
            case MES -> {
                for (int s = 3; s >= 0; s--) {
                    LocalDate ate = fim.minusDays(7L * s);
                    LocalDate de = ate.minusDays(6);
                    blocos.add(new DadosAcompanhamento.Bloco(de, 7, ConstanciaCalculadora.diasAtivos(ativos, de, ate)));
                }
            }
            case ANO -> {
                LocalDate mes = fim.withDayOfMonth(1).minusMonths(11);
                for (int m = 0; m < 12; m++, mes = mes.plusMonths(1)) {
                    LocalDate ate = mes.plusMonths(1).minusDays(1);
                    LocalDate de = mes.isBefore(janela.inicio()) ? janela.inicio() : mes;
                    if (ate.isAfter(fim)) {
                        ate = fim;
                    }
                    int dias = (int) ChronoUnit.DAYS.between(de, ate) + 1;
                    blocos.add(new DadosAcompanhamento.Bloco(mes, dias, ConstanciaCalculadora.diasAtivos(ativos, de, ate)));
                }
            }
        }
        return blocos;
    }
}
