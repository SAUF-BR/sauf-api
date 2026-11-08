package br.com.sauf_api.questao;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProvaRepository extends JpaRepository<Prova, UUID> {

    Optional<Prova> findByBancaAndAnoAndEdicaoAndCaderno(Banca banca, Integer ano, String edicao, String caderno);
}
