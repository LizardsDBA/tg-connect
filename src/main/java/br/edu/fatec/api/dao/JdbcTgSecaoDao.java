package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcTgSecaoDao implements TgSecaoDao {

    // ... (Seus métodos findByTrabalhoAndSemestre e upsert) ...
    @Override
    public Optional<SecaoDto> findByTrabalhoAndSemestre(long trabalhoId, int semestreApi) { return Optional.empty(); }
    @Override
    public boolean upsert(long trabalhoId, int semestreApi, int ano, String semestreLetivo, String empresa, String problema, String solucao, String repo, String tecnologias, String contribuicoes, String hard, String soft, String conteudoMd) { return false; }

    /** insert-only versionado (salvarTudo) */
    public void insertVersao(Connection con, long trabalhoId, String versao, int semestreApi,
                             String empresa, String problema, String solucao, String repo,
                             String tecnologias, String contrib, String hard, String soft,
                             String conteudoMd) throws SQLException {
        // CORREÇÃO: Não reseta mais os status para 0 aqui.
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

    /** DTO só para leitura versionada (ATUALIZADO) */
    public static final class SecaoVersaoDto {
        public final int semestreApi;
        public final String empresa, problema, solucao, repo, tecnologias, contrib, hard, soft;
        // Campos de status adicionados
        public final int empresaStatus, problemaStatus, solucaoStatus, repoStatus, tecnologiasStatus, contribStatus, hardStatus, softStatus;

        public SecaoVersaoDto(int semestreApi, String empresa, String problema, String solucao, String repo, String tecnologias, String contrib, String hard, String soft, int empresaStatus, int problemaStatus, int solucaoStatus, int repoStatus, int tecnologiasStatus, int contribStatus, int hardStatus, int softStatus) {
            this.semestreApi = semestreApi;
            this.empresa = empresa;
            this.problema = problema;
            this.solucao = solucao;
            this.repo = repo;
            this.tecnologias = tecnologias;
            this.contrib = contrib;
            this.hard = hard;
            this.soft = soft;
            this.empresaStatus = empresaStatus;
            this.problemaStatus = problemaStatus;
            this.solucaoStatus = solucaoStatus;
            this.repoStatus = repoStatus;
            this.tecnologiasStatus = tecnologiasStatus;
            this.contribStatus = contribStatus;
            this.hardStatus = hardStatus;
            this.softStatus = softStatus;
        }
    }

    /** leitura por versão (para preload do editor) (ATUALIZADO) */
    public List<SecaoVersaoDto> findByTrabalhoIdAndVersao(long trabalhoId, String versao) {
        final String sql = """
            SELECT semestre_api, empresa_parceira, problema, solucao_resumo, link_repositorio,
                   tecnologias, contribuicoes, hard_skills, soft_skills,
                   
                   empresa_parceira_status, problema_status, solucao_resumo_status,
                   link_repositorio_status, tecnologias_status, contribuicoes_status,
                   hard_skills_status, soft_skills_status
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
                            rs.getInt("semestre_api"),
                            rs.getString("empresa_parceira"), rs.getString("problema"), rs.getString("solucao_resumo"), rs.getString("link_repositorio"),
                            rs.getString("tecnologias"), rs.getString("contribuicoes"), rs.getString("hard_skills"), rs.getString("soft_skills"),
                            rs.getInt("empresa_parceira_status"), rs.getInt("problema_status"), rs.getInt("solucao_resumo_status"),
                            rs.getInt("link_repositorio_status"), rs.getInt("tecnologias_status"), rs.getInt("contribuicoes_status"),
                            rs.getInt("hard_skills_status"), rs.getInt("soft_skills_status")
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return out;
    }

    // ======= NOVO: suporte a validação (CORRIGIDO) =======

    /** Retorna 1 se TODOS os campos da Seção (API) estiverem Aprovados (status=1). */
    public Integer getValidacaoSecao(long trabalhoId, String versao, int semestreApi) {
        final String sql = """
        SELECT CASE WHEN
            empresa_parceira_status = 1 AND
            problema_status = 1 AND
            solucao_resumo_status = 1 AND
            link_repositorio_status = 1 AND
            tecnologias_status = 1 AND
            contribuicoes_status = 1 AND
            hard_skills_status = 1 AND
            soft_skills_status = 1 AND
            COALESCE(conteudo_md_status, 1) = 1
        THEN 1 ELSE 0 END
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
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    /** Copia TODOS os flags *_status da última versão anterior para a nova, por semestre. */
    public void copyValidacaoFromUltimaVersao(Connection con, long trabalhoId, String novaVersao) throws SQLException {
        final String sql = """
        UPDATE tg_secao AS n
        LEFT JOIN (
            SELECT t.trabalho_id, t.semestre_api,
                   t.empresa_parceira_status, t.problema_status, t.solucao_resumo_status,
                   t.link_repositorio_status, t.tecnologias_status, t.contribuicoes_status,
                   t.hard_skills_status, t.soft_skills_status, t.conteudo_md_status
            FROM (
                SELECT s.*,
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
        SET n.empresa_parceira_status = COALESCE(src.empresa_parceira_status, 0),
            n.problema_status = COALESCE(src.problema_status, 0),
            n.solucao_resumo_status = COALESCE(src.solucao_resumo_status, 0),
            n.link_repositorio_status = COALESCE(src.link_repositorio_status, 0),
            n.tecnologias_status = COALESCE(src.tecnologias_status, 0),
            n.contribuicoes_status = COALESCE(src.contribuicoes_status, 0),
            n.hard_skills_status = COALESCE(src.hard_skills_status, 0),
            n.soft_skills_status = COALESCE(src.soft_skills_status, 0),
            n.conteudo_md_status = COALESCE(src.conteudo_md_status, 0)
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