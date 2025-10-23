package br.edu.fatec.api.dto;

public class PainelOrientadorRow {
    private final String orientador;
    private final String aluno;
    private final String titulo;
    private final String tema;
    private final String versaoAtual;
    private final int totalValidadas;
    private final int pendencias;
    private final String status;           // "Não avaliado", "Em andamento", "Concluído"
    private final double percentual;       // 0..100

    public PainelOrientadorRow(String orientador, String aluno, String titulo, String tema,
                               String versaoAtual, int totalValidadas, int pendencias,
                               String status, double percentual) {
        this.orientador = orientador;
        this.aluno = aluno;
        this.titulo = titulo;
        this.tema = tema;
        this.versaoAtual = versaoAtual;
        this.totalValidadas = totalValidadas;
        this.pendencias = pendencias;
        this.status = status;
        this.percentual = percentual;
    }

    public String getOrientador() { return orientador; }
    public String getAluno() { return aluno; }
    public String getTitulo() { return titulo; }
    public String getTema() { return tema; }
    public String getVersaoAtual() { return versaoAtual; }
    public int getTotalValidadas() { return totalValidadas; }
    public int getPendencias() { return pendencias; }
    public String getStatus() { return status; }
    public double getPercentual() { return percentual; }
}
