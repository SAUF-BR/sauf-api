package br.com.sauf_api.sisu;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Leitura das notas de corte do Sisu para as respostas da API (D-50). Sempre usa o ano mais
 * recente que tenha alguma nota entre os códigos e-MEC pedidos.
 */
@Component
public class NotasSisu {

    /** Menor corte de uma oferta, com o turno em que ele aconteceu. */
    public record Corte(BigDecimal nota, String turno, Integer ano) {}

    /** Média simples dos cortes de um curso genérico, cada par (código e-MEC, turno) uma vez. */
    public record Media(BigDecimal nota, Integer ano) {}

    private final NotaCorteSisuRepository notas;

    public NotasSisu(NotaCorteSisuRepository notas) {
        this.notas = notas;
    }

    /** Chave genérica (id da oferta ou do curso) → menor corte entre os códigos dela. */
    public <K> Map<K, Corte> menorCorte(Map<K, Set<Integer>> codigosPorChave) {
        Map<Integer, List<NotaCorteSisu>> porCodigo = buscar(codigosPorChave);
        Map<K, Corte> resultado = new HashMap<>();
        codigosPorChave.forEach((chave, codigos) -> doAnoMaisRecente(codigos, porCodigo).stream()
                .min(Comparator.comparing(NotaCorteSisu::getNota).thenComparing(NotaCorteSisu::getTurno))
                .ifPresent(n -> resultado.put(chave, new Corte(n.getNota(), n.getTurno(), n.getAno()))));
        return resultado;
    }

    public <K> Map<K, Media> media(Map<K, Set<Integer>> codigosPorChave) {
        Map<Integer, List<NotaCorteSisu>> porCodigo = buscar(codigosPorChave);
        Map<K, Media> resultado = new HashMap<>();
        codigosPorChave.forEach((chave, codigos) -> {
            List<NotaCorteSisu> doAno = doAnoMaisRecente(codigos, porCodigo);
            if (!doAno.isEmpty()) {
                BigDecimal soma = doAno.stream().map(NotaCorteSisu::getNota).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal media = soma.divide(BigDecimal.valueOf(doAno.size()), 2, RoundingMode.HALF_UP);
                resultado.put(chave, new Media(media, doAno.getFirst().getAno()));
            }
        });
        return resultado;
    }

    private Map<Integer, List<NotaCorteSisu>> buscar(Map<?, Set<Integer>> codigosPorChave) {
        Set<Integer> todos = codigosPorChave.values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        if (todos.isEmpty()) {
            return Map.of();
        }
        return notas.findByCodigoEmecCursoInAndNotaIsNotNull(todos).stream()
                .collect(Collectors.groupingBy(NotaCorteSisu::getCodigoEmecCurso));
    }

    /** Notas dos códigos no ano mais recente que tem nota; cada (código, turno) aparece uma vez por ano. */
    private static List<NotaCorteSisu> doAnoMaisRecente(
            Collection<Integer> codigos, Map<Integer, List<NotaCorteSisu>> porCodigo) {
        List<NotaCorteSisu> todas = codigos.stream()
                .flatMap(c -> porCodigo.getOrDefault(c, List.of()).stream())
                .toList();
        return todas.stream()
                .map(NotaCorteSisu::getAno)
                .max(Integer::compare)
                .map(ano -> todas.stream().filter(n -> n.getAno().equals(ano)).toList())
                .orElse(List.of());
    }
}
