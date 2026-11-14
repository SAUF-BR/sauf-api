package br.com.sauf_api.simulado;

import java.util.UUID;

/** Linha da consulta nativa do ranking. O id vem como texto: o H2 dos testes devolve UUID como bytes. */
public interface LinhaRanking {

    String getUsuarioId();

    default UUID usuario() {
        return UUID.fromString(getUsuarioId());
    }

    Number getSegundos();

    Number getSimulados();

    Number getPosicao();
}
