package br.com.sauf_api.comum;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** De onde vêm nome, cidade e preferências de privacidade dos usuários. */
public interface PerfilPublicoProvider {

    Optional<PerfilPublico> perfil(UUID usuarioId);

    /** Só traz quem tem perfil; os ids sem perfil ficam de fora do mapa. */
    Map<UUID, PerfilPublico> perfis(Collection<UUID> usuarioIds);
}
