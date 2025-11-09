package br.edu.fatec.api.service;

import br.edu.fatec.api.config.Database;
import br.edu.fatec.api.dao.*;
import br.edu.fatec.api.nav.Session;
import br.edu.fatec.api.model.auth.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public class EditorAlunoService {

    private static final boolean VALIDAR_CAMPOS_OBRIGATORIOS = false;
    private final JdbcTrabalhosGraduacaoDao tgDao;
    private final JdbcTgApresentacaoDao apDao;
    private final JdbcTgSecaoDao secaoDao;
    private final JdbcTgResumoDao resumoDao;
    private final JdbcVersoesTrabalhoDao versoesDao;

    public EditorAlunoService() {
        this.tgDao = new JdbcTrabalhosGraduacaoDao();
        this.apDao = new JdbcTgApresentacaoDao();
        this.secaoDao = new JdbcTgSecaoDao();
        this.resumoDao = new JdbcTgResumoDao();
        this.versoesDao = new JdbcVersoesTrabalhoDao();
    }

    public long resolveTrabalhoIdDoAlunoLogado() {
        User u = Session.getUser();
        if (u == null) throw new IllegalStateException("Usuário não logado.");
        final String sql = "SELECT id FROM trabalhos_graduacao WHERE aluno_id=? LIMIT 1";
        try (var con = Database.get(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, u.getId());
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao resolver trabalho do aluno: " + e.getMessage(), e);
        }
        throw new IllegalStateException("Trabalho de graduação não encontrado para o aluno.");
    }

    private String proximaVersaoFromDb(long trabalhoId) {
        String sql =
                "WITH v AS ( " +
                        "  SELECT versao FROM versoes_trabalho WHERE trabalho_id=? " +
                        "  UNION ALL " +
                        "  SELECT versao FROM tg_apresentacao WHERE trabalho_id=? " +
                        "  UNION ALL " +
                        "  SELECT versao FROM tg_resumo WHERE trabalho_id=? " +
                        "  UNION ALL " +
                        "  SELECT versao FROM tg_secao WHERE trabalho_id=? " +
                        ") " +
                        "SELECT CASE " +
                        "  WHEN COUNT(*)=0 THEN 'v1' " +
                        "  WHEN SUM(versao='v1')>0 OR SUM(versao LIKE 'v1.%')>0 THEN " +
                        "       CONCAT('v1.', COALESCE(MAX(CASE WHEN versao LIKE 'v1.%' " +
                        "              THEN CAST(SUBSTRING(versao,4) AS UNSIGNED) END), 0) + 1) " +
                        "  ELSE 'v1' " +
                        "END AS prox FROM v";

        try (var con = Database.get(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            ps.setLong(2, trabalhoId);
            ps.setLong(3, trabalhoId);
            ps.setLong(4, trabalhoId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (Exception e) { return "v1"; }
        return "v1";
    }

    public static final class DadosEditor {
        // ... (Seu DTO original está OK) ...
        public String infoPessoais, historicoAcad, motivacao, historicoProf, contatos, conhecimentos, consideracoesFinais;
        public String api1Empresa, api1Problema, api1Solucao, api1Repo, api1Tecnologias, api1Contrib, api1Hard, api1Soft;
        public String api2Empresa, api2Problema, api2Solucao, api2Repo, api2Tecnologias, api2Contrib, api2Hard, api2Soft;
        public String api3Empresa, api3Problema, api3Solucao, api3Repo, api3Tecnologias, api3Contrib, api3Hard, api3Soft;
        public String api4Empresa, api4Problema, api4Solucao, api4Repo, api4Tecnologias, api4Contrib, api4Hard, api4Soft;
        public String api5Empresa, api5Problema, api5Solucao, api5Repo, api5Tecnologias, api5Contrib, api5Hard, api5Soft;
        public String api6Empresa, api6Problema, api6Solucao, api6Repo, api6Tecnologias, api6Contrib, api6Hard, api6Soft;
        public String resumoMd;
        public String mdCompleto;
    }

    private void validarObrigatorios(DadosEditor d) {
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    public String salvarTudo(long trabalhoId, DadosEditor d) throws SQLException {
        Objects.requireNonNull(d, "Dados do editor são obrigatórios.");
        validarObrigatorios(d);

        // --- NOVO: VERIFICAÇÃO DE STATUS ---
        // O aluno só pode salvar se o status for 'EM_ANDAMENTO' ou 'REPROVADO'
        TrabalhoInfo info = fetchTrabalhoInfo(trabalhoId).orElseThrow();
        if (info.status().equals("ENTREGUE") || info.status().equals("APROVADO")) {
            throw new IllegalStateException("Este TG está '" + info.status() + "' e não pode ser editado. Aguarde a devolutiva do orientador.");
        }
        // ------------------------------------

        String novaVersao = proximaVersaoFromDb(trabalhoId);

        try (Connection con = Database.get()) {
            con.setAutoCommit(false);

            // ... (Seu código de insertVersao para apDao, secaoDao, resumoDao, versoesDao está OK) ...
            apDao.insertVersao(con, trabalhoId, novaVersao,
                    safe(d.infoPessoais), safe(d.historicoAcad), safe(d.motivacao),
                    safe(d.historicoProf), safe(d.contatos), safe(d.conhecimentos),
                    safe(d.consideracoesFinais));
            secaoDao.insertVersao(con, trabalhoId, novaVersao, 1,
                    d.api1Empresa, d.api1Problema, d.api1Solucao, d.api1Repo,
                    d.api1Tecnologias, d.api1Contrib, d.api1Hard, d.api1Soft, null);
            secaoDao.insertVersao(con, trabalhoId, novaVersao, 2,
                    d.api2Empresa, d.api2Problema, d.api2Solucao, d.api2Repo,
                    d.api2Tecnologias, d.api2Contrib, d.api2Hard, d.api2Soft, null);
            secaoDao.insertVersao(con, trabalhoId, novaVersao, 3,
                    d.api3Empresa, d.api3Problema, d.api3Solucao, d.api3Repo,
                    d.api3Tecnologias, d.api3Contrib, d.api3Hard, d.api3Soft, null);
            secaoDao.insertVersao(con, trabalhoId, novaVersao, 4,
                    d.api4Empresa, d.api4Problema, d.api4Solucao, d.api4Repo,
                    d.api4Tecnologias, d.api4Contrib, d.api4Hard, d.api4Soft, null);
            secaoDao.insertVersao(con, trabalhoId, novaVersao, 5,
                    d.api5Empresa, d.api5Problema, d.api5Solucao, d.api5Repo,
                    d.api5Tecnologias, d.api5Contrib, d.api5Hard, d.api5Soft, null);
            secaoDao.insertVersao(con, trabalhoId, novaVersao, 6,
                    d.api6Empresa, d.api6Problema, d.api6Solucao, d.api6Repo,
                    d.api6Tecnologias, d.api6Contrib, d.api6Hard, d.api6Soft, null);
            resumoDao.insertVersao(con, trabalhoId, novaVersao, d.resumoMd);
            versoesDao.insertCompleto(con, trabalhoId, "COMPLETO", novaVersao, d.mdCompleto, null);

            // 5) Copiar flags (está OK)
            copiarFlagsValidacaoDaUltimaVersaoParaNova(con, trabalhoId, novaVersao);
            // 6) Atualiza versao_atual (está OK)
            tgDao.updateVersaoAtual(con, trabalhoId, novaVersao);

            // 7) NOVO: Garante que o status volte para EM_ANDAMENTO após salvar
            tgDao.updateStatus(con, trabalhoId, "EM_ANDAMENTO"); // <-- LINHA CORRIGIDA

            con.commit();

            con.commit();
            return novaVersao;
        } catch (SQLException e) {
            throw new RuntimeException("Falha ao salvar tudo (rollback executado): " + e.getMessage(), e);
        }
    }

    public void solicitarRevisao(long trabalhoId) throws SQLException {
        // Verifica o status atual
        TrabalhoInfo info = fetchTrabalhoInfo(trabalhoId).orElse(null);
        if (info == null) {
            throw new IllegalStateException("Trabalho não encontrado.");
        }
        if (!info.status().equals("EM_ANDAMENTO") && !info.status().equals("REPROVADO")) {
            throw new IllegalStateException("O trabalho já foi entregue ou está aprovado.");
        }

        try (Connection con = Database.get()) {
            // Não precisa de transação, é um único update
            tgDao.updateStatus(con, trabalhoId, "ENTREGUE");
        }
        // --- FIM DA CORREÇÃO ---
    }

    private String safe(String s){ return s == null ? "" : s.trim(); }

    // ====== Leitura para preencher a UI (ATUALIZADO) ======

    /** DTO ATUALIZADO: Agora inclui o status do fluxo */
    public static final class DadosEditorLeitura {
        public String status; // NOVO (EM_ANDAMENTO, ENTREGUE, etc.)
        // Aba 1
        public String infoPessoais, historicoAcad, motivacao, historicoProf, contatos, conhecimentos, consideracoes;
        public int infoPessoaisStatus, historicoAcadStatus, motivacaoStatus, historicoProfStatus, contatosStatus, conhecimentosStatus, consideracoesStatus;
        // ... (Todos os outros campos de API e Resumo que já corrigimos) ...
        public String api1Empresa, api1Problema, api1Solucao, api1Repo, api1Tecnologias, api1Contrib, api1Hard, api1Soft;
        public int api1EmpresaStatus, api1ProblemaStatus, api1SolucaoStatus, api1RepoStatus, api1TecnologiasStatus, api1ContribStatus, api1HardStatus, api1SoftStatus;
        public String api2Empresa, api2Problema, api2Solucao, api2Repo, api2Tecnologias, api2Contrib, api2Hard, api2Soft;
        public int api2EmpresaStatus, api2ProblemaStatus, api2SolucaoStatus, api2RepoStatus, api2TecnologiasStatus, api2ContribStatus, api2HardStatus, api2SoftStatus;
        public String api3Empresa, api3Problema, api3Solucao, api3Repo, api3Tecnologias, api3Contrib, api3Hard, api3Soft;
        public int api3EmpresaStatus, api3ProblemaStatus, api3SolucaoStatus, api3RepoStatus, api3TecnologiasStatus, api3ContribStatus, api3HardStatus, api3SoftStatus;
        public String api4Empresa, api4Problema, api4Solucao, api4Repo, api4Tecnologias, api4Contrib, api4Hard, api4Soft;
        public int api4EmpresaStatus, api4ProblemaStatus, api4SolucaoStatus, api4RepoStatus, api4TecnologiasStatus, api4ContribStatus, api4HardStatus, api4SoftStatus;
        public String api5Empresa, api5Problema, api5Solucao, api5Repo, api5Tecnologias, api5Contrib, api5Hard, api5Soft;
        public int api5EmpresaStatus, api5ProblemaStatus, api5SolucaoStatus, api5RepoStatus, api5TecnologiasStatus, api5ContribStatus, api5HardStatus, api5SoftStatus;
        public String api6Empresa, api6Problema, api6Solucao, api6Repo, api6Tecnologias, api6Contrib, api6Hard, api6Soft;
        public int api6EmpresaStatus, api6ProblemaStatus, api6SolucaoStatus, api6RepoStatus, api6TecnologiasStatus, api6ContribStatus, api6HardStatus, api6SoftStatus;
        public String resumoMd;
        public int resumoMdStatus;
    }

    /** DTO ATUALIZADO: Retorna a versão E o status do fluxo */
    public record TrabalhoInfo(String versao, String status) {}

    /** ATUALIZADO: Busca a versão E o status do fluxo */
    public Optional<TrabalhoInfo> fetchTrabalhoInfo(long trabalhoId) {
        final String sql = "SELECT versao_atual, status FROM trabalhos_graduacao WHERE id=? LIMIT 1";
        try (var con = Database.get(); var ps = con.prepareStatement(sql)) {
            ps.setLong(1, trabalhoId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new TrabalhoInfo(
                            rs.getString("versao_atual"),
                            rs.getString("status")
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return Optional.empty();
    }


    /** ATUALIZADO: Agora preenche o campo de status */
    public Optional<DadosEditorLeitura> carregarTudo(long trabalhoId) {
        // ATUALIZADO: Busca a info (versão e status)
        Optional<TrabalhoInfo> infoOpt = fetchTrabalhoInfo(trabalhoId);
        if (infoOpt.isEmpty()) {
            return Optional.empty();
        }
        TrabalhoInfo info = infoOpt.get();
        String versao = info.versao();

        if (versao == null || versao.isBlank()) return Optional.empty();

        DadosEditorLeitura d = new DadosEditorLeitura();
        d.status = info.status(); // <-- POPULA O STATUS

        apDao.findByTrabalhoIdAndVersao(trabalhoId, versao).ifPresent(a -> {
            d.infoPessoais  = nz(a.nomeCompleto);
            d.historicoAcad = nz(a.historicoAcad);
            d.motivacao     = nz(a.motivacao);
            d.historicoProf = nz(a.historicoProf);
            d.contatos      = nz(a.contatosEmailOuLivre);
            d.conhecimentos = nz(a.conhecimentos);
            d.consideracoes = nz(a.consideracoes);
            d.infoPessoaisStatus = a.nomeCompletoStatus;
            d.historicoAcadStatus = a.historicoAcadStatus;
            d.motivacaoStatus = a.motivacaoStatus;
            d.historicoProfStatus = a.historicoProfStatus;
            d.contatosStatus = a.contatosEmailStatus;
            d.conhecimentosStatus = a.conhecimentosStatus;
            d.consideracoesStatus = a.consideracoesFinaisStatus;
        });
        var secoes = secaoDao.findByTrabalhoIdAndVersao(trabalhoId, versao);
        for (var s : secoes) {
            switch (s.semestreApi) {
                case 1 -> {
                    d.api1Empresa=s.empresa; d.api1Problema=s.problema; d.api1Solucao=s.solucao; d.api1Repo=s.repo;
                    d.api1Tecnologias=s.tecnologias; d.api1Contrib=s.contrib; d.api1Hard=s.hard; d.api1Soft=s.soft;
                    d.api1EmpresaStatus=s.empresaStatus; d.api1ProblemaStatus=s.problemaStatus; d.api1SolucaoStatus=s.solucaoStatus; d.api1RepoStatus=s.repoStatus;
                    d.api1TecnologiasStatus=s.tecnologiasStatus; d.api1ContribStatus=s.contribStatus; d.api1HardStatus=s.hardStatus; d.api1SoftStatus=s.softStatus;
                }
                case 2 -> {
                    d.api2Empresa=s.empresa; d.api2Problema=s.problema; d.api2Solucao=s.solucao; d.api2Repo=s.repo;
                    d.api2Tecnologias=s.tecnologias; d.api2Contrib=s.contrib; d.api2Hard=s.hard; d.api2Soft=s.soft;
                    d.api2EmpresaStatus=s.empresaStatus; d.api2ProblemaStatus=s.problemaStatus; d.api2SolucaoStatus=s.solucaoStatus; d.api2RepoStatus=s.repoStatus;
                    d.api2TecnologiasStatus=s.tecnologiasStatus; d.api2ContribStatus=s.contribStatus; d.api2HardStatus=s.hardStatus; d.api2SoftStatus=s.softStatus;
                }
                // ... (cases 3, 4, 5, 6) ...
                case 3 -> {
                    d.api3Empresa=s.empresa; d.api3Problema=s.problema; d.api3Solucao=s.solucao; d.api3Repo=s.repo;
                    d.api3Tecnologias=s.tecnologias; d.api3Contrib=s.contrib; d.api3Hard=s.hard; d.api3Soft=s.soft;
                    d.api3EmpresaStatus=s.empresaStatus; d.api3ProblemaStatus=s.problemaStatus; d.api3SolucaoStatus=s.solucaoStatus; d.api3RepoStatus=s.repoStatus;
                    d.api3TecnologiasStatus=s.tecnologiasStatus; d.api3ContribStatus=s.contribStatus; d.api3HardStatus=s.hardStatus; d.api3SoftStatus=s.softStatus;
                }
                case 4 -> {
                    d.api4Empresa=s.empresa; d.api4Problema=s.problema; d.api4Solucao=s.solucao; d.api4Repo=s.repo;
                    d.api4Tecnologias=s.tecnologias; d.api4Contrib=s.contrib; d.api4Hard=s.hard; d.api4Soft=s.soft;
                    d.api4EmpresaStatus=s.empresaStatus; d.api4ProblemaStatus=s.problemaStatus; d.api4SolucaoStatus=s.solucaoStatus; d.api4RepoStatus=s.repoStatus;
                    d.api4TecnologiasStatus=s.tecnologiasStatus; d.api4ContribStatus=s.contribStatus; d.api4HardStatus=s.hardStatus; d.api4SoftStatus=s.softStatus;
                }
                case 5 -> {
                    d.api5Empresa=s.empresa; d.api5Problema=s.problema; d.api5Solucao=s.solucao; d.api5Repo=s.repo;
                    d.api5Tecnologias=s.tecnologias; d.api5Contrib=s.contrib; d.api5Hard=s.hard; d.api5Soft=s.soft;
                    d.api5EmpresaStatus=s.empresaStatus; d.api5ProblemaStatus=s.problemaStatus; d.api5SolucaoStatus=s.solucaoStatus; d.api5RepoStatus=s.repoStatus;
                    d.api5TecnologiasStatus=s.tecnologiasStatus; d.api5ContribStatus=s.contribStatus; d.api5HardStatus=s.hardStatus; d.api5SoftStatus=s.softStatus;
                }
                case 6 -> {
                    d.api6Empresa=s.empresa; d.api6Problema=s.problema; d.api6Solucao=s.solucao; d.api6Repo=s.repo;
                    d.api6Tecnologias=s.tecnologias; d.api6Contrib=s.contrib; d.api6Hard=s.hard; d.api6Soft=s.soft;
                    d.api6EmpresaStatus=s.empresaStatus; d.api6ProblemaStatus=s.problemaStatus; d.api6SolucaoStatus=s.solucaoStatus; d.api6RepoStatus=s.repoStatus;
                    d.api6TecnologiasStatus=s.tecnologiasStatus; d.api6ContribStatus=s.contribStatus; d.api6HardStatus=s.hardStatus; d.api6SoftStatus=s.softStatus;
                }
            }
        }
        resumoDao.findByTrabalhoIdAndVersao(trabalhoId, versao)
                .ifPresent(r -> {
                    d.resumoMd = nz(r.resumoMd());
                    d.resumoMdStatus = r.versaoValidada();
                });
        return Optional.of(d);
    }

    private String nz(String s){ return s == null ? "" : s; }
    public boolean isAbaValidada(long trabalhoId, int abaNumero) throws SQLException {
        String versao = fetchTrabalhoInfo(trabalhoId).map(TrabalhoInfo::versao).orElse(null);
        if (versao == null || versao.isBlank()) return false;
        if (abaNumero == 1) { return apDao.getApresentacaoValidada(trabalhoId, versao) == 1; }
        if (abaNumero == 9) { return apDao.getConsideracaoValidada(trabalhoId, versao) == 1; }
        if (abaNumero == 8) { return resumoDao.getValidacaoResumo(trabalhoId, versao) == 1; }
        if (abaNumero >= 2 && abaNumero <= 7) {
            int semestre = abaNumero - 1;
            Integer v = secaoDao.getValidacaoSecao(trabalhoId, versao, semestre);
            return v != null && v == 1;
        }
        return false;
    }
    private void copiarFlagsValidacaoDaUltimaVersaoParaNova(Connection con, long trabalhoId, String novaVersao) throws SQLException {
        apDao.copyValidacaoFromUltimaVersao(con, trabalhoId, novaVersao);
        resumoDao.copyValidacaoFromUltimaVersao(con, trabalhoId, novaVersao);
        secaoDao.copyValidacaoFromUltimaVersao(con, trabalhoId, novaVersao);
    }
}