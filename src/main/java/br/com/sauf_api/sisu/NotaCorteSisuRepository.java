package br.com.sauf_api.sisu;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaCorteSisuRepository extends JpaRepository<NotaCorteSisu, UUID> {

    Optional<NotaCorteSisu> findByCodigoEmecCursoAndTurnoAndAno(Integer codigoEmecCurso, String turno, Integer ano);

    List<NotaCorteSisu> findByCodigoEmecCursoInAndNotaIsNotNull(Collection<Integer> codigosEmecCurso);
}
