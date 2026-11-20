package br.com.sauf_api.imagem;

import java.util.UUID;

/** Só o hash da imagem, sem os bytes: basta para montar a URL versionada. */
public record VersaoImagemInstituicao(UUID instituicaoId, ImagemInstituicao.Tipo tipo, String hash) {}
