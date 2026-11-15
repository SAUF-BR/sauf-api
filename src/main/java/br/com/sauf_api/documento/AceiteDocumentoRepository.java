package br.com.sauf_api.documento;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AceiteDocumentoRepository extends JpaRepository<AceiteDocumento, UUID> {

    List<AceiteDocumento> findByUsuarioId(UUID usuarioId);

    boolean existsByDocumentoVersaoId(UUID documentoVersaoId);

    boolean existsByUsuarioIdAndDocumentoVersaoId(UUID usuarioId, UUID documentoVersaoId);
}
