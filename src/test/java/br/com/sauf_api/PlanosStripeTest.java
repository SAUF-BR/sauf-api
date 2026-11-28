package br.com.sauf_api;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.sauf_api.assinatura.Plano;
import br.com.sauf_api.pagamento.PlanosStripe;
import br.com.sauf_api.pagamento.StripeProperties;
import org.junit.jupiter.api.Test;

class PlanosStripeTest {

    private static PlanosStripe planos(String basic, String plus) {
        var precos = new StripeProperties.Precos(basic, plus);
        return new PlanosStripe(new StripeProperties("sk_test_x", "whsec_x", precos, null, null, null));
    }

    @Test
    void cadaPlanoTemOSeuPriceNosDoisSentidos() {
        PlanosStripe planos = planos("price_b", "price_pl");

        assertThat(planos.priceDe(Plano.BASIC)).contains("price_b");
        assertThat(planos.priceDe(Plano.PLUS)).contains("price_pl");
        assertThat(planos.planoDe("price_b")).contains(Plano.BASIC);
        assertThat(planos.planoDe("price_pl")).contains(Plano.PLUS);
    }

    @Test
    void freeNuncaTemPrice() {
        assertThat(planos("price_b", "price_pl").priceDe(Plano.FREE)).isEmpty();
    }

    @Test
    void priceDesconhecidoNaoViraPlano() {
        assertThat(planos("price_b", "price_pl").planoDe("price_outro")).isEmpty();
    }

    @Test
    void priceVazioContaComoNaoConfigurado() {
        PlanosStripe planos = planos("", "price_pl");

        assertThat(planos.priceDe(Plano.BASIC)).isEmpty();
        assertThat(planos.planoDe("")).isEmpty();
        assertThat(planos.priceDe(Plano.PLUS)).contains("price_pl");

        assertThat(planos(null, "price_pl").priceDe(Plano.BASIC)).isEmpty();
    }

    @Test
    void priceRepetidoNaoValeParaNenhumDosPlanos() {
        PlanosStripe planos = planos("price_troque", "price_troque");

        assertThat(planos.priceDe(Plano.BASIC)).isEmpty();
        assertThat(planos.priceDe(Plano.PLUS)).isEmpty();
        assertThat(planos.planoDe("price_troque")).isEmpty();
    }
}
