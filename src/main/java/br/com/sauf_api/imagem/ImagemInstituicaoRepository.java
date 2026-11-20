package br.com.sauf_api.imagem;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ImagemInstituicaoRepository extends JpaRepository<ImagemInstituicao, ImagemInstituicao.Chave> {

    Optional<ImagemInstituicao> findByInstituicaoIdAndTipo(UUID instituicaoId, ImagemInstituicao.Tipo tipo);

    // Só o hash, sem carregar os bytes: monta as URLs das listas
    @Query("select new br.com.sauf_api.imagem.VersaoImagemInstituicao(i.instituicaoId, i.tipo, i.hash) "
            + "from ImagemInstituicao i where i.instituicaoId in :ids")
    List<VersaoImagemInstituicao> versoes(Collection<UUID> ids);
}
