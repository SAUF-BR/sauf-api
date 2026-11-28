package br.com.sauf_api.assinatura;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** O dia da assinatura é o de Brasília: vale até 23:59:59 do dia do renovaEm (D-57). */
class AssinaturaFusoTest {

    static final UUID USUARIO = UUID.randomUUID();

    private AssinaturaService servico(String instanteUtc, Assinatura existente) {
        AssinaturaRepository repositorio = (AssinaturaRepository) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {AssinaturaRepository.class},
                (proxy, metodo, args) -> metodo.getName().equals("findFirstByUsuarioIdOrderByCriadaEmDesc")
                        ? Optional.ofNullable(existente)
                        : null);
        return new AssinaturaService(repositorio, Clock.fixed(Instant.parse(instanteUtc), ZoneOffset.UTC));
    }

    @Test
    void as2359DeBrasiliaAindaVale() {
        // 02:59 UTC de 17/11 = 23:59 de 16/11 em Brasília
        Assinatura a = new Assinatura(USUARIO, Plano.BASIC, LocalDate.parse("2026-11-16"));

        Optional<AssinaturaResposta> atual = servico("2026-11-17T02:59:00Z", a).atual(USUARIO);

        assertTrue(atual.isPresent());
    }

    @Test
    void aMeiaNoiteDeBrasiliaVence() {
        // 03:00 UTC de 17/11 = 00:00 de 17/11 em Brasília
        Assinatura a = new Assinatura(USUARIO, Plano.BASIC, LocalDate.parse("2026-11-16"));

        Optional<AssinaturaResposta> atual = servico("2026-11-17T03:00:00Z", a).atual(USUARIO);

        assertTrue(atual.isEmpty());
    }
}
