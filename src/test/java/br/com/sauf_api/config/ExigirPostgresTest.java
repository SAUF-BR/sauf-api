package br.com.sauf_api.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ExigirPostgresTest {

    @Test
    void aceitaUrlDoPostgres() {
        var env = new MockEnvironment().withProperty(
                "spring.datasource.url", "jdbc:postgresql://host:5432/postgres?sslmode=require");

        assertThatCode(() -> ExigirPostgres.validar(env)).doesNotThrowAnyException();
    }

    @Test
    void recusaUrlAusente() {
        assertThatThrownBy(() -> ExigirPostgres.validar(new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SAUF_DB_URL não está configurada");
    }

    @Test
    void recusaVariavelDeAmbienteNaoDefinida() {
        var env = new MockEnvironment().withProperty("spring.datasource.url", "${SAUF_DB_URL}");

        assertThatThrownBy(() -> ExigirPostgres.validar(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SAUF_DB_URL");
    }

    @Test
    void recusaBancoQueNaoEPostgres() {
        var h2 = new MockEnvironment().withProperty("spring.datasource.url", "jdbc:h2:mem:sauf");
        var semJdbc = new MockEnvironment().withProperty(
                "spring.datasource.url", "postgresql://host:5432/postgres");

        assertThatThrownBy(() -> ExigirPostgres.validar(h2)).hasMessageContaining("jdbc:postgresql://");
        assertThatThrownBy(() -> ExigirPostgres.validar(semJdbc)).hasMessageContaining("jdbc:postgresql://");
    }
}
