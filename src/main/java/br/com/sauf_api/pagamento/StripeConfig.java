package br.com.sauf_api.pagamento;

import com.stripe.StripeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StripeProperties.class)
public class StripeConfig {

    private static final Logger log = LoggerFactory.getLogger(StripeConfig.class);

    /**
     * Só existe com STRIPE_SECRET_KEY preenchida. Quem usa recebe um ObjectProvider e
     * responde "pagamentos desabilitados" quando ele estiver vazio, como a ingestão faz com o SAUF_API_KEY.
     */
    @Bean
    StripeClient stripeClient(StripeProperties props) {
        if (!props.habilitado()) {
            log.warn("STRIPE_SECRET_KEY não configurada: pagamentos desabilitados");
            return null;
        }
        if (!props.secretKey().startsWith("sk_test_")) {
            log.warn("STRIPE_SECRET_KEY não é de modo teste: confira se isso é intencional");
        }
        return StripeClient.builder()
                .setApiKey(props.secretKey())
                .setMaxNetworkRetries(2)
                .build();
    }
}
