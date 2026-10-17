package br.com.sauf_api.instituicao;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndicadorIesRepository extends JpaRepository<IndicadorIes, UUID> {

    Optional<IndicadorIes> findByInstituicaoIdAndAno(UUID instituicaoId, Integer ano);

    Optional<IndicadorIes> findFirstByInstituicaoIdOrderByAnoDesc(UUID instituicaoId);
}
