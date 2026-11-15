package br.com.sauf_api.documento;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ao subir, copia os documentos de src/main/resources/documentos-legais/*.md para o banco
 * (D-93). Versão nova: insere. Versão que já existe com o mesmo texto: nada muda.
 * Versão que já existe com texto diferente: atualiza só se ninguém a aceitou ainda; se já
 * houver aceite, a API não sobe (o texto aceito é prova; mude criando uma versão nova).
 */
@Component
public class DocumentosLegaisSincronizador {

    static final String PADRAO = "classpath*:documentos-legais/*.md";

    private static final Logger log = LoggerFactory.getLogger(DocumentosLegaisSincronizador.class);

    private final DocumentoLegalVersaoRepository versoes;
    private final AceiteDocumentoRepository aceites;

    public DocumentosLegaisSincronizador(DocumentoLegalVersaoRepository versoes, AceiteDocumentoRepository aceites) {
        this.versoes = versoes;
        this.aceites = aceites;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(0)
    @Transactional
    public void sincronizar() {
        Set<String> vistos = new HashSet<>();
        for (Resource recurso : arquivos()) {
            ArquivoDocumento arquivo = ArquivoDocumento.ler(recurso.getFilename(), ler(recurso));
            if (!vistos.add(arquivo.tipo() + "@" + arquivo.versao())) {
                throw new IllegalStateException("Dois arquivos com " + arquivo.tipo() + " versão " + arquivo.versao());
            }
            versoes.findByTipoAndVersao(arquivo.tipo(), arquivo.versao()).ifPresentOrElse(
                    existente -> conferir(existente, arquivo),
                    () -> {
                        versoes.save(new DocumentoLegalVersao(arquivo));
                        log.info("Documento legal publicado: {} {}", arquivo.tipo(), arquivo.versao());
                    });
        }
        for (TipoDocumento tipo : TipoDocumento.values()) {
            if (versoes.findByTipo(tipo).isEmpty()) {
                throw new IllegalStateException("Nenhuma versão de " + tipo + " em src/main/resources/documentos-legais");
            }
        }
    }

    private void conferir(DocumentoLegalVersao existente, ArquivoDocumento arquivo) {
        if (existente.getConteudoSha256().equals(arquivo.sha256())
                && existente.getVigenteAPartirDe().equals(arquivo.vigenteAPartirDe())
                && existente.isExigeNovoAceite() == arquivo.exigeNovoAceite()) {
            return;
        }
        if (aceites.existsByDocumentoVersaoId(existente.getId())) {
            throw new IllegalStateException("O arquivo " + arquivo.nomeArquivo() + " mudou, mas a versão "
                    + arquivo.versao() + " já foi aceita por usuários e não pode ser alterada. "
                    + "Crie um arquivo novo com a próxima versão (ex.: 1.0.1) em documentos-legais.");
        }
        existente.atualizar(arquivo);
        log.warn("Documento legal {} {} atualizado (ainda sem aceites)", arquivo.tipo(), arquivo.versao());
    }

    private static Resource[] arquivos() {
        try {
            return new PathMatchingResourcePatternResolver().getResources(PADRAO);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler os documentos legais", e);
        }
    }

    private static String ler(Resource recurso) {
        try {
            return new String(recurso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + recurso.getFilename(), e);
        }
    }
}
