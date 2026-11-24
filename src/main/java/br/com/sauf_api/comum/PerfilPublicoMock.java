package br.com.sauf_api.comum;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Perfis fictícios em memória até o login (D-55, TR-336), como o UsuarioLogado. Vêm de
 * {@code mock/perfis-publicos.json}: o usuário provisório e os alunos inventados do
 * {@code scripts/seed-ranking-local.sql}. Nomes inventados, sem dados reais (LGPD).
 */
@Component
public class PerfilPublicoMock implements PerfilPublicoProvider {

    private final Map<UUID, PerfilPublico> porId;

    public PerfilPublicoMock(JsonMapper json) {
        try (InputStream entrada = new ClassPathResource("mock/perfis-publicos.json").getInputStream()) {
            List<PerfilPublico> perfis = json.readValue(entrada, new TypeReference<>() {});
            porId = perfis.stream().collect(Collectors.toMap(PerfilPublico::usuarioId, Function.identity()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<PerfilPublico> perfil(UUID usuarioId) {
        return Optional.ofNullable(porId.get(usuarioId));
    }

    @Override
    public Map<UUID, PerfilPublico> perfis(Collection<UUID> usuarioIds) {
        return usuarioIds.stream()
                .map(porId::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(PerfilPublico::usuarioId, Function.identity()));
    }
}
