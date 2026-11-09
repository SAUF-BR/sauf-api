package br.com.sauf_api.pagamento;

import br.com.sauf_api.assinatura.Assinatura;
import br.com.sauf_api.assinatura.AssinaturaRepository;
import br.com.sauf_api.comum.AcaoNaoPermitidaException;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.param.billingportal.SessionCreateParams;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class PortalService {

    private static final Logger log = LoggerFactory.getLogger(PortalService.class);

    private final ObjectProvider<StripeClient> stripe;
    private final StripeProperties props;
    private final AssinaturaRepository assinaturas;

    public PortalService(ObjectProvider<StripeClient> stripe, StripeProperties props, AssinaturaRepository assinaturas) {
        this.stripe = stripe;
        this.props = props;
        this.assinaturas = assinaturas;
    }

    public String criarSessao(UUID usuarioId) {
        StripeClient cliente = stripe.getIfAvailable();
        if (cliente == null) {
            throw new PagamentoIndisponivelException("Pagamentos desabilitados");
        }
        String customer = assinaturas.findFirstByUsuarioIdOrderByCriadaEmDesc(usuarioId)
                .map(Assinatura::getStripeCustomerId)
                .orElseThrow(() -> new AcaoNaoPermitidaException("Você ainda não tem uma assinatura para gerenciar"));

        SessionCreateParams params = SessionCreateParams.builder()
                .setCustomer(customer)
                .setReturnUrl(props.urlRetornoPortal())
                .build();
        try {
            return cliente.v1().billingPortal().sessions().create(params).getUrl();
        } catch (StripeException e) {
            log.error("Stripe recusou a criação do portal: {}", e.getMessage());
            throw new FalhaNoGatewayException("Não foi possível abrir o portal da assinatura", e);
        }
    }
}
