package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;
import br.edu.fatec.api.model.auth.Role;
import br.edu.fatec.api.model.auth.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class JdbcLoginDao implements LoginDao {

    private static final String SQL =
            "SELECT id, nome, email, tipo " +
                    "FROM usuarios " +
                    "WHERE email = ? " +
                    "  AND senha_hash = SHA2(?, 256) " +
                    "  AND ativo = 1";

    @Override
    public Optional<User> authenticate(String email, String senhaEmTextoClaro) {
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(SQL)) {
            ps.setString(1, email);
            ps.setString(2, senhaEmTextoClaro);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    String nome = rs.getString("nome");
                    String tipo = rs.getString("tipo");
                    Role role = Role.valueOf(tipo);
                    return Optional.of(new User(id, nome, email, role));
                }
            }
        } catch (Exception e) {
            // Logue de forma adequada no seu projeto
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
