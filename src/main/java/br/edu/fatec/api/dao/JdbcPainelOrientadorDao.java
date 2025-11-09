package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;
import br.edu.fatec.api.dto.PainelOrientadorRow; // Precisamos atualizar este DTO no próximo passo

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcPainelOrientadorDao {

    public List<PainelOrientadorRow> listarParaCoordenador() {
        // ... (seu código existente) ...
        List<PainelOrientadorRow> out = new ArrayList<>();
        final String sqlIds = "SELECT DISTINCT orientador_id FROM trabalhos_graduacao";
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sqlIds);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long orientadorId = rs.getLong(1);
                out.addAll(listar(orientadorId));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }


    /**
     * SQL ATUALIZADA:
     * 1. Remove o CASE complexo para 'status'.
     * 2. Seleciona o 'tg.status' real (EM_ANDAMENTO, ENTREGUE, etc.)
     * 3. O cálculo de pendencias/percentual (58 campos) já está correto.
     */
    private static final String SQL =
            """
            SELECT
              uOrientador.nome AS orientador,
              uAluno.nome      AS aluno,
              tg.titulo,
              tg.tema,
              tg.versao_atual,
              tg.status        AS status_fluxo, -- NOVO CAMPO DE STATUS
              
              (COALESCE(ap.valid_ap, 0) + COALESCE(sec.valid_sec, 0) + COALESCE(res.valid_res, 0)) AS total_validadas,
              (58 - (COALESCE(ap.valid_ap, 0) + COALESCE(sec.valid_sec, 0) + COALESCE(res.valid_res, 0))) AS pendencias,
              ROUND(((COALESCE(ap.valid_ap, 0) + COALESCE(sec.valid_sec, 0) + COALESCE(res.valid_res, 0)) / 58.0) * 100, 2) AS percentual_conclusao
              
            FROM trabalhos_graduacao tg
            INNER JOIN usuarios uAluno      ON uAluno.id       = tg.aluno_id AND uAluno.ativo = TRUE
            INNER JOIN usuarios uOrientador ON uOrientador.id  = tg.orientador_id
            INNER JOIN orientacoes o       ON o.aluno_id      = uAluno.id AND o.ativo = TRUE

            -- Subquery para Apresentação (9 campos)
            LEFT JOIN (
                SELECT 
                    trabalho_id, versao,
                    SUM(CASE WHEN nome_completo_status = 1 THEN 1 ELSE 0 END +
                        CASE WHEN idade_status = 1 THEN 1 ELSE 0 END +
                        CASE WHEN curso_status = 1 THEN 1 ELSE 0 END +
                        CASE WHEN historico_academico_status = 1 THEN 1 ELSE 0 END +
                        CASE WHEN motivacao_fatec_status = 1 THEN 1 ELSE 0 END +
                        CASE WHEN historico_profissional_status = 1 THEN 1 ELSE 0 END +
                        CASE WHEN contatos_email_status = 1 THEN 1 ELSE 0 END +
                        CASE WHEN principais_conhecimentos_status = 1 THEN 1 ELSE 0 END +
                        CASE WHEN consideracoes_finais_status = 1 THEN 1 ELSE 0 END) AS valid_ap
                FROM tg_apresentacao
                GROUP BY trabalho_id, versao
            ) ap ON ap.trabalho_id = tg.id AND ap.versao = tg.versao_atual

            -- Subquery para Seções (8 campos * 6 seções = 48)
            LEFT JOIN (
                SELECT 
                    trabalho_id, versao,
                    SUM(
                        (CASE WHEN empresa_parceira_status = 1 THEN 1 ELSE 0 END) +
                        (CASE WHEN problema_status = 1 THEN 1 ELSE 0 END) +
                        (CASE WHEN solucao_resumo_status = 1 THEN 1 ELSE 0 END) +
                        (CASE WHEN link_repositorio_status = 1 THEN 1 ELSE 0 END) +
                        (CASE WHEN tecnologias_status = 1 THEN 1 ELSE 0 END) +
                        (CASE WHEN contribuicoes_status = 1 THEN 1 ELSE 0 END) +
                        (CASE WHEN hard_skills_status = 1 THEN 1 ELSE 0 END) +
                        (CASE WHEN soft_skills_status = 1 THEN 1 ELSE 0 END)
                    ) AS valid_sec
                FROM tg_secao
                GROUP BY trabalho_id, versao
            ) sec ON sec.trabalho_id = tg.id AND sec.versao = tg.versao_atual

            -- Subquery para Resumo (1 campo)
            LEFT JOIN (
                SELECT 
                    trabalho_id, versao,
                    (CASE WHEN versao_validada = 1 THEN 1 ELSE 0 END) AS valid_res
                FROM tg_resumo
                GROUP BY trabalho_id, versao
            ) res ON res.trabalho_id = tg.id AND res.versao = tg.versao_atual
            
            WHERE uOrientador.id = ?
            """;

    public List<PainelOrientadorRow> listar(long orientadorId) {
        List<PainelOrientadorRow> out = new ArrayList<>();
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(SQL)) {
            ps.setLong(1, orientadorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new PainelOrientadorRow(
                            rs.getString("orientador"),
                            rs.getString("aluno"),
                            rs.getString("titulo"),
                            rs.getString("tema"),
                            rs.getString("versao_atual"),
                            rs.getInt("total_validadas"),
                            rs.getInt("pendencias"),
                            rs.getString("status_fluxo"), // ATUALIZADO
                            rs.getDouble("percentual_conclusao")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }
}