package br.com.sauf_api.imagem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "instituicao_imagem")
@IdClass(ImagemInstituicao.Chave.class)
public class ImagemInstituicao extends ImagemLicenciada {

    public enum Tipo { LOGO, CAPA }

    @Id
    @Column(name = "instituicao_id")
    private UUID instituicaoId;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Tipo tipo;

    protected ImagemInstituicao() {}

    ImagemInstituicao(UUID instituicaoId, Tipo tipo, ArquivoImagem arquivo, CreditoImagem credito) {
        this.instituicaoId = instituicaoId;
        this.tipo = tipo;
        trocar(arquivo, credito);
    }

    public UUID getInstituicaoId() {
        return instituicaoId;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public static class Chave implements Serializable {

        private UUID instituicaoId;
        private Tipo tipo;

        protected Chave() {}

        public Chave(UUID instituicaoId, Tipo tipo) {
            this.instituicaoId = instituicaoId;
            this.tipo = tipo;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Chave outra && Objects.equals(instituicaoId, outra.instituicaoId) && tipo == outra.tipo;
        }

        @Override
        public int hashCode() {
            return Objects.hash(instituicaoId, tipo);
        }
    }
}
