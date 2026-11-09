package br.com.sauf_api.pagamento;

import com.stripe.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EventosStripe {

    private static final Logger log = LoggerFactory.getLogger(EventosStripe.class);

    public void tratar(Event evento) {
        log.info("Evento do Stripe recebido: {} ({})", evento.getId(), evento.getType());
    }
}
