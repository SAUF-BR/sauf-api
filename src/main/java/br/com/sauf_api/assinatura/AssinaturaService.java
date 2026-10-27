package br.com.sauf_api.assinatura;

import br.com.sauf_api.comum.AcaoNaoPermitidaException;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

    public AssinaturaService(AssinaturaRepository assinaturas) {
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

    // Provisório (P-14): sem gateway, assinar ou trocar conta como pagamento confirmado.
    @Transactional
    public AssinaturaResposta trocarPlano(UUID usuarioId, Plano plano) {
        Optional<Assinatura> existente = vigente(usuarioId);
        if (existente.isEmpty()) {
            Assinatura nova = new Assinatura(usuarioId, plano, hoje().plusMonths(1));
            return AssinaturaResposta.de(assinaturas.save(nova));
        }
        Assinatura assinatura = existente.get();
        assinatura.trocarPlano(plano);
        return AssinaturaResposta.de(assinatura);
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
        return LocalDate.now(ZoneOffset.UTC);
    }
}
