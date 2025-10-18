package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;
import br.edu.fatec.api.dao.MensagemDao;
import br.edu.fatec.api.model.Mensagem;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class JdbcMensagemDao implements MensagemDao {
    private Mensagem map(ResultSet rs) throws SQLException {
        Mensagem m = new Mensagem();
        m.setId(rs.getLong("id"));
        m.setTrabalhoId(rs.getLong("trabalho_id"));
        m.setRemetenteId(rs.getLong("remetente_id"));
        m.setDestinatarioId(rs.getLong("destinatario_id"));
        m.setTipo(rs.getString("tipo"));
        m.setConteudo(rs.getString("conteudo"));
        m.setLida(rs.getBoolean("lida"));
        Timestamp ts = rs.getTimestamp("created_at");
        m.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
        return m;
    }

    @Override
    public List<Mensagem> listarHistorico(Long trabalhoId) throws SQLException {
        String sql = """
        SELECT id, trabalho_id, remetente_id, destinatario_id, tipo, conteudo, lida, created_at
        FROM mensagens
        WHERE trabalho_id = ?
        ORDER BY created_at ASC, id ASC
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Mensagem> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    @Override
    public List<Mensagem> listarNovas(Long trabalhoId, LocalDateTime afterCreatedAt) throws SQLException {
        String sql = """
        SELECT id, trabalho_id, remetente_id, destinatario_id, tipo, conteudo, lida, created_at
        FROM mensagens
        WHERE trabalho_id = ?
          AND created_at > ?
        ORDER BY created_at ASC, id ASC
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setTimestamp(2, Timestamp.valueOf(afterCreatedAt));
            try (ResultSet rs = ps.executeQuery()) {
                List<Mensagem> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    @Override
    public Long inserirTexto(Long trabalhoId, Long remetenteId, Long destinatarioId, String conteudo) throws SQLException {
        String sql = """
        INSERT INTO mensagens (trabalho_id, remetente_id, destinatario_id, tipo, conteudo)
        VALUES (?, ?, ?, 'TEXTO', ?)
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, trabalhoId);
            ps.setLong(2, remetenteId);
            ps.setLong(3, destinatarioId);
            ps.setString(4, conteudo);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                return null;
            }
        }
    }

    @Override
    public int marcarComoLidas(Long trabalhoId, Long destinatarioId) throws SQLException {
        String sql = """
        UPDATE mensagens
           SET lida = TRUE
         WHERE trabalho_id = ?
           AND destinatario_id = ?
           AND lida = FALSE
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setLong(2, destinatarioId);
            return ps.executeUpdate();
        }
    }
}
