package br.edu.fatec.api.dao;

import java.util.Optional;

public interface TgApresentacaoDao {
    Optional<ApresentacaoDto> findByTrabalhoId(long trabalhoId);
    boolean upsert(long trabalhoId, String nomeCompleto, Integer idade, String curso,
                   String historicoAcad, String motivacao, String historicoProf,
                   String contatosEmail, String contatosGithub, String contatosLinkedin,
                   String conhecimentos, String consideracoes);

    record ApresentacaoDto(
            long trabalhoId, String nomeCompleto, Integer idade, String curso,
            String historicoAcad, String motivacao, String historicoProf,
            String contatosEmail, String contatosGithub, String contatosLinkedin,
            String conhecimentos, String consideracoes
    ) {}
}
