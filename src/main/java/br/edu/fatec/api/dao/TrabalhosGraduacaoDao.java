package br.edu.fatec.api.dao;

import java.util.Optional;

public interface TrabalhosGraduacaoDao {
    Optional<Long> findIdByAlunoId(long alunoId);
}
