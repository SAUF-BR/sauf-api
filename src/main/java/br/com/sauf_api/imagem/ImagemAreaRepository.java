package br.com.sauf_api.imagem;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ImagemAreaRepository extends JpaRepository<ImagemArea, UUID> {

    // Poucas áreas (11 no Cine): lê todas as versões de uma vez, sem os bytes
    @Query("select new br.com.sauf_api.imagem.VersaoImagemArea(i.areaId, i.hash) from ImagemArea i")
    List<VersaoImagemArea> versoes();
}
