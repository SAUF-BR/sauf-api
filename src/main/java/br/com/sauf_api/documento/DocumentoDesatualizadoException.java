package br.com.sauf_api.documento;

/** A tela mostrou uma versão que já não é a vigente: 409 com fields.aceites (D-93). */
public class DocumentoDesatualizadoException extends RuntimeException {

    public DocumentoDesatualizadoException(String mensagem) {
        super(mensagem);
    }
}
