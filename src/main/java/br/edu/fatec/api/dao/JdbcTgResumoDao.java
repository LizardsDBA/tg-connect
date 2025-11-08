package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

public class JdbcTgResumoDao implements TgResumoDao {

    // ... (Seus métodos findByTrabalhoId, upsert, findByTrabalhoIdAndVersao) ...

    @Override
    public Optional<ResumoDto> findByTrabalhoId(long trabalhoId) {
        // Implementação original (se houver)
        return Optional.empty();
    }

    @Override
    public boolean upsert(long trabalhoId, String resumoMd) {
        // Implementação original (se houver)
        return false;
    }

    // Lê por (trabalho_id, versao)
    private static final String SQL_FIND_BY_VERSAO = """
        SELECT trabalho_id, resumo_md
          FROM tg_resumo
         WHERE trabalho_id = ? AND versao = ?
         LIMIT 1
    """;

    public Optional<ResumoDto> findByTrabalhoIdAndVersao(long trabalhoId, String versao) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(SQL_FIND_BY_VERSAO)) {
            ps.setLong(1, trabalhoId);
            ps.setString(2, versao);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // CORRIGIDO: Removida a leitura da coluna 3 (kpis)
                    return Optional.of(new ResumoDto(
                            rs.getLong(1), // trabalho_id
                            rs.getString(2)  // resumo_md
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
        // ATUALIZADO: Insere o status 0 (Pendente)
        final String sql = """
            INSERT INTO tg_resumo (trabalho_id, versao, resumo_md, versao_validada)
            VALUES (?,?,?, 0)
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setString(2, versao);
            ps.setString(3, resumoMd);
            ps.executeUpdate();
        }
    }

    // ======= NOVO: suporte a validação (CORRIGIDO) =======
    // OBS: Seus métodos de validação para Resumo já estavam corretos,
    // pois o nome da coluna 'versao_validada' foi mantido (mesmo mudando o tipo).
    // Nenhuma alteração necessária aqui.

    /** Retorna 0/1 do campo versao_validada da versão informada. */
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

    /** Copia o flag versao_validada da última versão anterior para a nova. */
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