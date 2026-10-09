package br.com.sauf_api.oferta;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

public record FiltroOfertas(
        Modalidade modalidade,
        Grau grau,
        Integer duracaoSemestres,
        String uf,
        String cidade) {

    public static FiltroOfertas vazio() {
        return new FiltroOfertas(null, null, null, null, null);
    }

    /** Oferta que aparece no catálogo. Só ofertas vindas do site existem (D-33). */
    public static Predicate visivel(Root<OfertaCurso> oferta, CriteriaBuilder cb) {
        return cb.isTrue(oferta.get("ativa"));
    }

    public List<Predicate> predicados(Root<OfertaCurso> oferta, CriteriaBuilder cb) {
        List<Predicate> predicados = new ArrayList<>();
        predicados.add(visivel(oferta, cb));
        if (modalidade != null) {
            predicados.add(cb.equal(oferta.get("modalidade"), modalidade));
        }
        if (grau != null) {
            // A chave (oferta, grau) é única, então o join não duplica ofertas.
            Join<OfertaCurso, Opcao> opcao = oferta.join("opcoes");
            predicados.add(cb.equal(opcao.get("grau"), grau));
        }
        if (duracaoSemestres != null) {
            predicados.add(cb.equal(oferta.get("duracaoSemestres"), duracaoSemestres));
        }
        if (uf != null && !uf.isBlank()) {
            predicados.add(cb.equal(cb.upper(oferta.get("uf")), uf.trim().toUpperCase()));
        }
        if (cidade != null && !cidade.isBlank()) {
            predicados.add(cb.equal(cb.lower(oferta.get("cidade")), cidade.trim().toLowerCase()));
        }
        return predicados;
    }
}
