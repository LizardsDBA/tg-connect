package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;
import br.edu.fatec.api.dto.VersaoHistoricoDTO;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import br.edu.fatec.api.dao.JdbcFeedbackDao.OrientandoDTO; // Para reutilizar o DTO
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
    
    /**
     * Busca todas as versões COMPLETO de um trabalho, ordenadas por data (mais recente primeiro)
     */
    public List<VersaoHistoricoDTO> listarHistoricoCompleto(Long trabalhoId) throws SQLException {
        final String sql = """
            SELECT id, versao, conteudo_md, comentario, created_at
              FROM versoes_trabalho
             WHERE trabalho_id = ? AND secao = 'COMPLETO'
             ORDER BY created_at DESC
        """;
        
        List<VersaoHistoricoDTO> resultado = new ArrayList<>();
        
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new VersaoHistoricoDTO(
                        rs.getLong("id"),
                        rs.getString("versao"),
                        rs.getString("conteudo_md"),
                        rs.getString("comentario"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        }
        
        return resultado;
    }
    
    /**
     * Busca trabalho_id pelo aluno_id
     */
    public Long obterTrabalhoIdPorAluno(Long alunoId) throws SQLException {
        final String sql = "SELECT id FROM trabalhos_graduacao WHERE aluno_id = ?";
        
        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, alunoId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        
        throw new SQLException("Trabalho não encontrado para o aluno " + alunoId);
    }

    /**
     * Lista TODOS os alunos (para o Coordenador) que possuem um TG.
     * Reutiliza o OrientandoDTO para transportar os dados.
     */
    public List<OrientandoDTO> listarTodosAlunosParaHistorico() throws SQLException {
        String sql = """
            SELECT u.id AS aluno_id, u.nome, t.id AS trabalho_id, t.status
              FROM usuarios u
              LEFT JOIN trabalhos_graduacao t ON t.aluno_id = u.id
             WHERE u.tipo = 'ALUNO' AND u.ativo = TRUE
             ORDER BY u.nome
        """;

        List<OrientandoDTO> out = new ArrayList<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new OrientandoDTO(
                            rs.getLong("aluno_id"),
                            rs.getString("nome"),
                            rs.getObject("trabalho_id") == null ? null : rs.getLong("trabalho_id"),
                            rs.getString("status")
                    ));
                }
            }
        }
        return out;
    }
}
