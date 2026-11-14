package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Texto das 30 habilidades de cada área da Matriz de Referência do ENEM (Inep), em
 * {@code enem/habilidades.json}. A questão guarda só o número (CO_HABILIDADE dos microdados).
 */
@Component
public class HabilidadesEnem {

    private final Map<String, Map<String, String>> porArea;

    public HabilidadesEnem(JsonMapper json) {
        try (InputStream entrada = new ClassPathResource("enem/habilidades.json").getInputStream()) {
            porArea = json.readValue(entrada, new TypeReference<>() {});
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String codigo(int habilidade) {
        return "H" + habilidade;
    }

    public String descricao(AreaQuestao area, int habilidade) {
        return porArea.getOrDefault(area.valor(), Map.of()).get(codigo(habilidade));
    }
}
