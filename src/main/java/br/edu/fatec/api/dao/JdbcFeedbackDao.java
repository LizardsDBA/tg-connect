package br.edu.fatec.api.dao;

import br.edu.fatec.api.config.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JdbcFeedbackDao {

    // ====== PARTES do TG ======
    public enum Parte { APRESENTACAO, API1, API2, API3, API4, API5, API6, RESUMO, FINAIS }

    // ====== DTOs ======
    public record OrientandoDTO(Long alunoId, String nome, Long trabalhoId) {}
    public record ConteudoParteDTO(Parte parte, String versao, String markdown, Boolean concluida) {}
    public record StatusParteDTO(Parte parte, Boolean concluida) {}

    // ====== API Pública ======

    /** Lista orientandos ativos do professor. */
    public List<OrientandoDTO> listarOrientandos(Long orientadorId) throws SQLException {
        String sql = """
            SELECT u.id AS aluno_id, u.nome, t.id AS trabalho_id
              FROM usuarios u
              JOIN orientacoes o ON o.aluno_id = u.id AND o.ativo = TRUE
              LEFT JOIN trabalhos_graduacao t ON t.aluno_id = u.id
             WHERE o.orientador_id = ? AND u.ativo = TRUE
             ORDER BY u.nome
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, orientadorId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrientandoDTO> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new OrientandoDTO(
                            rs.getLong("aluno_id"),
                            rs.getString("nome"),
                            rs.getObject("trabalho_id") == null ? null : rs.getLong("trabalho_id")
                    ));
                }
                return out;
            }
        }
    }

    /** Retorna trabalho_id pelo aluno_id. */
    public Long obterTrabalhoIdPorAluno(Long alunoId) throws SQLException {
        String sql = "SELECT id FROM trabalhos_graduacao WHERE aluno_id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, alunoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("Trabalho de Graduação não encontrado para o aluno " + alunoId);
    }

    /** Carrega última versão (heurística textual) da parte solicitada. */
    public ConteudoParteDTO carregarUltimaVersao(Long trabalhoId, Parte parte) throws SQLException {
        return switch (parte) {
            case APRESENTACAO -> carregarApresentacaoUltima(trabalhoId);
            case RESUMO       -> carregarResumoUltimo(trabalhoId);
            case FINAIS       -> carregarFinaisUltimo(trabalhoId);
            default           -> carregarApiUltima(trabalhoId, apiIndex(parte));
        };
    }

    /** Envia comentário (mensagens) – aparece na tela de Chat. */
    public Long enviarComentario(Long trabalhoId, Long orientadorId, Long alunoId, Parte parte, String versaoRef, String texto)
            throws SQLException {

        String sql = """
            INSERT INTO mensagens (trabalho_id, remetente_id, destinatario_id, secao, conteudo)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, trabalhoId);
            ps.setLong(2, orientadorId);
            ps.setLong(3, alunoId);
            ps.setString(4, secaoString(parte)); // APRESENTACAO | API1..API6 | RESUMO | FINAIS
            ps.setString(5, texto);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        return null;
    }

    /** Marca parte como concluída (flags por versão; não reabre com novas versões). */
    public boolean marcarComoConcluida(Long trabalhoId, Parte parte, String versao) throws SQLException {
        Objects.requireNonNull(versao, "versao não pode ser nula");

        return switch (parte) {
            case APRESENTACAO -> execUpdate(
                    "UPDATE tg_apresentacao SET apresentacao_versao_validada = TRUE WHERE trabalho_id = ? AND versao = ?",
                    p -> { p.setLong(1, trabalhoId); p.setString(2, versao); });

            case FINAIS -> execUpdate(
                    "UPDATE tg_apresentacao SET consideracao_versao_validada = TRUE WHERE trabalho_id = ? AND versao = ?",
                    p -> { p.setLong(1, trabalhoId); p.setString(2, versao); });

            case RESUMO -> execUpdate(
                    "UPDATE tg_resumo SET versao_validada = TRUE WHERE trabalho_id = ? AND versao = ?",
                    p -> { p.setLong(1, trabalhoId); p.setString(2, versao); });

            default -> execUpdate(
                    "UPDATE tg_secao SET versao_validada = TRUE WHERE trabalho_id = ? AND semestre_api = ? AND versao = ?",
                    p -> { p.setLong(1, trabalhoId); p.setInt(2, apiIndex(parte)); p.setString(3, versao); });
        };
    }

    // ====== Implementações por parte ======

    private ConteudoParteDTO carregarApresentacaoUltima(Long trabalhoId) throws SQLException {
        String sql = """
            SELECT versao,
                   COALESCE(nome_completo,'')            AS nome_completo,
                   COALESCE(idade, NULL)                 AS idade,
                   COALESCE(curso,'')                    AS curso,
                   COALESCE(historico_academico,'')      AS historico_academico,
                   COALESCE(motivacao_fatec,'')          AS motivacao_fatec,
                   COALESCE(historico_profissional,'')   AS historico_profissional,
                   COALESCE(contatos_email,'')           AS contatos_email,
                   COALESCE(principais_conhecimentos,'') AS principais_conhecimentos,
                   apresentacao_versao_validada          AS concluida
              FROM tg_apresentacao
             WHERE trabalho_id = ?
             ORDER BY LENGTH(versao) DESC, versao DESC
             LIMIT 1
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String md = buildApresentacaoMarkdown(rs);
                    return new ConteudoParteDTO(Parte.APRESENTACAO,
                            rs.getString("versao"),
                            md,
                            rs.getBoolean("concluida"));
                }
            }
        }
        return new ConteudoParteDTO(Parte.APRESENTACAO, null, "", false);
    }

    private ConteudoParteDTO carregarFinaisUltimo(Long trabalhoId) throws SQLException {
        String sql = """
            SELECT versao,
                   COALESCE(consideracoes_finais,'') AS markdown,
                   consideracao_versao_validada     AS concluida
              FROM tg_apresentacao
             WHERE trabalho_id = ?
             ORDER BY LENGTH(versao) DESC, versao DESC
             LIMIT 1
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ConteudoParteDTO(Parte.FINAIS,
                            rs.getString("versao"),
                            rs.getString("markdown"),
                            rs.getBoolean("concluida"));
                }
            }
        }
        return new ConteudoParteDTO(Parte.FINAIS, null, "", false);
    }

    private ConteudoParteDTO carregarResumoUltimo(Long trabalhoId) throws SQLException {
        String sql = """
            SELECT versao, resumo_md AS markdown, versao_validada AS concluida
              FROM tg_resumo
             WHERE trabalho_id = ?
             ORDER BY LENGTH(versao) DESC, versao DESC
             LIMIT 1
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ConteudoParteDTO(Parte.RESUMO,
                            rs.getString("versao"),
                            rs.getString("markdown"),
                            rs.getBoolean("concluida"));
                }
            }
        }
        return new ConteudoParteDTO(Parte.RESUMO, null, "", false);
    }

    private ConteudoParteDTO carregarApiUltima(Long trabalhoId, int semestreApi) throws SQLException {
        String sql = """
            SELECT versao,
                   COALESCE(conteudo_md,
                            CONCAT_WS('\\n',
                              CONCAT('### Problema:\\n', COALESCE(problema,'')),
                              CONCAT('### Solução (resumo):\\n', COALESCE(solucao_resumo,'')),
                              CONCAT('### Tecnologias:\\n', COALESCE(tecnologias,'')),
                              CONCAT('### Contribuições:\\n', COALESCE(contribuicoes,'')),
                              CONCAT('### Hard skills:\\n', COALESCE(hard_skills,'')),
                              CONCAT('### Soft skills:\\n', COALESCE(soft_skills,'')),
                              CONCAT('### Repositório:\\n', COALESCE(link_repositorio,''))))
                       AS markdown,
                   versao_validada AS concluida
              FROM tg_secao
             WHERE trabalho_id = ? AND semestre_api = ?
             ORDER BY LENGTH(versao) DESC, versao DESC
             LIMIT 1
        """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setInt(2, semestreApi);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ConteudoParteDTO(parteFromApi(semestreApi),
                            rs.getString("versao"),
                            rs.getString("markdown"),
                            rs.getBoolean("concluida"));
                }
            }
        }
        return new ConteudoParteDTO(parteFromApi(semestreApi), null, "", false);
    }

    // ====== Helpers ======

    private boolean execUpdate(String sql, Binder binder) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate() > 0;
        }
    }

    private String secaoString(Parte p) {
        return switch (p) {
            case APRESENTACAO -> "APRESENTACAO";
            case RESUMO       -> "RESUMO";
            case FINAIS       -> "FINAIS";
            default           -> p.name(); // API1..API6
        };
    }

    private int apiIndex(Parte p) {
        return switch (p) {
            case API1 -> 1;
            case API2 -> 2;
            case API3 -> 3;
            case API4 -> 4;
            case API5 -> 5;
            case API6 -> 6;
            default -> throw new IllegalArgumentException("Parte não é API: " + p);
        };
    }

    private Parte parteFromApi(int i) {
        return switch (i) {
            case 1 -> Parte.API1;
            case 2 -> Parte.API2;
            case 3 -> Parte.API3;
            case 4 -> Parte.API4;
            case 5 -> Parte.API5;
            case 6 -> Parte.API6;
            default -> throw new IllegalArgumentException("API inválida: " + i);
        };
    }

    private String buildApresentacaoMarkdown(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder("# Apresentação do aluno\n\n");

        String nomeCompleto = rs.getString("nome_completo");
        Integer idade = (Integer) rs.getObject("idade");
        String curso = rs.getString("curso");
        String histAcad = rs.getString("historico_academico");
        String motiv = rs.getString("motivacao_fatec");
        String histProf = rs.getString("historico_profissional");
        String contatos = rs.getString("contatos_email");
        String conhecimentos = rs.getString("principais_conhecimentos");

        if (notBlank(nomeCompleto)) sb.append("## Informações pessoais\n").append(nomeCompleto).append("\n\n");
        if (notBlank(curso)) sb.append("## Curso\n").append(curso).append("\n\n");
        if (idade != null) sb.append("**Idade:** ").append(idade).append("\n\n");
        if (notBlank(histAcad)) sb.append("## Histórico Acadêmico\n").append(histAcad).append("\n\n");
        if (notBlank(motiv)) sb.append("## Motivação FATEC\n").append(motiv).append("\n\n");
        if (notBlank(histProf)) sb.append("## Histórico Profissional\n").append(histProf).append("\n\n");
        if (notBlank(contatos)) sb.append("## Contatos\n").append(contatos).append("\n\n");
        if (notBlank(conhecimentos)) sb.append("## Principais conhecimentos\n").append(conhecimentos).append("\n\n");
        return sb.toString();
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    @FunctionalInterface
    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }
}
