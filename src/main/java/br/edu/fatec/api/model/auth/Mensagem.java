package br.edu.fatec.api.model;

import java.time.LocalDateTime;

public class Mensagem {
    private Long id;
    private Long trabalhoId;
    private Long remetenteId;
    private Long destinatarioId;
    private String tipo; // "TEXTO" ou "SISTEMA"
    private String conteudo;
    private boolean lida;
    private LocalDateTime createdAt;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTrabalhoId() { return trabalhoId; }
    public void setTrabalhoId(Long trabalhoId) { this.trabalhoId = trabalhoId; }
    public Long getRemetenteId() { return remetenteId; }
    public void setRemetenteId(Long remetenteId) { this.remetenteId = remetenteId; }
    public Long getDestinatarioId() { return destinatarioId; }
    public void setDestinatarioId(Long destinatarioId) { this.destinatarioId = destinatarioId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }
    public boolean isLida() { return lida; }
    public void setLida(boolean lida) { this.lida = lida; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}