package br.com.sauf_api.favorito;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import br.com.sauf_api.comum.UsuarioLogado;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class FavoritoCursoControllerTest {

    @Test
    void cliqueDuploQueEstouraAUniqueContaComoJaFavoritado() {
        FavoritoCursoService service = mock(FavoritoCursoService.class);
        doThrow(new DataIntegrityViolationException("uk_favorito_curso"))
                .when(service).favoritar(any(), any());
        var controller = new FavoritoCursoController(service, new UsuarioLogado());

        assertThatCode(() -> controller.favoritar(UUID.randomUUID())).doesNotThrowAnyException();
    }
}