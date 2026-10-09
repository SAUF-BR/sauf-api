package br.com.sauf_api.oferta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OfertaCursoRepository extends JpaRepository<OfertaCurso, UUID>, JpaSpecificationExecutor<OfertaCurso> {

    Optional<OfertaCurso> findByInstituicaoIdAndChave(UUID instituicaoId, String chave);

    List<OfertaCurso> findByInstituicaoId(UUID instituicaoId);

    List<OfertaCurso> findByInstituicaoIdAndAtivaTrue(UUID instituicaoId);
}
