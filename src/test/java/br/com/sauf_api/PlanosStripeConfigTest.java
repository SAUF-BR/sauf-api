package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.sauf_api.assinatura.Plano;
import br.com.sauf_api.pagamento.PlanosStripe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "STRIPE_PRICE_BASIC=price_config_basic",
    "STRIPE_PRICE_PLUS=price_config_plus",
    "STRIPE_PRICE_PREMIUM=price_config_premium"
})
@ActiveProfiles("test")
class PlanosStripeConfigTest {

    @Autowired PlanosStripe planos;

    @Test
    void leOsPricesDasVariaveisDeAmbiente() {
        assertThat(planos.priceDe(Plano.BASIC)).contains("price_config_basic");
        assertThat(planos.priceDe(Plano.PLUS)).contains("price_config_plus");
        assertThat(planos.priceDe(Plano.PREMIUM)).contains("price_config_premium");
    }
}
