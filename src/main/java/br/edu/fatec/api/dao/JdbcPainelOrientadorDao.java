package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;
import br.edu.fatec.api.dto.PainelOrientadorRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class JdbcPainelOrientadorDao {

    private static final String SQL =
            """
            SELECT
              uOrientador.nome AS orientador,
              uAluno.nome      AS aluno,
              tg.titulo,
              tg.tema,
              tg.versao_atual,
              COALESCE(sec.valid_sec, 0)
            + COALESCE(ap.valid, 0)
            + COALESCE(res.valid, 0) AS total_validadas,
              9 - (COALESCE(sec.valid_sec, 0)
            + COALESCE(ap.valid, 0)
            + COALESCE(res.valid, 0)) AS pendencias,
              CASE
                WHEN 9 - (COALESCE(sec.valid_sec, 0)
                       + COALESCE(ap.valid, 0)
                       + COALESCE(res.valid, 0)) = 0 THEN 'Concluído'
                WHEN 9 - (COALESCE(sec.valid_sec, 0)
                       + COALESCE(ap.valid, 0)
                       + COALESCE(res.valid, 0)) = 9 THEN 'Não avaliado'
                ELSE 'Em andamento'
              END AS status,
              ROUND(((COALESCE(sec.valid_sec,0)
                    + COALESCE(ap.valid,0)
                    + COALESCE(res.valid,0)) / 9) * 100, 2) AS percentual_conclusao
            FROM trabalhos_graduacao tg
            INNER JOIN usuarios uAluno      ON uAluno.id       = tg.aluno_id
            INNER JOIN usuarios uOrientador ON uOrientador.id  = tg.orientador_id
            LEFT JOIN (
              SELECT trabalho_id, versao, SUM(COALESCE(versao_validada,0)) AS valid_sec
              FROM tg_secao
              GROUP BY trabalho_id, versao
            ) sec  ON sec.trabalho_id = tg.id AND sec.versao = tg.versao_atual
            LEFT JOIN (
              SELECT trabalho_id, versao,
                     COALESCE(apresentacao_versao_validada,0)
                   + COALESCE(consideracao_versao_validada,0) AS valid
              FROM tg_apresentacao
            ) ap   ON ap.trabalho_id = tg.id AND ap.versao = tg.versao_atual
            LEFT JOIN (
              SELECT trabalho_id, versao, COALESCE(versao_validada,0) AS valid
              FROM tg_resumo
            ) res  ON res.trabalho_id = tg.id AND res.versao = tg.versao_atual
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
                            rs.getString("status"),
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
