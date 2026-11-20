package br.com.sauf_api.imagem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Logo e capa das universidades e imagem das áreas Cine, guardadas no Postgres (D-99).
 * As URLs levam ?v=<início do hash>: mudou a imagem, muda a URL, e o navegador pode guardar em cache.
 */
@Service
@Transactional(readOnly = true)
public class ImagensCatalogo {

    public static final String CAMINHO_UNIVERSIDADE = "/api/v1/universidades/{id}/{tipo}";
    public static final String CAMINHO_AREA = "/api/v1/areas/{id}/imagem";

    private final ImagemInstituicaoRepository instituicoes;
    private final ImagemAreaRepository areas;

    public ImagensCatalogo(ImagemInstituicaoRepository instituicoes, ImagemAreaRepository areas) {
        this.instituicoes = instituicoes;
        this.areas = areas;
    }

    /** URLs das imagens das universidades, por id; quem não tem imagem fica fora do mapa. */
    public UrlsUniversidades urlsUniversidades(Collection<UUID> instituicaoIds) {
        Map<UUID, String> logos = new HashMap<>();
        Map<UUID, String> capas = new HashMap<>();
        if (!instituicaoIds.isEmpty()) {
            for (VersaoImagemInstituicao v : instituicoes.versoes(instituicaoIds)) {
                String url = url(CAMINHO_UNIVERSIDADE, v.hash(), v.instituicaoId(), v.tipo().name().toLowerCase());
                (v.tipo() == ImagemInstituicao.Tipo.LOGO ? logos : capas).put(v.instituicaoId(), url);
            }
        }
        return new UrlsUniversidades(logos, capas);
    }

    /** URL da imagem de cada área que tem imagem. */
    public CreditoImagem credito(UUID instituicaoId, ImagemInstituicao.Tipo tipo) {
        return instituicoes.credito(instituicaoId, tipo).orElse(null);
    }

    public Map<UUID, String> urlsAreas() {
        Map<UUID, String> urls = new HashMap<>();
        for (VersaoImagemArea v : areas.versoes()) {
            urls.put(v.areaId(), url(CAMINHO_AREA, v.hash(), v.areaId()));
        }
        return urls;
    }

    public Optional<ImagemInstituicao> daUniversidade(UUID instituicaoId, ImagemInstituicao.Tipo tipo) {
        return instituicoes.findByInstituicaoIdAndTipo(instituicaoId, tipo);
    }

    public Optional<ImagemArea> daArea(UUID areaId) {
        return areas.findById(areaId);
    }

    /** Grava, troca ou (com arquivo null) apaga. Devolve true se algo mudou. */
    @Transactional
    public boolean definirDaUniversidade(
            UUID instituicaoId, ImagemInstituicao.Tipo tipo, ArquivoImagem arquivo, CreditoImagem credito) {
        var atual = instituicoes.findByInstituicaoIdAndTipo(instituicaoId, tipo);
        if (arquivo == null) {
            atual.ifPresent(instituicoes::delete);
            return atual.isPresent();
        }
        if (atual.isPresent()) {
            return atual.get().trocar(arquivo, credito);
        }
        instituicoes.save(new ImagemInstituicao(instituicaoId, tipo, arquivo, credito));
        return true;
    }

    @Transactional
    public boolean definirDaArea(UUID areaId, ArquivoImagem arquivo, CreditoImagem credito) {
        var atual = areas.findById(areaId);
        if (atual.isPresent()) {
            return atual.get().trocar(arquivo, credito);
        }
        areas.save(new ImagemArea(areaId, arquivo, credito));
        return true;
    }

    // Endereço completo (o front usa direto no <img>); fora de uma requisição, só o caminho
    private static String url(String caminho, String hash, Object... variaveis) {
        var base = RequestContextHolder.getRequestAttributes() == null
                ? UriComponentsBuilder.fromPath(caminho)
                : ServletUriComponentsBuilder.fromCurrentContextPath().path(caminho);
        return base.queryParam("v", hash.substring(0, 12)).buildAndExpand(variaveis).toUriString();
    }

    public record UrlsUniversidades(Map<UUID, String> logos, Map<UUID, String> capas) {

        public static final UrlsUniversidades NENHUMA = new UrlsUniversidades(Map.of(), Map.of());

        public String logo(UUID id) {
            return id == null ? null : logos.get(id);
        }

        public String capa(UUID id) {
            return id == null ? null : capas.get(id);
        }
    }
}
