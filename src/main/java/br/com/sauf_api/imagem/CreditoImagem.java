package br.com.sauf_api.imagem;

/** Quem fez a imagem, sob qual licença e onde está a página do arquivo (ex.: no Wikimedia Commons). */
public record CreditoImagem(String autor, String licenca, String licencaUrl, String fonteUrl) {}
