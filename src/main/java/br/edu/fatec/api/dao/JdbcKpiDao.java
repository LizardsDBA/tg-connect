package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class JdbcKpiDao implements KpiDao {

    // 🔹 Pendências de validação (0–9)
    private static final String SQL_PENDENCIAS_VALIDACOES = """
        SELECT
            (9 - (
                COALESCE(sec.valid_sec, 0)
              + COALESCE(ap.valid, 0)
              + COALESCE(res.valid, 0)
            )) AS pendencias
        FROM trabalhos_graduacao tg
        LEFT JOIN (
            SELECT trabalho_id, versao, SUM(COALESCE(versao_validada, 0)) AS valid_sec
            FROM tg_secao
            GROUP BY trabalho_id, versao
        ) sec
            ON sec.trabalho_id = tg.id AND sec.versao = tg.versao_atual
        LEFT JOIN (
            SELECT trabalho_id, versao,
                   COALESCE(apresentacao_versao_validada, 0)
                 + COALESCE(consideracao_versao_validada, 0) AS valid
            FROM tg_apresentacao
        ) ap
            ON ap.trabalho_id = tg.id AND ap.versao = tg.versao_atual
        LEFT JOIN (
            SELECT trabalho_id, versao, COALESCE(versao_validada, 0) AS valid
            FROM tg_resumo
        ) res
            ON res.trabalho_id = tg.id AND res.versao = tg.versao_atual
        WHERE tg.aluno_id = ?
        """;

    // 🔹 Última versão do trabalho (mantida funcional)
    private static final String SQL_ULTIMA_VERSAO = """
        SELECT versao
        FROM versoes_trabalho
        WHERE trabalho_id = ?
        ORDER BY created_at DESC, id DESC
        LIMIT 1
        """;

    // 🔹 Percentual de conclusão com base nas 9 validações
    private static final String SQL_PERCENTUAL = """
        SELECT
            ROUND((
                (COALESCE(sec.valid_sec, 0)
                + COALESCE(ap.valid, 0)
                + COALESCE(res.valid, 0)) / 9
            ) * 100, 2) AS percentual_conclusao
        FROM trabalhos_graduacao tg
        LEFT JOIN (
            SELECT trabalho_id, versao, SUM(COALESCE(versao_validada, 0)) AS valid_sec
            FROM tg_secao
            GROUP BY trabalho_id, versao
        ) sec
            ON sec.trabalho_id = tg.id AND sec.versao = tg.versao_atual
        LEFT JOIN (
            SELECT trabalho_id, versao,
                   COALESCE(apresentacao_versao_validada, 0)
                 + COALESCE(consideracao_versao_validada, 0) AS valid
            FROM tg_apresentacao
        ) ap
            ON ap.trabalho_id = tg.id AND ap.versao = tg.versao_atual
        LEFT JOIN (
            SELECT trabalho_id, versao, COALESCE(versao_validada, 0) AS valid
            FROM tg_resumo
        ) res
            ON res.trabalho_id = tg.id AND res.versao = tg.versao_atual
        WHERE tg.id = ?
        """;

    @Override
    public int countPendenciasAluno(long alunoId) {
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(SQL_PENDENCIAS_VALIDACOES)) {
            ps.setLong(1, alunoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("pendencias");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public Optional<String> findUltimaVersaoByTrabalhoId(long trabalhoId) {
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(SQL_ULTIMA_VERSAO)) {
            ps.setLong(1, trabalhoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.ofNullable(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public double findPercentualConclusaoByTrabalhoId(long trabalhoId) {
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(SQL_PERCENTUAL)) {
            ps.setLong(1, trabalhoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("percentual_conclusao");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}
