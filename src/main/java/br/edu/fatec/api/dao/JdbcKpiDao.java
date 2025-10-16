package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class JdbcKpiDao implements KpiDao {

    private static final String SQL_PENDENCIAS =
            "SELECT COUNT(*) " +
                    "FROM mensagens m " +
                    "WHERE m.destinatario_id = ? AND m.lida = 0";

    private static final String SQL_ULTIMA_VERSAO =
            "SELECT versao " +
                    "FROM versoes_trabalho " +
                    "WHERE trabalho_id = ? " +
                    "ORDER BY created_at DESC, id DESC LIMIT 1";

    private static final String SQL_PERCENTUAL =
            "SELECT percentual_conclusao FROM trabalhos_graduacao WHERE id = ?";

    @Override
    public int countPendenciasAluno(long alunoId) {
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(SQL_PENDENCIAS)) {
            ps.setLong(1, alunoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
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
        } catch (Exception e) { e.printStackTrace(); }
        return Optional.empty();
    }

    @Override
    public double findPercentualConclusaoByTrabalhoId(long trabalhoId) {
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(SQL_PERCENTUAL)) {
            ps.setLong(1, trabalhoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0.0;
    }
}
