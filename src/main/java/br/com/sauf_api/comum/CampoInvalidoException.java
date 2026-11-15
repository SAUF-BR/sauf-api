package br.com.sauf_api.comum;

/** Regra de negócio que recusa um campo do corpo: 400 com a mensagem em fields.&lt;campo&gt;. */
public class CampoInvalidoException extends RuntimeException {

    private final String campo;

    public CampoInvalidoException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
