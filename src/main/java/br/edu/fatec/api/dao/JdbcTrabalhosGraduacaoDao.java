package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;

import java.sql.*;
import java.util.Optional;

public class JdbcTrabalhosGraduacaoDao implements TrabalhosGraduacaoDao {

    private static final String SQL_FIND_ID =
            "SELECT id FROM trabalhos_graduacao WHERE aluno_id = ?";

    @Override
    public Optional<Long> findIdByAlunoId(long alunoId) {
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(SQL_FIND_ID)) {
            ps.setLong(1, alunoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getLong(1));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public Optional<String> findVersaoAtual(long trabalhoId) {
        final String sql = "SELECT versao_atual FROM trabalhos_graduacao WHERE id=?";
        try (var con = Database.get(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return Optional.ofNullable(rs.getString(1));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public void updateVersaoAtual(Connection con, long trabalhoId, String versao) throws SQLException {
        final String sql = "UPDATE trabalhos_graduacao SET versao_atual=? WHERE id=?";
        try (var ps = con.prepareStatement(sql)) {
            ps.setString(1, versao);
            ps.setLong(2, trabalhoId);
            ps.executeUpdate();
        }
    }
}
