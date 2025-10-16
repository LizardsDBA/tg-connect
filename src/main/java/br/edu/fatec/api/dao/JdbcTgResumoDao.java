package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

public class JdbcTgResumoDao implements TgResumoDao {

    // Lê a ÚLTIMA versão do resumo para um trabalho (pelo created_at desc)
    private static final String SQL_FIND_LATEST = """
        SELECT trabalho_id, resumo_md, kpis
          FROM tg_resumo
         WHERE trabalho_id = ?
         ORDER BY updated_at DESC, id DESC
         LIMIT 1
    """;

    // Lê por (trabalho_id, versao)
    private static final String SQL_FIND_BY_VERSAO = """
        SELECT trabalho_id, resumo_md, kpis
          FROM tg_resumo
         WHERE trabalho_id = ? AND versao = ?
         LIMIT 1
    """;

    @Override
    public Optional<ResumoDto> findByTrabalhoId(long trabalhoId) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(SQL_FIND_LATEST)) {
            ps.setLong(1, trabalhoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Observação: no contrato da interface, kpis é String.
                    // Vamos devolver como string formatada do DECIMAL.
                    BigDecimal k = rs.getBigDecimal(3);
                    String kpisStr = (k == null) ? null : k.stripTrailingZeros().toPlainString();
                    return Optional.of(new ResumoDto(
                            rs.getLong(1),
                            rs.getString(2),
                            kpisStr
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Optional.empty();
    }

    @Override
    public boolean upsert(long trabalhoId, String resumoMd, String kpisStr) {
        // Não usamos mais upsert; manter compatibilidade retornando false
        return false;
    }

    public Optional<ResumoDto> findByTrabalhoIdAndVersao(long trabalhoId, String versao) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(SQL_FIND_BY_VERSAO)) {
            ps.setLong(1, trabalhoId);
            ps.setString(2, versao);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal k = rs.getBigDecimal(3);
                    String kpisStr = (k == null) ? null : k.stripTrailingZeros().toPlainString();
                    return Optional.of(new ResumoDto(
                            rs.getLong(1),
                            rs.getString(2),
                            kpisStr
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Optional.empty();
    }

    // Inserção "insert-only" versionada
    public void insertVersao(Connection con,
                             long trabalhoId, String versao,
                             String resumoMd, String kpisStr) throws SQLException {
        final String sql = """
            INSERT INTO tg_resumo (trabalho_id, versao, resumo_md, kpis)
            VALUES (?,?,?,?)
        """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setString(2, versao);
            ps.setString(3, resumoMd);

            // kpis é DECIMAL(5,2). Se vier null/vazio, grava 0.00
            BigDecimal kpis = null;
            if (kpisStr != null && !kpisStr.isBlank()) {
                try {
                    kpis = new BigDecimal(kpisStr.trim());
                } catch (NumberFormatException ignore) {
                    kpis = BigDecimal.ZERO;
                }
            } else {
                kpis = BigDecimal.ZERO;
            }
            ps.setBigDecimal(4, kpis);

            ps.executeUpdate();
        }
    }
}