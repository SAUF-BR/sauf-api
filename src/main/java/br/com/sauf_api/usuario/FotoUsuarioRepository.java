package br.com.sauf_api.usuario;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FotoUsuarioRepository extends JpaRepository<FotoUsuario, UUID> {

    // Só a data, sem carregar a imagem: monta a fotoUrl da resposta do /me
    @Query("select f.atualizadaEm from FotoUsuario f where f.usuarioId = :usuarioId")
    Optional<Instant> buscarAtualizadaEm(UUID usuarioId);
}
