package br.com.sauf_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "cursos")
@Data
public class Curso {

    @Id
    private String id;

    private String nome;

    @Column(name = "descricao_resumida")
    private String descricaoResumida;

    @ManyToOne
    @JoinColumn(name = "instituicao_id")
    private Instituicao instituicao;

    private Boolean ativo;
}