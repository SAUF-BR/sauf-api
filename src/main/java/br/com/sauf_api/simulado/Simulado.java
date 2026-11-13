package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.AreaQuestao;
import br.com.sauf_api.questao.Banca;
import br.com.sauf_api.questao.Questao;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Uma tentativa de simulado de um usuário (D-80). */
@Entity
public class Simulado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Banca banca;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private TamanhoSimulado tamanho;

    @Column(nullable = false)
    private boolean cronometrado;

    @Column(name = "segundos_por_questao")
    private Integer segundosPorQuestao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusSimulado status;

    @Column(name = "iniciado_em", nullable = false)
    private Instant iniciadoEm;

    @Column(name = "finalizado_em")
    private Instant finalizadoEm;

    private Integer acertos;

    private Integer erros;

    @Column(name = "em_branco")
    private Integer emBranco;

    @Column(name = "origem_id")
    private UUID origemId;

    @ElementCollection
    @CollectionTable(name = "simulado_area", joinColumns = @JoinColumn(name = "simulado_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "area", nullable = false, length = 2)
    private Set<AreaQuestao> areas = new HashSet<>();

    @OneToMany(mappedBy = "simulado", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SimuladoQuestao> questoes = new ArrayList<>();

    protected Simulado() {}

    public Simulado(
            UUID usuarioId,
            Banca banca,
            TamanhoSimulado tamanho,
            Set<AreaQuestao> areas,
            boolean cronometrado,
            Integer segundosPorQuestao,
            UUID origemId,
            Instant agora) {
        this.usuarioId = usuarioId;
        this.banca = banca;
        this.tamanho = tamanho;
        this.areas.addAll(areas);
        this.cronometrado = cronometrado;
        this.segundosPorQuestao = segundosPorQuestao;
        this.origemId = origemId;
        this.status = StatusSimulado.EM_ANDAMENTO;
        this.iniciadoEm = agora;
    }

    /** Só vale depois de salvar o simulado, porque o id da questão do simulado usa o id dele. */
    public void adicionarQuestao(Questao questao) {
        questoes.add(new SimuladoQuestao(this, questoes.size() + 1, questao));
    }

    void concluir(Instant agora, int acertos, int erros, int emBranco) {
        this.status = StatusSimulado.CONCLUIDO;
        this.finalizadoEm = agora;
        this.acertos = acertos;
        this.erros = erros;
        this.emBranco = emBranco;
    }

    public boolean concluido() {
        return status == StatusSimulado.CONCLUIDO;
    }

    public Optional<SimuladoQuestao> questao(int ordem) {
        return questoes.stream().filter(q -> q.getOrdem() == ordem).findFirst();
    }

    public List<SimuladoQuestao> questoesEmOrdem() {
        return questoes.stream().sorted(Comparator.comparingInt(SimuladoQuestao::getOrdem)).toList();
    }

    public List<AreaQuestao> areasEmOrdem() {
        return areas.stream().sorted().toList();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public Banca getBanca() {
        return banca;
    }

    public TamanhoSimulado getTamanho() {
        return tamanho;
    }

    public boolean isCronometrado() {
        return cronometrado;
    }

    public Integer getSegundosPorQuestao() {
        return segundosPorQuestao;
    }

    public StatusSimulado getStatus() {
        return status;
    }

    public Instant getIniciadoEm() {
        return iniciadoEm;
    }

    public Instant getFinalizadoEm() {
        return finalizadoEm;
    }

    public Integer getAcertos() {
        return acertos;
    }

    public Integer getErros() {
        return erros;
    }

    public Integer getEmBranco() {
        return emBranco;
    }

    public UUID getOrigemId() {
        return origemId;
    }
}
