package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.sauf_api.assinatura.Plano;
import br.com.sauf_api.pagamento.PlanosStripe;
import br.com.sauf_api.pagamento.StripeProperties;
import org.junit.jupiter.api.Test;

class PlanosStripeTest {

    private static PlanosStripe planos(String basic, String plus, String premium) {
        var precos = new StripeProperties.Precos(basic, plus, premium);
        return new PlanosStripe(new StripeProperties("sk_test_x", "whsec_x", precos, null, null, null));
    }

    @Test
    void cadaPlanoTemOSeuPriceNosDoisSentidos() {
        PlanosStripe planos = planos("price_b", "price_pl", "price_pr");

        assertThat(planos.priceDe(Plano.BASIC)).contains("price_b");
        assertThat(planos.priceDe(Plano.PLUS)).contains("price_pl");
        assertThat(planos.priceDe(Plano.PREMIUM)).contains("price_pr");
        assertThat(planos.planoDe("price_b")).contains(Plano.BASIC);
        assertThat(planos.planoDe("price_pl")).contains(Plano.PLUS);
        assertThat(planos.planoDe("price_pr")).contains(Plano.PREMIUM);
    }

    @Test
    void priceDesconhecidoNaoViraPlano() {
        assertThat(planos("price_b", "price_pl", "price_pr").planoDe("price_outro")).isEmpty();
    }

    @Test
    void priceVazioContaComoNaoConfigurado() {
        PlanosStripe planos = planos("", null, "price_pr");

        assertThat(planos.priceDe(Plano.BASIC)).isEmpty();
        assertThat(planos.priceDe(Plano.PLUS)).isEmpty();
        assertThat(planos.planoDe("")).isEmpty();
        assertThat(planos.priceDe(Plano.PREMIUM)).contains("price_pr");
    }

    @Test
    void priceRepetidoNaoValeParaNenhumDosPlanos() {
        PlanosStripe planos = planos("price_troque", "price_troque", "price_pr");

        assertThat(planos.priceDe(Plano.BASIC)).isEmpty();
        assertThat(planos.priceDe(Plano.PLUS)).isEmpty();
        assertThat(planos.planoDe("price_troque")).isEmpty();
        assertThat(planos.planoDe("price_pr")).contains(Plano.PREMIUM);
    }
}
