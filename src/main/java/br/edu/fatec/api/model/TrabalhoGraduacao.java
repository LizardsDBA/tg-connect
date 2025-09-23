package br.edu.fatec.api.model;

import java.time.LocalDateTime;

/**
 * Modelo para Trabalho de Graduação
 */
public class TrabalhoGraduacao {
    private Long id;
    private String titulo;
    private String tema;
    private String conteudo;
    private String versao;
    private StatusTG status;
    private double percentualConclusao;
    private Long alunoId;
    private Long orientadorId;
    private LocalDateTime dataInicio;
    private LocalDateTime dataUltimaModificacao;
    private LocalDateTime dataEntrega;
    
    public enum StatusTG {
        INICIADO, EM_ANDAMENTO, AGUARDANDO_REVISAO, 
        EM_REVISAO, APROVADO, REPROVADO, CONCLUIDO
    }
    
    // Construtores
    public TrabalhoGraduacao() {
        this.dataInicio = LocalDateTime.now();
        this.dataUltimaModificacao = LocalDateTime.now();
        this.status = StatusTG.INICIADO;
        this.percentualConclusao = 0.0;
        this.versao = "1.0";
    }
    
    public TrabalhoGraduacao(String titulo, String tema, Long alunoId, Long orientadorId) {
        this();
        this.titulo = titulo;
        this.tema = tema;
        this.alunoId = alunoId;
        this.orientadorId = orientadorId;
    }
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    
    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema; }
    
    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { 
        this.conteudo = conteudo;
        this.dataUltimaModificacao = LocalDateTime.now();
    }
    
    public String getVersao() { return versao; }
    public void setVersao(String versao) { this.versao = versao; }
    
    public StatusTG getStatus() { return status; }
    public void setStatus(StatusTG status) { this.status = status; }
    
    public double getPercentualConclusao() { return percentualConclusao; }
    public void setPercentualConclusao(double percentualConclusao) { 
        this.percentualConclusao = Math.max(0, Math.min(100, percentualConclusao));
    }
    
    public Long getAlunoId() { return alunoId; }
    public void setAlunoId(Long alunoId) { this.alunoId = alunoId; }
    
    public Long getOrientadorId() { return orientadorId; }
    public void setOrientadorId(Long orientadorId) { this.orientadorId = orientadorId; }
    
    public LocalDateTime getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDateTime dataInicio) { this.dataInicio = dataInicio; }
    
    public LocalDateTime getDataUltimaModificacao() { return dataUltimaModificacao; }
    public void setDataUltimaModificacao(LocalDateTime dataUltimaModificacao) { 
        this.dataUltimaModificacao = dataUltimaModificacao; 
    }
    
    public LocalDateTime getDataEntrega() { return dataEntrega; }
    public void setDataEntrega(LocalDateTime dataEntrega) { this.dataEntrega = dataEntrega; }
    
    @Override
    public String toString() {
        return "TrabalhoGraduacao{" +
                "id=" + id +
                ", titulo='" + titulo + '\'' +
                ", versao='" + versao + '\'' +
                ", status=" + status +
                ", percentualConclusao=" + percentualConclusao +
                '}';
    }
}
