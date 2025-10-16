package br.edu.fatec.api.dao;

import java.util.Optional;

public interface KpiDao {
    int countPendenciasAluno(long alunoId);
    Optional<String> findUltimaVersaoByTrabalhoId(long trabalhoId);
    double findPercentualConclusaoByTrabalhoId(long trabalhoId);
}
