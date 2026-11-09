package br.com.sauf_api.pagamento;

import com.stripe.model.Event;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StripeWebhookController {

    private final StripeWebhookService service;

    public StripeWebhookController(StripeWebhookService service) {
        this.service = service;
    }

    // Corpo em bytes: a assinatura é calculada sobre o conteúdo exato que o Stripe enviou.
    @PostMapping("/api/v1/webhooks/stripe")
    public ResponseEntity<Void> receber(
            @RequestBody byte[] corpo,
            @RequestHeader(name = "Stripe-Signature", required = false) String assinatura) {
        Event evento = service.verificar(new String(corpo, StandardCharsets.UTF_8), assinatura);
        service.processar(evento);
        return ResponseEntity.ok().build();
    }
}
