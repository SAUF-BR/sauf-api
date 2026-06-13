package br.com.sauf_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "instituicoes")
@Data
public class Instituicao {

    @Id
    private String id;

    private String nome;

    private String sigla;

    private String cidade;

    private String estado;

    @Column(name = "logo_url")
    private String logoUrl;

    private Boolean ativa;
}