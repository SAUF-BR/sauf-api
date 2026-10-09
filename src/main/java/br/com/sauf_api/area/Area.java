package br.com.sauf_api.area;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo_cine", nullable = false, unique = true, length = 2)
    private String codigoCine;

    @Column(nullable = false)
    private String nome;

    protected Area() {}

    public Area(String codigoCine, String nome) {
        this.codigoCine = codigoCine;
        this.nome = nome;
    }

    public UUID getId() {
        return id;
    }

    public String getCodigoCine() {
        return codigoCine;
    }

    public String getNome() {
        return nome;
    }
}
