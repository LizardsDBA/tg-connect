package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcTgSecaoDao implements TgSecaoDao {

    private static final String SQL_FIND =
            "SELECT trabalho_id,semestre_api,empresa_parceira,problema," +
                    " solucao_resumo,link_repositorio,tecnologias,contribuicoes,hard_skills,soft_skills,conteudo_md " +
                    "FROM tg_secao WHERE trabalho_id=? AND semestre_api=? " +
                    "ORDER BY created_at DESC LIMIT 1";

    private static final String SQL_UPSERT =
            "INSERT INTO tg_secao(trabalho_id,semestre_api,empresa_parceira,problema," +
                    " solucao_resumo,link_repositorio,tecnologias,contribuicoes,hard_skills,soft_skills,conteudo_md) " +
                    "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "empresa_parceira=VALUES(empresa_parceira)," +
                    " problema=VALUES(problema), solucao_resumo=VALUES(solucao_resumo), link_repositorio=VALUES(link_repositorio)," +
                    " tecnologias=VALUES(tecnologias), contribuicoes=VALUES(contribuicoes), hard_skills=VALUES(hard_skills)," +
                    " soft_skills=VALUES(soft_skills), conteudo_md=VALUES(conteudo_md)";

    @Override
    public Optional<TgSecaoDao.SecaoDto> findByTrabalhoAndSemestre(long trabalhoId, int semestreApi) {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(SQL_FIND)) {
            ps.setLong(1, trabalhoId);
            ps.setInt(2, semestreApi);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new TgSecaoDao.SecaoDto(
                            rs.getLong(1), rs.getInt(2), rs.getInt(3), rs.getString(4),
                            rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                            rs.getString(9), rs.getString(10), rs.getString(11), rs.getString(12), rs.getString(13)
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Optional.empty();
    }

    @Override
    public boolean upsert(long trabalhoId, int semestreApi, int ano, String semestreLetivo,
                          String empresa, String problema, String solucao, String repo,
                          String tecnologias, String contribuicoes, String hard, String soft, String conteudoMd) {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(SQL_UPSERT)) {
            int i=1;
            ps.setLong(i++, trabalhoId);
            ps.setInt(i++, semestreApi);
            ps.setInt(i++, ano);
            ps.setString(i++, semestreLetivo);
            ps.setString(i++, empresa);
            ps.setString(i++, problema);
            ps.setString(i++, solucao);
            ps.setString(i++, repo);
            ps.setString(i++, tecnologias);
            ps.setString(i++, contribuicoes);
            ps.setString(i++, hard);
            ps.setString(i++, soft);
            ps.setString(i++, conteudoMd);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    /** insert-only versionado (salvarTudo) */
    public void insertVersao(Connection con, long trabalhoId, String versao, int semestreApi,
                             String empresa, String problema, String solucao, String repo,
                             String tecnologias, String contrib, String hard, String soft,
                             String conteudoMd) throws SQLException {
        final String sql = """
          INSERT INTO tg_secao
          (trabalho_id, versao, semestre_api, empresa_parceira, problema, solucao_resumo,
           link_repositorio, tecnologias, contribuicoes, hard_skills, soft_skills, conteudo_md)
          VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (var ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setString(2, versao);
            ps.setInt(3, semestreApi);
            ps.setString(4, empresa);
            ps.setString(5, problema);
            ps.setString(6, solucao);
            ps.setString(7, repo);
            ps.setString(8, tecnologias);
            ps.setString(9, contrib);
            ps.setString(10, hard);
            ps.setString(11, soft);
            ps.setString(12, conteudoMd);
            ps.executeUpdate();
        }
    }

    /** DTO só para leitura versionada (não conflita com o record da interface) */
    public static final class SecaoVersaoDto {
        public final int semestreApi;
        public final String empresa, problema, solucao, repo, tecnologias, contrib, hard, soft;
        public SecaoVersaoDto(int semestreApi, String empresa, String problema, String solucao, String repo,
                              String tecnologias, String contrib, String hard, String soft) {
            this.semestreApi = semestreApi; this.empresa = empresa; this.problema = problema;
            this.solucao = solucao; this.repo = repo; this.tecnologias = tecnologias;
            this.contrib = contrib; this.hard = hard; this.soft = soft;
        }
    }

    /** leitura por versão (para preload do editor) */
    public List<SecaoVersaoDto> findByTrabalhoIdAndVersao(long trabalhoId, String versao) {
        final String sql = """
            SELECT semestre_api, empresa_parceira, problema, solucao_resumo, link_repositorio,
                   tecnologias, contribuicoes, hard_skills, soft_skills
              FROM tg_secao
             WHERE trabalho_id=? AND versao=?
             ORDER BY semestre_api
        """;
        List<SecaoVersaoDto> out = new ArrayList<>();
        try (var con = Database.get(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setString(2, versao);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new SecaoVersaoDto(
                            rs.getInt(1),
                            rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                            rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9)
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return out;
    }

    // ======= NOVO: suporte a validação =======

    /** Retorna 0/1 do campo versao_validada da seção (por trabalho/versão/semestre). */
    public Integer getValidacaoSecao(long trabalhoId, String versao, int semestreApi) {
        final String sql = """
        SELECT COALESCE(CAST(versao_validada AS SIGNED), 0) AS flag
          FROM tg_secao
         WHERE trabalho_id=? AND versao=? AND semestre_api=?
         LIMIT 1
    """;
        try (var con = Database.get(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setString(2, versao);
            ps.setInt(3, semestreApi);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    int v = rs.getInt(1);       // lê como inteiro, sem cast de Object
                    if (rs.wasNull()) return 0;
                    return v;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    /** Copia o flag versao_validada das seções da última versão anterior para a nova, por semestre. */
    public void copyValidacaoFromUltimaVersao(Connection con, long trabalhoId, String novaVersao) throws SQLException {
        final String sql = """
        UPDATE tg_secao AS n
        LEFT JOIN (
            SELECT t.trabalho_id, t.semestre_api, t.versao_validada
            FROM (
                SELECT s.trabalho_id, s.semestre_api, s.versao_validada,
                       ROW_NUMBER() OVER (
                           PARTITION BY s.trabalho_id, s.semestre_api
                           ORDER BY s.created_at DESC, s.id DESC
                       ) AS rn
                FROM tg_secao s
                WHERE s.trabalho_id = ? AND s.versao <> ?
            ) t
            WHERE t.rn = 1
        ) AS src
          ON src.trabalho_id = n.trabalho_id
         AND src.semestre_api = n.semestre_api
        SET n.versao_validada = COALESCE(src.versao_validada, 0)
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
