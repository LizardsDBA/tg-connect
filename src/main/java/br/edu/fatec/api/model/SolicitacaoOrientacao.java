package br.edu.fatec.api.model;

import java.time.LocalDateTime;

public class SolicitacaoOrientacao {
    private Long id;
    private Long alunoId;
    private Long orientadorId;
    private StatusSolicitacao status;
    private String justificativa;
    private LocalDateTime dataSolicitacao;
    private LocalDateTime dataResposta;
    private String nomeAluno;
    private String nomeOrientador;

    // --- NOVOS CAMPOS ---
    private String tituloTg;
    private String temaTg;

    public enum StatusSolicitacao {
        PENDENTE, APROVADA, RECUSADA
    }

    public SolicitacaoOrientacao() {
        this.status = StatusSolicitacao.PENDENTE;
        this.dataSolicitacao = LocalDateTime.now();
    }

    // Getters e Setters padrão
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAlunoId() { return alunoId; }
    public void setAlunoId(Long alunoId) { this.alunoId = alunoId; }

    public Long getOrientadorId() { return orientadorId; }
    public void setOrientadorId(Long orientadorId) { this.orientadorId = orientadorId; }

    public StatusSolicitacao getStatus() { return status; }
    public void setStatus(StatusSolicitacao status) { this.status = status; }

    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }

    public LocalDateTime getDataSolicitacao() { return dataSolicitacao; }
    public void setDataSolicitacao(LocalDateTime dataSolicitacao) { this.dataSolicitacao = dataSolicitacao; }

    public LocalDateTime getDataResposta() { return dataResposta; }
    public void setDataResposta(LocalDateTime dataResposta) { this.dataResposta = dataResposta; }

    public String getNomeAluno() { return nomeAluno; }
    public void setNomeAluno(String nomeAluno) { this.nomeAluno = nomeAluno; }

    public String getNomeOrientador() { return nomeOrientador; }
    public void setNomeOrientador(String nomeOrientador) { this.nomeOrientador = nomeOrientador; }

    // --- NOVOS GETTERS E SETTERS ---
    public String getTituloTg() { return tituloTg; }
    public void setTituloTg(String tituloTg) { this.tituloTg = tituloTg; }

    public String getTemaTg() { return temaTg; }
    public void setTemaTg(String temaTg) { this.temaTg = temaTg; }
}