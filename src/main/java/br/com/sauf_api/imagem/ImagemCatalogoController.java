package br.com.sauf_api.imagem;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

/** Leitura pública das imagens do catálogo, com ETag (o hash) e cache de 7 dias (D-99). */
@RestController
public class ImagemCatalogoController {

    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofDays(7)).cachePublic();

    private final ImagensCatalogo imagens;

    public ImagemCatalogoController(ImagensCatalogo imagens) {
        this.imagens = imagens;
    }

    @GetMapping("/api/v1/universidades/{id}/logo")
    public ResponseEntity<byte[]> logo(@PathVariable UUID id, WebRequest request) {
        return responder(imagens.daUniversidade(id, ImagemInstituicao.Tipo.LOGO), request);
    }

    @GetMapping("/api/v1/universidades/{id}/capa")
    public ResponseEntity<byte[]> capa(@PathVariable UUID id, WebRequest request) {
        return responder(imagens.daUniversidade(id, ImagemInstituicao.Tipo.CAPA), request);
    }

    @GetMapping("/api/v1/areas/{id}/imagem")
    public ResponseEntity<byte[]> area(@PathVariable UUID id, WebRequest request) {
        return responder(imagens.daArea(id), request);
    }

    private static ResponseEntity<byte[]> responder(Optional<? extends ImagemLicenciada> imagem, WebRequest request) {
        if (imagem.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ImagemLicenciada i = imagem.get();
        if (request.checkNotModified(i.getHash())) {
            return ResponseEntity.status(304).eTag(i.getHash()).cacheControl(CACHE).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(i.getTipoMime()))
                .eTag(i.getHash())
                .cacheControl(CACHE)
                .body(i.getConteudo());
    }
}
