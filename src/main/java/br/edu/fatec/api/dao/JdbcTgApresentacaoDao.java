package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;

import java.sql.*;
import java.util.Optional;

public class JdbcTgApresentacaoDao implements TgApresentacaoDao {

    private static final String SQL_FIND =
            "SELECT trabalho_id,nome_completo,idade,curso,historico_academico,motivacao_fatec," +
                    " historico_profissional,contatos_email,contatos_github,contatos_linkedin," +
                    " principais_conhecimentos,consideracoes_finais " +
                    "FROM tg_apresentacao WHERE trabalho_id=? " +
                    "ORDER BY created_at DESC LIMIT 1";

    private static final String SQL_UPSERT =
            "INSERT INTO tg_apresentacao(" +
                    " trabalho_id,nome_completo,idade,curso,historico_academico,motivacao_fatec," +
                    " historico_profissional,contatos_email,contatos_github,contatos_linkedin," +
                    " principais_conhecimentos,consideracoes_finais) " +
                    "VALUES(?,?,?,?,?,?,?,?,?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    " nome_completo=VALUES(nome_completo), idade=VALUES(idade), curso=VALUES(curso)," +
                    " historico_academico=VALUES(historico_academico), motivacao_fatec=VALUES(motivacao_fatec)," +
                    " historico_profissional=VALUES(historico_profissional), contatos_email=VALUES(contatos_email)," +
                    " contatos_github=VALUES(contatos_github), contatos_linkedin=VALUES(contatos_linkedin)," +
                    " principais_conhecimentos=VALUES(principais_conhecimentos), consideracoes_finais=VALUES(consideracoes_finais)";

    @Override
    public Optional<TgApresentacaoDao.ApresentacaoDto> findByTrabalhoId(long trabalhoId) {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(SQL_FIND)) {
            ps.setLong(1, trabalhoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new TgApresentacaoDao.ApresentacaoDto(
                            rs.getLong(1), rs.getString(2), (Integer) rs.getObject(3),
                            rs.getString(4), rs.getString(5), rs.getString(6),
                            rs.getString(7), rs.getString(8), rs.getString(9),
                            rs.getString(10), rs.getString(11), rs.getString(12)
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Optional.empty();
    }

    @Override
    public boolean upsert(long trabalhoId, String nomeCompleto, Integer idade, String curso,
                          String historicoAcad, String motivacao, String historicoProf,
                          String contatosEmail, String contatosGithub, String contatosLinkedin,
                          String conhecimentos, String consideracoes) {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(SQL_UPSERT)) {
            int i=1;
            ps.setLong(i++, trabalhoId);
            ps.setString(i++, nomeCompleto);
            if (idade == null) ps.setNull(i++, Types.INTEGER); else ps.setInt(i++, idade);
            ps.setString(i++, curso);
            ps.setString(i++, historicoAcad);
            ps.setString(i++, motivacao);
            ps.setString(i++, historicoProf);
            ps.setString(i++, contatosEmail);
            ps.setString(i++, contatosGithub);
            ps.setString(i++, contatosLinkedin);
            ps.setString(i++, conhecimentos);
            ps.setString(i++, consideracoes);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    /** Insert-only versionado usado pelo salvarTudo */
    public void insertVersao(Connection con, long trabalhoId, String versao,
                             String infoPessoais, String historicoAcad, String motivacao,
                             String historicoProf, String contatos, String conhecimentos,
                             String consideracoes) throws SQLException {
        final String sql = """
            INSERT INTO tg_apresentacao (
                trabalho_id, versao,
                nome_completo, historico_academico, motivacao_fatec,
                historico_profissional, contatos_email, principais_conhecimentos, consideracoes_finais
            ) VALUES (?,?,?,?,?,?,?,?,?)
        """;
        try (var ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setString(2, versao);
            // Derivar nome do bloco livre (fallback se não achar)
            String nomeBloco = (infoPessoais == null) ? "" : infoPessoais.trim();
            ps.setString(3, nomeBloco);
            ps.setString(4, historicoAcad);
            ps.setString(5, motivacao);
            ps.setString(6, historicoProf);
            ps.setString(7, contatos);          // hoje guardamos "contatos" livres aqui
            ps.setString(8, conhecimentos);
            ps.setString(9, consideracoes);
            ps.executeUpdate();
        }
    }

    private String extrairNome(String infoPessoais) {
        // TODO (futuro): regex "**Nome completo:** (.*)$"
        return null;
    }

    /** DTO só para leitura versionada (NÃO conflita com o record da interface) */
    public static final class ApresentacaoVersaoDto {
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
}
