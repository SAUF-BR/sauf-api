package br.com.sauf_api.favorito;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoritoUniversidadeRepository extends JpaRepository<FavoritoUniversidade, UUID> {

    @EntityGraph(attributePaths = "instituicao")
    List<FavoritoUniversidade> findByUsuarioIdAndInstituicaoAtivaTrueOrderByCriadoEmDesc(UUID usuarioId);

    boolean existsByUsuarioIdAndInstituicaoId(UUID usuarioId, UUID instituicaoId);

    long deleteByUsuarioIdAndInstituicaoId(UUID usuarioId, UUID instituicaoId);

    @Query("""
            select o.instituicao.id, count(distinct o.cidade) from OfertaCurso o
            where o.ativa = true and o.cidade is not null and o.instituicao.id in :ids
            group by o.instituicao.id""")
    List<Object[]> campiPorInstituicao(@Param("ids") Collection<UUID> ids);
}