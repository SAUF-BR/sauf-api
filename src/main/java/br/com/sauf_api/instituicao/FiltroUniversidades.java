package br.com.sauf_api.instituicao;

import br.com.sauf_api.oferta.FiltroOfertas;
import br.com.sauf_api.oferta.Modalidade;
import br.com.sauf_api.oferta.OfertaCurso;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public record FiltroUniversidades(
        String busca,
        String uf,
        String cidade,
        List<TipoInstituicao> tipos,
        List<Modalidade> modalidades,
        FaixaNotaMec notaMec) {

    private static final String COM_ACENTO = "áàâãäéèêëíìîïóòôõöúùûüçñ-";
    private static final String SEM_ACENTO = "aaaaaeeeeiiiiooooouuuucn ";

    public FiltroUniversidades comTipos(List<TipoInstituicao> novosTipos) {
        return new FiltroUniversidades(busca, uf, cidade, novosTipos, modalidades, notaMec);
    }

    public FiltroUniversidades comModalidades(List<Modalidade> novasModalidades) {
        return new FiltroUniversidades(busca, uf, cidade, tipos, novasModalidades, notaMec);
    }

    public Specification<Instituicao> especificacao() {
        return (instituicao, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.isTrue(instituicao.get("ativa")));
            String textoBusca = textoDaBusca();
            if (!textoBusca.isEmpty()) {
                String termo = "%" + textoBusca + "%";
                predicados.add(cb.or(
                        cb.like(minusculoSemAcento(cb, instituicao.get("nome")), termo),
                        cb.like(minusculoSemAcento(cb, instituicao.get("sigla")), termo)));
            }
            if (uf != null && !uf.isBlank()) {
                predicados.add(cb.equal(cb.upper(instituicao.get("uf")), uf.trim().toUpperCase()));
            }
            if (cidade != null && !cidade.isBlank()) {
                predicados.add(cb.equal(cb.lower(instituicao.get("cidade")), cidade.trim().toLowerCase()));
            }
            if (tipos != null && !tipos.isEmpty()) {
                predicados.add(instituicao.get("tipo").in(tipos));
            }
            if (modalidades != null && !modalidades.isEmpty()) {
                Subquery<UUID> sub = query.subquery(UUID.class);
                Root<OfertaCurso> oferta = sub.from(OfertaCurso.class);
                sub.select(oferta.get("id")).where(
                        cb.equal(oferta.get("instituicao"), instituicao),
                        FiltroOfertas.visivel(oferta, cb),
                        oferta.get("modalidade").in(modalidades));
                predicados.add(cb.exists(sub));
            }
            if (notaMec != null) {
                predicados.add(cb.between(instituicao.get("notaMec"), notaMec.minima(), notaMec.maxima()));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }

    private String textoDaBusca() {
        if (busca == null) {
            return "";
        }
        return semAcento(busca.toLowerCase()).replace('-', ' ').trim().replaceAll("\\s+", " ");
    }

    private static Expression<String> minusculoSemAcento(CriteriaBuilder cb, Expression<String> coluna) {
        return cb.function("translate", String.class, cb.lower(coluna), cb.literal(COM_ACENTO), cb.literal(SEM_ACENTO));
    }

    private static String semAcento(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}