package br.com.sauf_api.assinatura;

import br.com.sauf_api.comum.AcaoNaoPermitidaException;
import br.com.sauf_api.simulado.JanelaPeriodo;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssinaturaService {

    private final AssinaturaRepository assinaturas;
    private final Clock clock;

    public AssinaturaService(AssinaturaRepository assinaturas, Clock clock) {
        this.clock = clock;
        this.assinaturas = assinaturas;
    }

    public List<PlanoResposta> listarPlanos() {
        return Arrays.stream(Plano.values())
                .sorted(Comparator.comparing(Plano::getPrecoMensal))
                .map(PlanoResposta::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<AssinaturaResposta> atual(UUID usuarioId) {
        return vigente(usuarioId).map(AssinaturaResposta::de);
    }

    /** Plano que vale hoje: o da assinatura vigente ou, sem ela, o Free (D-98). */
    @Transactional(readOnly = true)
    public Plano planoAtual(UUID usuarioId) {
        return vigente(usuarioId).map(Assinatura::getPlano).orElse(Plano.FREE);
    }

    // D-77: assinar e trocar de plano só com pagamento confirmado pelo Stripe.
    @Transactional(readOnly = true)
    public void recusarTrocaDePlano(UUID usuarioId, Plano plano) {
        // D-98: o Free é de quem não assina; para voltar a ele, cancela-se o plano pago.
        if (plano.gratuito()) {
            throw new AcaoNaoPermitidaException(
                    "O plano Free não precisa ser assinado. Para voltar a ele, cancele a sua assinatura");
        }
        boolean aberta = assinaturas.findFirstByUsuarioIdOrderByCriadaEmDesc(usuarioId)
                .filter(Assinatura::abertaNoStripe)
                .isPresent();
        throw new AcaoNaoPermitidaException(aberta
                ? "Para trocar de plano, use o portal da assinatura"
                : "Para assinar, use o checkout");
    }

    @Transactional
    public AssinaturaResposta cancelar(UUID usuarioId) {
        Assinatura assinatura = buscarVigente(usuarioId);
        assinatura.cancelar();
        return AssinaturaResposta.de(assinatura);
    }

    @Transactional
    public AssinaturaResposta reativar(UUID usuarioId) {
        Assinatura assinatura = buscarVigente(usuarioId);
        assinatura.reativar();
        return AssinaturaResposta.de(assinatura);
    }

    private Optional<Assinatura> vigente(UUID usuarioId) {
        return assinaturas.findFirstByUsuarioIdOrderByCriadaEmDesc(usuarioId)
                .filter(assinatura -> assinatura.vigente(hoje()));
    }

    private Assinatura buscarVigente(UUID usuarioId) {
        return vigente(usuarioId)
                .orElseThrow(() -> new AcaoNaoPermitidaException("Você não tem uma assinatura ativa"));
    }

    private LocalDate hoje() {
        // O dia vale até 23:59:59 em Brasília (D-57, CA-VIG-05): "hoje" é o dia de America/Sao_Paulo
        return JanelaPeriodo.hoje(clock);
    }
}
