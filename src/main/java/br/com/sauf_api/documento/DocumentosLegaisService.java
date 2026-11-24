package br.com.sauf_api.documento;

import br.com.sauf_api.comum.CampoInvalidoException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Documentos legais vigentes, pendências de aceite e registro dos aceites (D-93).
 * Vigente = a maior versão cuja data já chegou (dá para publicar com antecedência).
 * Pendente = a pessoa não aceitou a última versão que exige novo aceite, nem uma posterior.
 */
@Service
public class DocumentosLegaisService {

    static final String MENSAGEM_DESATUALIZADO =
            "Os documentos foram atualizados enquanto você lia. Leia a nova versão e aceite de novo.";

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final DocumentoLegalVersaoRepository versoes;
    private final AceiteDocumentoRepository aceites;

    public DocumentosLegaisService(DocumentoLegalVersaoRepository versoes, AceiteDocumentoRepository aceites) {
        this.versoes = versoes;
        this.aceites = aceites;
    }

    @Transactional(readOnly = true)
    public List<DocumentoResposta> vigentes() {
        return Arrays.stream(TipoDocumento.values()).map(this::vigente).map(DocumentoResposta::de).toList();
    }

    /** Documentos que a pessoa precisa aceitar (de novo) antes de seguir usando. */
    @Transactional(readOnly = true)
    public List<TipoDocumento> pendentes(UUID usuarioId) {
        Set<UUID> aceitas = aceites.findByUsuarioId(usuarioId).stream()
                .map(AceiteDocumento::getDocumentoVersaoId)
                .collect(Collectors.toSet());
        return Arrays.stream(TipoDocumento.values())
                .filter(tipo -> !emDia(tipo, aceitas))
                .toList();
    }

    /**
     * Confere os aceites sem gravar nada: no cadastro, chamado antes de criar a conta.
     * {@code exigirTodos}: todos os documentos precisam vir. Versão que não é a vigente: 409.
     */
    @Transactional(readOnly = true)
    public void conferir(List<AceiteRequest> pedidos, boolean exigirTodos) {
        vigentesPedidas(pedidos, exigirTodos);
    }

    /** Registra os aceites das versões vigentes (confere de novo antes de gravar). */
    @Transactional
    public void registrar(UUID usuarioId, List<AceiteRequest> pedidos, OrigemAceite origem, boolean exigirTodos) {
        for (DocumentoLegalVersao atual : vigentesPedidas(pedidos, exigirTodos)) {
            if (!aceites.existsByUsuarioIdAndDocumentoVersaoId(usuarioId, atual.getId())) {
                aceites.save(new AceiteDocumento(usuarioId, atual, origem));
            }
        }
    }

    private List<DocumentoLegalVersao> vigentesPedidas(List<AceiteRequest> pedidos, boolean exigirTodos) {
        Map<TipoDocumento, AceiteRequest> porTipo = pedidos.stream()
                .collect(Collectors.toMap(AceiteRequest::tipo, Function.identity(), (a, b) -> b));
        if (exigirTodos) {
            for (TipoDocumento tipo : TipoDocumento.values()) {
                if (!porTipo.containsKey(tipo)) {
                    throw new CampoInvalidoException("aceites", "Aceite " + tipo.nomeNaFrase() + " para continuar");
                }
            }
        }
        return porTipo.values().stream().map(pedido -> {
            DocumentoLegalVersao atual = vigente(pedido.tipo());
            if (!atual.getVersao().equals(pedido.versao().trim())) {
                throw new DocumentoDesatualizadoException(MENSAGEM_DESATUALIZADO);
            }
            return atual;
        }).toList();
    }

    private DocumentoLegalVersao vigente(TipoDocumento tipo) {
        LocalDate hoje = LocalDate.now(FUSO);
        return versoes.findByTipo(tipo).stream()
                .filter(versao -> !versao.getVigenteAPartirDe().isAfter(hoje))
                .max(Comparator.comparing(DocumentoLegalVersao::getVersao, Versao.ORDEM))
                .orElseThrow(() -> new IllegalStateException("Nenhuma versão vigente de " + tipo));
    }

    // Em dia: aceitou a última versão vigente que exige aceite, ou alguma posterior a ela
    private boolean emDia(TipoDocumento tipo, Set<UUID> aceitas) {
        LocalDate hoje = LocalDate.now(FUSO);
        List<DocumentoLegalVersao> vigentes = versoes.findByTipo(tipo).stream()
                .filter(versao -> !versao.getVigenteAPartirDe().isAfter(hoje))
                .sorted(Comparator.comparing(DocumentoLegalVersao::getVersao, Versao.ORDEM))
                .toList();
        if (vigentes.isEmpty()) return true;
        Optional<DocumentoLegalVersao> corte = vigentes.reversed().stream()
                .filter(DocumentoLegalVersao::isExigeNovoAceite)
                .findFirst();
        String minima = corte.map(DocumentoLegalVersao::getVersao).orElse(vigentes.getFirst().getVersao());
        return vigentes.stream()
                .filter(versao -> Versao.ORDEM.compare(versao.getVersao(), minima) >= 0)
                .anyMatch(versao -> aceitas.contains(versao.getId()));
    }
}
