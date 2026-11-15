package br.com.sauf_api.documento;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoLegalVersaoRepository extends JpaRepository<DocumentoLegalVersao, UUID> {

    Optional<DocumentoLegalVersao> findByTipoAndVersao(TipoDocumento tipo, String versao);

    List<DocumentoLegalVersao> findByTipo(TipoDocumento tipo);
}
