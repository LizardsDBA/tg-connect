package br.edu.fatec.api.service;

import br.edu.fatec.api.dao.*;
import java.util.Optional;

public class DashboardAlunoService {

    private final TrabalhosGraduacaoDao tgDao;
    private final KpiDao kpiDao;

    public DashboardAlunoService() {
        this.tgDao = new JdbcTrabalhosGraduacaoDao();
        this.kpiDao = new JdbcKpiDao();
    }

    public record Kpis(double percentual, int pendencias, String ultimaVersao) {}

    public Optional<Kpis> carregarKpis(long alunoId) {
        return tgDao.findIdByAlunoId(alunoId).map(trabalhoId -> {
            double perc = kpiDao.findPercentualConclusaoByTrabalhoId(trabalhoId);
            int pend = kpiDao.countPendenciasAluno(alunoId);
            String ult = kpiDao.findUltimaVersaoByTrabalhoId(trabalhoId).orElse("—");
            return new Kpis(perc, pend, ult);
        });
    }
}