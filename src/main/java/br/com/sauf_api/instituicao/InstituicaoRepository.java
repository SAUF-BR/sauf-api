package br.com.sauf_api.instituicao;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InstituicaoRepository extends JpaRepository<Instituicao, UUID>, JpaSpecificationExecutor<Instituicao> {

    Optional<Instituicao> findByIdAndAtivaTrue(UUID id);

    Optional<Instituicao> findByCodigoEmec(Integer codigoEmec);
}
