package br.com.sauf_api.pagamento;

import br.com.sauf_api.assinatura.Plano;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Mapeamento Plano <-> Price do Stripe, vindo só da configuração (D-72).
 * Price vazio ou repetido em mais de um plano conta como não configurado: com o mesmo price,
 * não dá para saber qual plano foi comprado.
 */
@Component
public class PlanosStripe {

    private static final Logger log = LoggerFactory.getLogger(PlanosStripe.class);

    private final Map<Plano, String> pricePorPlano = new EnumMap<>(Plano.class);
    private final Map<String, Plano> planoPorPrice = new HashMap<>();

    public PlanosStripe(StripeProperties props) {
        Map<String, List<Plano>> planosPorPrice = Arrays.stream(Plano.values())
                .filter(plano -> preenchido(configurado(props.precos(), plano)))
                .collect(Collectors.groupingBy(plano -> configurado(props.precos(), plano)));

        planosPorPrice.forEach((price, planos) -> {
            if (planos.size() == 1) {
                pricePorPlano.put(planos.get(0), price);
                planoPorPrice.put(price, planos.get(0));
            }
        });

        if (props.habilitado()) {
            avisarPlanosSemPrice();
        }
    }

    public Optional<String> priceDe(Plano plano) {
        return Optional.ofNullable(pricePorPlano.get(plano));
    }

    public Optional<Plano> planoDe(String price) {
        return Optional.ofNullable(planoPorPrice.get(price));
    }

    private void avisarPlanosSemPrice() {
        List<Plano> semPrice = new ArrayList<>(List.of(Plano.values()));
        semPrice.removeAll(pricePorPlano.keySet());
        if (!semPrice.isEmpty()) {
            log.warn("Planos sem price do Stripe válido (vazio ou repetido): {}", semPrice);
        }
    }

    private static String configurado(StripeProperties.Precos precos, Plano plano) {
        return switch (plano) {
            case BASIC -> precos.basic();
            case PLUS -> precos.plus();
            case PREMIUM -> precos.premium();
        };
    }

    private static boolean preenchido(String price) {
        return price != null && !price.isBlank();
    }
}
