package br.com.sauf_api.oferta;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfertaCursoRepository extends JpaRepository<OfertaCurso, UUID>, JpaSpecificationExecutor<OfertaCurso> {

    Optional<OfertaCurso> findByInstituicaoIdAndChave(UUID instituicaoId, String chave);

    List<OfertaCurso> findByInstituicaoId(UUID instituicaoId);

    List<OfertaCurso> findByInstituicaoIdAndAtivaTrue(UUID instituicaoId);

    @Query("select distinct c from OfertaCurso o join o.codigosEmec c where o.ativa = true and c in :codigos")
    Set<Integer> codigosEmecComOferta(@Param("codigos") Collection<Integer> codigos);
}
