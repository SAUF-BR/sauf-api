package br.com.sauf_api.questao;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TextoBaseRepository extends JpaRepository<TextoBase, UUID> {

    Optional<TextoBase> findByProvaAndHash(Prova prova, String hash);

    long countByProva(Prova prova);

    /** Textos da prova que nenhuma questão usa mais (o texto mudou numa reimportação). */
    @Modifying
    @Query("""
            delete from TextoBase t where t.prova = :prova
            and not exists (select 1 from Questao q where q.textoBase = t)
            """)
    int apagarSemQuestao(Prova prova);
}
