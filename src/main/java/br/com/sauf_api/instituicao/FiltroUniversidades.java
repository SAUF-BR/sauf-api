package br.com.sauf_api.instituicao;

import br.com.sauf_api.oferta.FiltroOfertas;
import br.com.sauf_api.oferta.Modalidade;
import br.com.sauf_api.oferta.OfertaCurso;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public record FiltroUniversidades(String busca, String uf, String cidade, TipoInstituicao tipo, Modalidade modalidade) {

    public Specification<Instituicao> especificacao() {
        return (instituicao, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.isTrue(instituicao.get("ativa")));
            if (busca != null && !busca.isBlank()) {
                String termo = "%" + busca.trim().toLowerCase() + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(instituicao.get("nome")), termo),
                        cb.like(cb.lower(instituicao.get("sigla")), termo)));
            }
            if (uf != null && !uf.isBlank()) {
                predicados.add(cb.equal(cb.upper(instituicao.get("uf")), uf.trim().toUpperCase()));
            }
            if (cidade != null && !cidade.isBlank()) {
                predicados.add(cb.equal(cb.lower(instituicao.get("cidade")), cidade.trim().toLowerCase()));
            }
            if (tipo != null) {
                predicados.add(cb.equal(instituicao.get("tipo"), tipo));
            }
            if (modalidade != null) {
                Subquery<UUID> sub = query.subquery(UUID.class);
                Root<OfertaCurso> oferta = sub.from(OfertaCurso.class);
                sub.select(oferta.get("id")).where(
                        cb.equal(oferta.get("instituicao"), instituicao),
                        FiltroOfertas.visivel(oferta, cb),
                        cb.equal(oferta.get("modalidade"), modalidade));
                predicados.add(cb.exists(sub));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
