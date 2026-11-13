package br.com.sauf_api.simulado;

import br.com.sauf_api.questao.Alternativa;
import br.com.sauf_api.questao.Questao;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;

/** Uma questão dentro de um simulado, na posição {@code ordem} (a partir de 1). */
@Entity
@Table(name = "simulado_questao")
public class SimuladoQuestao {

    @EmbeddedId
    private SimuladoQuestaoId id;

    @MapsId("simuladoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulado_id")
    private Simulado simulado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questao_id")
    private Questao questao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoQuestao situacao;

    @Column(name = "alternativa_marcada", length = 2)
    private String alternativaMarcada;

    private Boolean correta;

    @Column(name = "tempo_segundos", nullable = false)
    private int tempoSegundos;

    // Só muda quando o aluno mexe na questão; a conversão de pendente na finalização não conta.
    @Column(name = "atualizada_em")
    private Instant atualizadaEm;

    protected SimuladoQuestao() {}

    SimuladoQuestao(Simulado simulado, int ordem, Questao questao) {
        this.id = new SimuladoQuestaoId(simulado.getId(), ordem);
        this.simulado = simulado;
        this.questao = questao;
        this.situacao = SituacaoQuestao.PENDENTE;
    }

    void registrar(SituacaoQuestao situacao, String alternativa, int tempoSegundos, Instant agora) {
        this.situacao = situacao;
        this.alternativaMarcada = situacao == SituacaoQuestao.RESPONDIDA ? alternativa : null;
        this.tempoSegundos = tempoSegundos;
        this.atualizadaEm = agora;
    }

    void esgotar(int limiteSegundos, Instant agora) {
        registrar(SituacaoQuestao.TEMPO_ESGOTADO, null, limiteSegundos, agora);
    }

    void encerrar() {
        if (situacao == SituacaoQuestao.PENDENTE) {
            situacao = SituacaoQuestao.EM_BRANCO;
        }
        correta = situacao == SituacaoQuestao.RESPONDIDA && alternativaMarcada.equals(rotuloCorreto());
    }

    public String rotuloCorreto() {
        return questao.getAlternativas().stream()
                .filter(Alternativa::isCorreta)
                .map(Alternativa::getRotulo)
                .findFirst()
                .orElse(null);
    }

    public ResultadoQuestao resultado() {
        if (situacao != SituacaoQuestao.RESPONDIDA) {
            return ResultadoQuestao.EM_BRANCO;
        }
        return Boolean.TRUE.equals(correta) ? ResultadoQuestao.ACERTO : ResultadoQuestao.ERRO;
    }

    public boolean ativa() {
        return atualizadaEm != null && (situacao != SituacaoQuestao.PENDENTE || tempoSegundos > 0);
    }

    public int getOrdem() {
        return id.getOrdem();
    }

    public Simulado getSimulado() {
        return simulado;
    }

    public Questao getQuestao() {
        return questao;
    }

    public SituacaoQuestao getSituacao() {
        return situacao;
    }

    public String getAlternativaMarcada() {
        return alternativaMarcada;
    }

    public Boolean getCorreta() {
        return correta;
    }

    public int getTempoSegundos() {
        return tempoSegundos;
    }

    public Instant getAtualizadaEm() {
        return atualizadaEm;
    }
}
