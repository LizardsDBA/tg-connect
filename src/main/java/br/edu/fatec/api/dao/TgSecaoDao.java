package br.edu.fatec.api.dao;

import java.util.Optional;

public interface TgSecaoDao {
    Optional<SecaoDto> findByTrabalhoAndSemestre(long trabalhoId, int semestreApi);
    boolean upsert(long trabalhoId, int semestreApi, int ano, String semestreLetivo,
                   String empresa, String problema, String solucao, String repo,
                   String tecnologias, String contribuicoes, String hard, String soft, String conteudoMd);

    record SecaoDto(
            long trabalhoId, int semestreApi, int ano, String semestreLetivo,
            String empresa, String problema, String solucao, String repo,
            String tecnologias, String contribuicoes, String hard, String soft, String conteudoMd
    ) {}
}
