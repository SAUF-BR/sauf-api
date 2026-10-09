package br.com.sauf_api.curso;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CursoRepository extends JpaRepository<Curso, UUID>, JpaSpecificationExecutor<Curso> {

    Optional<Curso> findByCodigoCineRotulo(String codigoCineRotulo);

    Optional<Curso> findFirstByPendenteRevisaoTrueAndNomeIgnoreCase(String nome);

    Optional<Curso> findFirstByPendenteRevisaoFalseAndNomeIgnoreCase(String nome);
}
