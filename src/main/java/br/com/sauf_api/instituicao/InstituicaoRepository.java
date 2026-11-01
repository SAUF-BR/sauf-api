package br.com.sauf_api.instituicao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InstituicaoRepository extends JpaRepository<Instituicao, UUID>, JpaSpecificationExecutor<Instituicao> {

    Optional<Instituicao> findByIdAndAtivaTrue(UUID id);

    Optional<Instituicao> findByCodigoEmec(Integer codigoEmec);

    @Query("""
            select distinct i.cidade from Instituicao i
            where i.ativa = true and upper(i.uf) = :uf
            order by i.cidade""")
    List<String> cidadesComUniversidadeAtiva(@Param("uf") String uf);
}