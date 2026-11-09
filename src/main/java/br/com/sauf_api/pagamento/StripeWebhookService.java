package br.com.sauf_api.pagamento;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final StripeProperties props;
    private final StripeEventRepository eventos;
    private final EventosStripe tratador;

    public StripeWebhookService(StripeProperties props, StripeEventRepository eventos, EventosStripe tratador) {
        this.props = props;
        this.eventos = eventos;
        this.tratador = tratador;
    }

    public Event verificar(String corpo, String assinatura) {
        if (props.webhookSecret() == null || props.webhookSecret().isBlank()) {
            throw new PagamentoIndisponivelException("Webhooks do Stripe desabilitados");
        }
        if (assinatura == null) {
            throw new WebhookInvalidoException("Assinatura do webhook ausente");
        }
        Event evento;
        try {
            evento = Webhook.constructEvent(corpo, assinatura, props.webhookSecret());
        } catch (SignatureVerificationException e) {
            throw new WebhookInvalidoException("Assinatura do webhook inválida");
        } catch (RuntimeException e) {
            // O SDK lança exceção do gson (só de runtime aqui) quando o corpo não é JSON válido.
            throw new WebhookInvalidoException("Corpo do webhook inválido");
        }
        if (evento.getId() == null || evento.getType() == null) {
            throw new WebhookInvalidoException("Corpo do webhook inválido");
        }
        return evento;
    }

    /**
     * Registro e tratamento na mesma transação (D-74): se o tratamento falhar, o registro é
     * desfeito e o Stripe pode reenviar. Evento repetido não é tratado de novo.
     */
    @Transactional
    public void processar(Event evento) {
        if (eventos.inserirSeNovo(evento.getId(), evento.getType(), Instant.now()) == 0) {
            log.info("Evento do Stripe repetido, ignorado: {}", evento.getId());
            return;
        }
        tratador.tratar(evento);
        eventos.getReferenceById(evento.getId()).marcarProcessado();
    }
}
