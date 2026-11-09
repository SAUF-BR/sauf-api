package br.com.sauf_api.pagamento;

import br.com.sauf_api.assinatura.Assinatura;
import br.com.sauf_api.assinatura.AssinaturaRepository;
import br.com.sauf_api.assinatura.Plano;
import br.com.sauf_api.comum.AcaoNaoPermitidaException;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.param.checkout.SessionCreateParams;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    static final String VERSAO_TERMO = "v1";

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    private final ObjectProvider<StripeClient> stripe;
    private final StripeProperties props;
    private final PlanosStripe planos;
    private final AssinaturaRepository assinaturas;

    public CheckoutService(
            ObjectProvider<StripeClient> stripe,
            StripeProperties props,
            PlanosStripe planos,
            AssinaturaRepository assinaturas) {
        this.stripe = stripe;
        this.props = props;
        this.planos = planos;
        this.assinaturas = assinaturas;
    }

    /** Devolve a URL do Checkout hospedado. A assinatura só é criada quando o webhook confirmar (D-74). */
    public String criarSessao(UUID usuarioId, Plano plano) {
        StripeClient cliente = stripe.getIfAvailable();
        if (cliente == null) {
            throw new PagamentoIndisponivelException("Pagamentos desabilitados");
        }
        String price = planos.priceDe(plano).orElseThrow(() -> new PagamentoIndisponivelException(
                "O plano " + plano.getNome() + " está indisponível para assinatura"));

        Optional<Assinatura> ultima = assinaturas.findFirstByUsuarioIdOrderByCriadaEmDesc(usuarioId);
        if (ultima.filter(Assinatura::abertaNoStripe).isPresent()) {
            throw new AcaoNaoPermitidaException("Você já tem uma assinatura: gerencie o plano pelo portal");
        }

        SessionCreateParams.Builder params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setClientReferenceId(usuarioId.toString())
                .addLineItem(SessionCreateParams.LineItem.builder().setPrice(price).setQuantity(1L).build())
                .setSuccessUrl(props.urlSucesso())
                .setCancelUrl(props.urlCancelamento())
                .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                        .putMetadata("usuario_id", usuarioId.toString())
                        .putMetadata("aceite_termo_maior_idade", "true")
                        .putMetadata("aceite_versao_termo", VERSAO_TERMO)
                        .putMetadata("aceite_em", Instant.now().toString())
                        .build());
        ultima.map(Assinatura::getStripeCustomerId).ifPresent(params::setCustomer);

        try {
            return cliente.v1().checkout().sessions().create(params.build()).getUrl();
        } catch (StripeException e) {
            log.error("Stripe recusou a criação do checkout: {}", e.getMessage());
            throw new FalhaNoGatewayException("Não foi possível iniciar o pagamento", e);
        }
    }
}
