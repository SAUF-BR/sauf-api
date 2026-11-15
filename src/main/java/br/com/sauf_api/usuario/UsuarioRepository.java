package br.com.sauf_api.usuario;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    // O e-mail é gravado em minúsculas: quem chama passa Usuario.normalizarEmail(...)
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
