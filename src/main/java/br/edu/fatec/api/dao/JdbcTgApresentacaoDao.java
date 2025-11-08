package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;

import java.sql.*;
import java.util.Optional;

public class JdbcTgApresentacaoDao implements TgApresentacaoDao {

    // (Os métodos findByTrabalhoId e upsert não são usados pelo EditorAlunoService, podem ser mantidos/removidos)
    // ... (seu código findByTrabalhoId e upsert) ...

    @Override
    public Optional<ApresentacaoDto> findByTrabalhoId(long trabalhoId) {
        // Implementação original (se houver)
        return Optional.empty();
    }

    @Override
    public boolean upsert(long trabalhoId, String nomeCompleto, Integer idade, String curso, String historicoAcad, String motivacao, String historicoProf, String contatosEmail, String contatosGithub, String contatosLinkedin, String conhecimentos, String consideracoes) {
        // Implementação original (se houver)
        return false;
    }


    /** Insert-only versionado usado pelo salvarTudo */
    public void insertVersao(Connection con, long trabalhoId, String versao,
                             String infoPessoais, String historicoAcad, String motivacao,
                             String historicoProf, String contatos, String conhecimentos,
                             String consideracoes) throws SQLException {
        // ATUALIZADO: Insere os dados E reseta todos os status para 0 (Pendente)
        final String sql = """
            INSERT INTO tg_apresentacao (
                trabalho_id, versao,
                nome_completo, historico_academico, motivacao_fatec,
                historico_profissional, contatos_email, principais_conhecimentos, consideracoes_finais,
                
                -- Resetar status
                nome_completo_status, historico_academico_status, motivacao_fatec_status,
                historico_profissional_status, contatos_email_status, principais_conhecimentos_status,
                consideracoes_finais_status, idade_status, curso_status
            ) VALUES (?,?,?,?,?,?,?,?,? , 0,0,0,0,0,0,0,0,0)
        """;
        try (var ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setString(2, versao);
            String nomeBloco = (infoPessoais == null) ? "" : infoPessoais.trim();
            ps.setString(3, nomeBloco);
            ps.setString(4, historicoAcad);
            ps.setString(5, motivacao);
            ps.setString(6, historicoProf);
            ps.setString(7, contatos);
            ps.setString(8, conhecimentos);
            ps.setString(9, consideracoes);
            ps.executeUpdate();
        }
    }

    /** DTO só para leitura versionada (NÃO conflita com o record da interface) */
    public static final class ApresentacaoVersaoDto {
        // ... (seu DTO original está OK)
        public final long trabalhoId;
        public final String versao;
        public final String nomeCompleto;
        public final String historicoAcad;
        public final String motivacao;
        public final String historicoProf;
        public final String contatosEmailOuLivre;
        public final String conhecimentos;
        public final String consideracoes;

        public ApresentacaoVersaoDto(long trabalhoId, String versao, String nomeCompleto,
                                     String historicoAcad, String motivacao, String historicoProf,
                                     String contatosEmailOuLivre, String conhecimentos, String consideracoes) {
            this.trabalhoId = trabalhoId; this.versao = versao;
            this.nomeCompleto = nomeCompleto; this.historicoAcad = historicoAcad; this.motivacao = motivacao;
            this.historicoProf = historicoProf; this.contatosEmailOuLivre = contatosEmailOuLivre;
            this.conhecimentos = conhecimentos; this.consideracoes = consideracoes;
        }
    }

    /** Leitura por versão (para preload) – colunas CORRETAS */
    public Optional<ApresentacaoVersaoDto> findByTrabalhoIdAndVersao(long trabalhoId, String versao) {
        // Este método já estava correto no seu arquivo, buscando as colunas de texto corretas.
        // Nenhuma alteração necessária aqui.
        final String sql = """
            SELECT trabalho_id, versao,
                   nome_completo, historico_academico, motivacao_fatec,
                   historico_profissional, contatos_email, principais_conhecimentos, consideracoes_finais
              FROM tg_apresentacao
             WHERE trabalho_id=? AND versao=?
             ORDER BY created_at DESC
             LIMIT 1
        """;
        try (var con = Database.get(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setString(2, versao);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ApresentacaoVersaoDto(
                            rs.getLong(1), rs.getString(2),
                            rs.getString(3), rs.getString(4), rs.getString(5),
                            rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9)
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Optional.empty();
    }

    // ======= NOVO: suporte a validação (CORRIGIDO) =======

    /** Retorna 1 se TODOS os campos da Aba 1 (Apresentação) estiverem Aprovados (status=1). */
    public int getApresentacaoValidada(long trabalhoId, String versao) {
        // ATUALIZADO: Verifica todos os status individuais, exceto o de considerações finais
        final String sql = """
            SELECT CASE WHEN
                nome_completo_status = 1 AND
                idade_status = 1 AND
                curso_status = 1 AND
                historico_academico_status = 1 AND
                motivacao_fatec_status = 1 AND
                historico_profissional_status = 1 AND
                contatos_email_status = 1 AND
                principais_conhecimentos_status = 1
            THEN 1 ELSE 0 END
              FROM tg_apresentacao
             WHERE trabalho_id=? AND versao=?
             LIMIT 1
        """;
        try (var con = Database.get(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setString(2, versao);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    /** Retorna 1 se o campo da Aba 9 (Considerações) estiver Aprovado (status=1). */
    public int getConsideracaoValidada(long trabalhoId, String versao) {
        // ATUALIZADO: Verifica apenas o 'consideracoes_finais_status'
        final String sql = """
            SELECT COALESCE(consideracoes_finais_status, 0)
              FROM tg_apresentacao
             WHERE trabalho_id=? AND versao=?
             LIMIT 1
        """;
        try (var con = Database.get(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setString(2, versao);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    /** Copia TODOS os flags *_status da última versão anterior para a nova. */
    public void copyValidacaoFromUltimaVersao(Connection con, long trabalhoId, String novaVersao) throws SQLException {
        // ATUALIZADO: Copia todas as colunas _status
        final String sql = """
        UPDATE tg_apresentacao AS n
        LEFT JOIN (
            SELECT t.trabalho_id,
                   t.nome_completo_status, t.idade_status, t.curso_status,
                   t.historico_academico_status, t.motivacao_fatec_status,
                   t.historico_profissional_status, t.contatos_email_status,
                   t.principais_conhecimentos_status, t.consideracoes_finais_status
            FROM (
                SELECT a.trabalho_id, a.created_at, a.id,
                       a.nome_completo_status, a.idade_status, a.curso_status,
                       a.historico_academico_status, a.motivacao_fatec_status,
                       a.historico_profissional_status, a.contatos_email_status,
                       a.principais_conhecimentos_status, a.consideracoes_finais_status,
                       ROW_NUMBER() OVER (
                           PARTITION BY a.trabalho_id
                           ORDER BY a.created_at DESC, a.id DESC
                       ) AS rn
                FROM tg_apresentacao a
                WHERE a.trabalho_id = ? AND a.versao <> ?
            ) t
            WHERE t.rn = 1
        ) AS src
          ON src.trabalho_id = n.trabalho_id
        SET n.nome_completo_status = COALESCE(src.nome_completo_status, 0),
            n.idade_status = COALESCE(src.idade_status, 0),
            n.curso_status = COALESCE(src.curso_status, 0),
            n.historico_academico_status = COALESCE(src.historico_academico_status, 0),
            n.motivacao_fatec_status = COALESCE(src.motivacao_fatec_status, 0),
            n.historico_profissional_status = COALESCE(src.historico_profissional_status, 0),
            n.contatos_email_status = COALESCE(src.contatos_email_status, 0),
            n.principais_conhecimentos_status = COALESCE(src.principais_conhecimentos_status, 0),
            n.consideracoes_finais_status = COALESCE(src.consideracoes_finais_status, 0)
        WHERE n.trabalho_id = ? AND n.versao = ?
    """;
        try (var ps = con.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, trabalhoId);
            ps.setString(i++, novaVersao);
            ps.setLong(i++, trabalhoId);
            ps.setString(i++, novaVersao);
            ps.executeUpdate();
        }
    }
}