package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Estatísticas de tempo de estudo. Os dias são os de America/Sao_Paulo (D-84). */
@Service
public class CronometroService {

    static final ZoneId FUSO = JanelaPeriodo.FUSO;
    static final int DIAS_DO_ANO = 365;

    private final SimuladoRepository simulados;
    private final SimuladoQuestaoRepository itens;
    private final Clock clock;

    public CronometroService(SimuladoRepository simulados, SimuladoQuestaoRepository itens, Clock clock) {
        this.simulados = simulados;
        this.itens = itens;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DadosCronometro dados(UUID usuarioId, PeriodoCronometro periodo) {
        LocalDate hoje = JanelaPeriodo.hoje(clock);
        LocalDate inicioDoAno = hoje.minusDays(DIAS_DO_ANO - 1);

        List<Resposta> respostas = itens.atividadeDesde(usuarioId, inicio(inicioDoAno)).stream()
                .filter(SimuladoQuestao::ativa)
                .map(sq -> new Resposta(
                        sq.getAtualizadaEm().atZone(FUSO).toLocalDate(),
                        sq.getQuestao().getArea(),
                        sq.getTempoSegundos()))
                .toList();

        Map<LocalDate, Integer> segundosPorDia = new HashMap<>();
        respostas.forEach(r -> segundosPorDia.merge(r.dia(), r.segundos(), Integer::sum));

        JanelaPeriodo janelaAtual = JanelaPeriodo.movel(periodo, hoje);
        Janela atual = janela(respostas, janelaAtual);
        // Só há dados de um ano para trás, então o ano não tem anterior.
        Janela anterior = periodo == PeriodoCronometro.ANO ? null : janela(respostas, janelaAtual.anterior());

        return new DadosCronometro(
                new DadosCronometro.Comparativo(atual.total(), anterior == null ? null : anterior.total()),
                new DadosCronometro.Comparativo(atual.medio(), anterior == null ? null : anterior.medio()),
                new DadosCronometro.Comparativo(atual.dias().size(), anterior == null ? null : anterior.dias().size()),
                ofensiva(segundosPorDia.keySet(), hoje, inicioDoAno),
                dias(segundosPorDia, hoje, inicioDoAno),
                porConfiguracao(usuarioId, janelaAtual.inicio()),
                semanas(segundosPorDia, hoje),
                ritmoPorArea(respostas, janelaAtual));
    }

    private record Resposta(LocalDate dia, AreaQuestao area, int segundos) {}

    private record Janela(int total, int medio, Set<LocalDate> dias) {}

    private Janela janela(List<Resposta> respostas, JanelaPeriodo periodo) {
        int total = 0;
        int comTempo = 0;
        Set<LocalDate> dias = new HashSet<>();
        for (Resposta r : respostas) {
            if (!periodo.contem(r.dia())) {
                continue;
            }
            total += r.segundos();
            comTempo += r.segundos() > 0 ? 1 : 0;
            dias.add(r.dia());
        }
        return new Janela(total, media(total, comTempo), dias);
    }

    private int media(int total, int quantidade) {
        return quantidade == 0 ? 0 : (int) Math.round((double) total / quantidade);
    }

    private Instant inicio(LocalDate dia) {
        return dia.atStartOfDay(FUSO).toInstant();
    }

    private DadosCronometro.Ofensiva ofensiva(Set<LocalDate> ativos, LocalDate hoje, LocalDate inicio) {
        ConstanciaCalculadora.Ofensiva ofensiva = ConstanciaCalculadora.ofensiva(ativos, hoje, inicio);
        return new DadosCronometro.Ofensiva(ofensiva.atual(), ofensiva.recorde());
    }

    private List<DadosCronometro.Dia> dias(Map<LocalDate, Integer> segundosPorDia, LocalDate hoje, LocalDate inicio) {
        List<DadosCronometro.Dia> dias = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(hoje); d = d.plusDays(1)) {
            dias.add(new DadosCronometro.Dia(d, segundosPorDia.getOrDefault(d, 0)));
        }
        return dias;
    }

    // S4 são os últimos 7 dias, S3 os 7 anteriores, e assim por diante.
    private List<DadosCronometro.Semana> semanas(Map<LocalDate, Integer> segundosPorDia, LocalDate hoje) {
        List<DadosCronometro.Semana> semanas = new ArrayList<>();
        for (int s = 1; s <= 4; s++) {
            LocalDate fim = hoje.minusDays(7L * (4 - s));
            int soma = 0;
            for (int i = 0; i < 7; i++) {
                soma += segundosPorDia.getOrDefault(fim.minusDays(i), 0);
            }
            semanas.add(new DadosCronometro.Semana("S" + s, soma));
        }
        return semanas;
    }

    private List<DadosCronometro.Configuracao> porConfiguracao(UUID usuarioId, LocalDate desde) {
        List<Simulado> concluidos = simulados.findByUsuarioIdAndStatusAndFinalizadoEmGreaterThanEqual(
                usuarioId, StatusSimulado.CONCLUIDO, inicio(desde));
        List<DadosCronometro.Configuracao> resultado = new ArrayList<>();
        for (TamanhoSimulado tamanho : TamanhoSimulado.values()) {
            List<Simulado> doTamanho =
                    concluidos.stream().filter(s -> s.getTamanho() == tamanho).toList();
            if (doTamanho.isEmpty()) {
                continue;
            }
            int total = doTamanho.stream()
                    .flatMap(s -> s.questoesEmOrdem().stream())
                    .mapToInt(SimuladoQuestao::getTempoSegundos)
                    .sum();
            int porQuestao = media(total, doTamanho.size() * tamanho.questoes());
            String observacao = porQuestao > SimuladoService.SEGUNDOS_POR_QUESTAO_PADRAO
                    ? "Acima do ritmo sugerido de 2m30 por questão"
                    : null;
            resultado.add(new DadosCronometro.Configuracao(
                    tamanho,
                    tamanho.questoes(),
                    doTamanho.size(),
                    media(total, doTamanho.size()),
                    total,
                    porQuestao,
                    observacao));
        }
        return resultado;
    }

    private List<DadosCronometro.Ritmo> ritmoPorArea(List<Resposta> respostas, JanelaPeriodo periodo) {
        Map<AreaQuestao, int[]> porArea = new EnumMap<>(AreaQuestao.class);
        for (Resposta r : respostas) {
            if (!periodo.contem(r.dia())) {
                continue;
            }
            int[] c = porArea.computeIfAbsent(r.area(), a -> new int[2]);
            c[0] += r.segundos();
            c[1] += r.segundos() > 0 ? 1 : 0;
        }
        return porArea.entrySet().stream()
                .map(e -> new DadosCronometro.Ritmo(e.getKey().valor(), media(e.getValue()[0], e.getValue()[1])))
                .toList();
    }
}
