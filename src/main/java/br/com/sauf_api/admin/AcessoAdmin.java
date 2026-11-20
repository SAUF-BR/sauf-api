package br.com.sauf_api.admin;

import br.com.sauf_api.usuario.Papel;
import br.com.sauf_api.usuario.Usuario;
import br.com.sauf_api.usuario.UsuarioRepository;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quem é administrador (D-107): a conta com papel ADMIN. Quem está na SAUF_ADMIN_EMAILS é
 * promovido na primeira vez que usa uma rota de admin; tirar o e-mail da lista não rebaixa
 * (para isso, trocar o papel no banco).
 */
@Component
public class AcessoAdmin {

    private static final Logger log = LoggerFactory.getLogger(AcessoAdmin.class);

    private final Set<String> emails;
    private final UsuarioRepository usuarios;

    public AcessoAdmin(@Value("${sauf.admin.emails:}") String emails, UsuarioRepository usuarios) {
        this.emails = Arrays.stream(emails.split(","))
                .map(String::trim)
                .filter(email -> !email.isEmpty())
                .map(Usuario::normalizarEmail)
                .collect(Collectors.toUnmodifiableSet());
        this.usuarios = usuarios;
    }

    @Transactional
    public boolean ehAdmin(UUID usuarioId) {
        return usuarios.findById(usuarioId)
                .map(usuario -> {
                    if (usuario.getPapel() != Papel.ADMIN && emails.contains(usuario.getEmail())) {
                        usuario.promoverAAdmin();
                        log.info("usuário promovido a ADMIN pela SAUF_ADMIN_EMAILS: id={}", usuario.getId());
                    }
                    return usuario.getPapel() == Papel.ADMIN;
                })
                .orElse(false);
    }
}
