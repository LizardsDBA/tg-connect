package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;

import java.sql.*;
import java.util.Optional;

public class JdbcVersoesTrabalhoDao {
    public void insertCompleto(Connection con, long trabalhoId, String secao,
                               String versao, String md, String comentario) throws SQLException {
        final String sql = """
          INSERT INTO versoes_trabalho (trabalho_id, secao, versao, conteudo_md, comentario)
          VALUES (?,?,?,?,?)
        """;
        try (var ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setString(2, secao);
            ps.setString(3, versao);
            ps.setString(4, md);
            ps.setString(5, comentario);
            ps.executeUpdate();
        }
    }
}