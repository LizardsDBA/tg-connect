package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;
import br.edu.fatec.api.dto.VersaoHistoricoDTO;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    public Optional<VersaoHistoricoDTO> findUltimaVersaoCorrigida(Long trabalhoId) throws SQLException {
        final String sql = """
            SELECT
                vt.id, vt.versao, vt.conteudo_md, vt.comentario, vt.created_at
            FROM
                versoes_trabalho AS vt
            WHERE
                vt.trabalho_id = ? AND vt.secao = 'COMPLETO'
                AND EXISTS (
                    SELECT 1
                    FROM pareceres p
                    WHERE p.trabalho_id = vt.trabalho_id
                      AND p.versao = vt.versao
                )
            ORDER BY
                vt.created_at DESC
            LIMIT 1
        """;

        try (Connection con = Database.get();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, trabalhoId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new VersaoHistoricoDTO(
                            rs.getLong("id"),
                            rs.getString("versao"),
                            rs.getString("conteudo_md"),
                            // Este é o 'versoes_trabalho.comentario', que ainda pode ser nulo (e tudo bem)
                            rs.getString("comentario"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        }
        return Optional.empty(); // Nenhuma versão com pareceres encontrada
    }
}
