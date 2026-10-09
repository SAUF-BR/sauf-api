package br.com.sauf_api.curso;

import br.com.sauf_api.oferta.FiltroOfertas;
import br.com.sauf_api.oferta.OfertaCurso;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public record FiltroCursos(String busca, UUID areaId, FiltroOfertas ofertas) {

    public Specification<Curso> especificacao() {
        return (curso, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (busca != null && !busca.isBlank()) {
                predicados.add(cb.like(cb.lower(curso.get("nome")), "%" + busca.trim().toLowerCase() + "%"));
            }
            if (areaId != null) {
                predicados.add(cb.equal(curso.get("area").get("id"), areaId));
            }
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<OfertaCurso> oferta = sub.from(OfertaCurso.class);
            List<Predicate> daOferta = ofertas.predicados(oferta, cb);
            daOferta.add(cb.equal(oferta.get("curso"), curso));
            sub.select(oferta.get("id")).where(daOferta.toArray(Predicate[]::new));
            predicados.add(cb.exists(sub));
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
