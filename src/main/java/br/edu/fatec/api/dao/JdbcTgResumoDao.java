package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;

import java.sql.*;
import java.util.Optional;

public class JdbcTgResumoDao implements TgResumoDao {

    // Lê a ÚLTIMA versão do resumo para um trabalho
    private static final String SQL_FIND_LATEST = """
        SELECT trabalho_id, resumo_md
          FROM tg_resumo
         WHERE trabalho_id = ?
         ORDER BY updated_at DESC, id DESC
         LIMIT 1
    """;

    // Lê por (trabalho_id, versao)
    private static final String SQL_FIND_BY_VERSAO = """
        SELECT trabalho_id, resumo_md, versao_validada
          FROM tg_resumo
         WHERE trabalho_id = ? AND versao = ?
         LIMIT 1
    """;

    /**
     * CORRIGIDO: Este método agora implementa a interface corretamente.
     */
    @Override
    public Optional<ResumoDto> findByTrabalhoId(long trabalhoId) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(SQL_FIND_LATEST)) {
            ps.setLong(1, trabalhoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Retorna o DTO da interface (sem kpis)
                    return Optional.of(new ResumoDto(
                            rs.getLong(1), // trabalho_id
                            rs.getString(2)  // resumo_md
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Optional.empty();
    }

    /**
     * CORRIGIDO: Este método agora implementa a interface corretamente.
     */
    @Override
    public boolean upsert(long trabalhoId, String resumoMd) {
        // Esta implementação provavelmente não é usada pelo seu fluxo de "salvar tudo",
        // mas precisa existir para o código compilar com a interface.
        // Se você precisar dela, teremos que escrever o SQL "UPSERT" aqui.
        return false;
    }

    // --- Métodos usados pelo EditorAlunoService ---

    /** DTO ATUALIZADO (para leitura interna com status) */
    public record ResumoVersaoDto(long trabalhoId, String resumoMd, int versaoValidada) {}

    public Optional<ResumoVersaoDto> findByTrabalhoIdAndVersao(long trabalhoId, String versao) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(SQL_FIND_BY_VERSAO)) {
            ps.setLong(1, trabalhoId);
            ps.setString(2, versao);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ResumoVersaoDto(
                            rs.getLong(1), // trabalho_id
                            rs.getString(2), // resumo_md
                            rs.getInt(3)     // versao_validada
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Optional.empty();
    }

    // Inserção "insert-only" versionada
    public void insertVersao(Connection con,
                             long trabalhoId, String versao,
                             String resumoMd) throws SQLException {
        final String sql = """
            INSERT INTO tg_resumo (trabalho_id, versao, resumo_md)
            VALUES (?,?,?)
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setString(2, versao);
            ps.setString(3, resumoMd);
            ps.executeUpdate();
        }
    }

    // ======= Suporte a validação (Métodos OK) =======

    public int getValidacaoResumo(long trabalhoId, String versao) {
        final String sql = """
            SELECT COALESCE(versao_validada,0)
              FROM tg_resumo
             WHERE trabalho_id=? AND versao=?
             LIMIT 1
        """;
        try (var con = Database.get(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setString(2, versao);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    public void copyValidacaoFromUltimaVersao(Connection con, long trabalhoId, String novaVersao) throws SQLException {
        final String sql = """
        UPDATE tg_resumo AS n
        LEFT JOIN (
            SELECT t.trabalho_id, t.versao_validada
            FROM (
                SELECT r.trabalho_id, r.versao_validada,
                       ROW_NUMBER() OVER (
                           PARTITION BY r.trabalho_id
                           ORDER BY r.updated_at DESC, r.id DESC
                       ) AS rn
                FROM tg_resumo r
                WHERE r.trabalho_id = ? AND r.versao <> ?
            ) t
            WHERE t.rn = 1
        ) AS src
          ON src.trabalho_id = n.trabalho_id
        SET n.versao_validada = COALESCE(src.versao_validada, 0)
        WHERE n.trabalho_id = ? AND n.versao = ?
    """;
        try (var ps = con.prepareStatement(sql)) {
            int i = 1;
            ps.setLong(i++, trabalhoId);
            ps.setString(i++, novaVersao);
            ps.setLong(i++, trabalhoId);
            ps.setString(i++, novaVersao);
            ps.executeUpdate();
        }
    }
}