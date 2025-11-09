package br.edu.fatec.api.service;

import br.edu.fatec.api.dao.JdbcDashboardOrientadorDao;
import java.util.Optional;

public class DashboardOrientadorService {

    private final JdbcDashboardOrientadorDao kpiDao;

    // DTO (Record) público que o Controller receberá
    public record Kpis(int orientandos, int pendencias, int concluidos, int reprovacoes) {}

    public DashboardOrientadorService() {
        this.kpiDao = new JdbcDashboardOrientadorDao();
    }

    /**
     * Carrega os 4 KPIs para o orientador logado.
     */
    public Optional<Kpis> carregarKpis(long orientadorId) {
        return kpiDao.getKpis(orientadorId)
                .map(dto -> new Kpis(
                        dto.totalOrientandos(),
                        dto.totalPendencias(),
                        dto.totalConcluidos(),
                        dto.totalComReprovacoes()
                ));
    }
}