package br.com.sauf_api.imagem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "area_imagem")
public class ImagemArea extends ImagemLicenciada {

    @Id
    @Column(name = "area_id")
    private UUID areaId;

    protected ImagemArea() {}

    ImagemArea(UUID areaId, ArquivoImagem arquivo, CreditoImagem credito) {
        this.areaId = areaId;
        trocar(arquivo, credito);
    }

    public UUID getAreaId() {
        return areaId;
    }
}
