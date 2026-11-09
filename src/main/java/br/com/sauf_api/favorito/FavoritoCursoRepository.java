package br.com.sauf_api.favorito;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoritoCursoRepository extends JpaRepository<FavoritoCurso, UUID> {

    @EntityGraph(attributePaths = {"oferta", "oferta.curso", "oferta.instituicao"})
    List<FavoritoCurso> findByUsuarioIdAndOfertaAtivaTrueAndOfertaInstituicaoAtivaTrueOrderByCriadoEmDesc(UUID usuarioId);

    boolean existsByUsuarioIdAndOfertaId(UUID usuarioId, UUID ofertaId);

    long deleteByUsuarioIdAndOfertaId(UUID usuarioId, UUID ofertaId);
} 
