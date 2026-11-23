package br.com.sauf_api.notificacao;

import br.com.sauf_api.calendario.CalendarioService;
import br.com.sauf_api.calendario.EventoCalendario;
import br.com.sauf_api.calendario.EventoCalendarioRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria notificações para os usuários conforme os prazos do calendário se aproximam.
 * Roda uma vez por dia; {@code notificacao_evento_gerada} garante que o mesmo aviso não é duplicado.
 */
@Service
public class GeradorNotificacoesCalendario {

    private static final Logger log = LoggerFactory.getLogger(GeradorNotificacoesCalendario.class);

    private final EventoCalendarioRepository eventos;
    private final NotificacaoRepository notificacoes;
    private final NotificacaoEventoGeradaRepository registros;
    private final EntityManager em;
    private final Clock relogio;

    public GeradorNotificacoesCalendario(
            EventoCalendarioRepository eventos,
            NotificacaoRepository notificacoes,
            NotificacaoEventoGeradaRepository registros,
            EntityManager em,
            Clock relogio) {
        this.eventos = eventos;
        this.notificacoes = notificacoes;
        this.registros = registros;
        this.em = em;
        this.relogio = relogio;
    }

    /** Dispara todo dia às 7h de Brasília; em testes, chama {@link #executar(LocalDate)} direto. */
    @Scheduled(cron = "0 0 7 * * *", zone = "America/Sao_Paulo")
    public void executar() {
        executar(LocalDate.now(relogio.withZone(CalendarioService.FUSO)));
    }

    @Transactional
    public int executar(LocalDate hoje) {
        int geradas = 0;
        for (JanelaAviso janela : JanelaAviso.TODAS) {
            LocalDate alvo = hoje.plusDays(janela.dias());
            // noPeriodo devolve todo evento que atravessa o dia alvo; filtramos pelo prazo.
            List<EventoCalendario> candidatos = eventos.noPeriodo(alvo, alvo).stream()
                    .filter(e -> e.prazo().equals(alvo))
                    .toList();
            for (EventoCalendario evento : candidatos) {
                geradas += gerarPara(evento, janela);
            }
        }
        if (geradas > 0) {
            log.info("gerador de notificacoes do calendario: {} avisos criados em {}", geradas, hoje);
        }
        return geradas;
    }

    private int gerarPara(EventoCalendario evento, JanelaAviso janela) {
        Set<UUID> jaAvisados = registros.usuariosJaAvisados(evento.getId(), janela.dias());
        List<UUID> todosUsuarios = em.createQuery("SELECT u.id FROM Usuario u", UUID.class).getResultList();
        Instant agora = relogio.instant();
        int criadas = 0;
        for (UUID usuarioId : todosUsuarios) {
            if (jaAvisados.contains(usuarioId)) {
                continue;
            }
            notificacoes.save(new Notificacao(
                    usuarioId,
                    TextoNotificacaoCalendario.titulo(evento),
                    TextoNotificacaoCalendario.descricao(janela),
                    TextoNotificacaoCalendario.ROTULO_ACAO,
                    TextoNotificacaoCalendario.ROTA_ACAO));
            registros.save(new NotificacaoEventoGerada(evento.getId(), usuarioId, janela.dias(), agora));
            criadas++;
        }
        return criadas;
    }
}
