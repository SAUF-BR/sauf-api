package br.com.sauf_api.curso;

import br.com.sauf_api.area.Area;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.UUID;

@Entity
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private Area area;

    @Column(name = "codigo_cine_rotulo", unique = true, length = 10)
    private String codigoCineRotulo;

    @Column(name = "pendente_revisao", nullable = false)
    private boolean pendenteRevisao;

    @Column(name = "imagem_url")
    private String imagemUrl;

    protected Curso() {}

    public Curso(String nome, Area area) {
        this.nome = nome;
        this.area = area;
    }

    public static Curso doCenso(String codigoCineRotulo, String nomeCineRotulo, Area area) {
        Curso curso = new Curso(nomeCineRotulo, area);
        curso.codigoCineRotulo = codigoCineRotulo;
        return curso;
    }

    public static Curso pendenteDeRevisao(String nomeNoSite) {
        Curso curso = new Curso(nomeNoSite, null);
        curso.pendenteRevisao = true;
        return curso;
    }

    // D-36: quando o mesmo curso tem bacharelado e licenciatura, vale a área do bacharelado.
    public void usarAreaDoBacharelado(Area areaDoBacharelado) {
        this.area = areaDoBacharelado;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Area getArea() {
        return area;
    }

    public String getCodigoCineRotulo() {
        return codigoCineRotulo;
    }

    public boolean isPendenteRevisao() {
        return pendenteRevisao;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }

    public void setImagemUrl(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }
}
