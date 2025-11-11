package br.edu.fatec.api.dto;

import java.time.LocalDateTime;

/**
 * DTO específico para a tela de Andamento Geral do Coordenador.
 * É uma cópia do PainelOrientadorRow, mas inclui a data de atualização.
 */
public class AndamentoRow {
    private final String orientador;
    private final String aluno;
    private final String titulo;
    private final String tema;
    private final String versaoAtual;
    private final int totalValidadas;
    private final int pendencias;
    private final String status;
    private final double percentual;
    private final LocalDateTime atualizadoEm; // <-- A NOVA INFORMAÇÃO

    public AndamentoRow(String orientador, String aluno, String titulo, String tema,
                        String versaoAtual, int totalValidadas, int pendencias,
                        String status, double percentual, LocalDateTime atualizadoEm) {
        this.orientador = orientador;
        this.aluno = aluno;
        this.titulo = titulo;
        this.tema = tema;
        this.versaoAtual = versaoAtual;
        this.totalValidadas = totalValidadas;
        this.pendencias = pendencias;
        this.status = status;
        this.percentual = percentual;
        this.atualizadoEm = atualizadoEm; // <-- A NOVA INFORMAÇÃO
    }

    // Getters
    public String getOrientador() { return orientador; }
    public String getAluno() { return aluno; }
    public String getTitulo() { return titulo; }
    public String getTema() { return tema; }
    public String getVersaoAtual() { return versaoAtual; }
    public int getTotalValidadas() { return totalValidadas; }
    public int getPendencias() { return pendencias; }
    public String getStatus() { return status; }
    public double getPercentual() { return percentual; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; } // <-- A NOVA INFORMAÇÃO
}