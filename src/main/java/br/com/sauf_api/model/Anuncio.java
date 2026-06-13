package br.com.sauf_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "anuncios")
@Data
public class Anuncio {

    @Id
    private String id;

    private String titulo;

    @Column(name = "imagem_url")
    private String imagemUrl;

    @Column(name = "link_destino")
    private String linkDestino;

    private Boolean ativo;
}