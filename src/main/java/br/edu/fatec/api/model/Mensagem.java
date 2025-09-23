package br.edu.fatec.api.model;

import java.time.LocalDateTime;

/**
 * Modelo para mensagens do sistema de chat
 */
public class Mensagem {
    private Long id;
    private String conteudo;
    private Long remetenteId;
    private Long destinatarioId;
    private Long trabalhoId;
    private String secao; // seção do trabalho relacionada
    private TipoMensagem tipo;
    private boolean lida;
    private LocalDateTime dataEnvio;
    
    public enum TipoMensagem {
        COMENTARIO, FEEDBACK, SOLICITACAO, APROVACAO, REJEICAO
    }
    
    // Construtores
    public Mensagem() {
        this.dataEnvio = LocalDateTime.now();
        this.lida = false;
    }
    
    public Mensagem(String conteudo, Long remetenteId, Long destinatarioId, Long trabalhoId) {
        this();
        this.conteudo = conteudo;
        this.remetenteId = remetenteId;
        this.destinatarioId = destinatarioId;
        this.trabalhoId = trabalhoId;
        this.tipo = TipoMensagem.COMENTARIO;
    }
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }
    
    public Long getRemetenteId() { return remetenteId; }
    public void setRemetenteId(Long remetenteId) { this.remetenteId = remetenteId; }
    
    public Long getDestinatarioId() { return destinatarioId; }
    public void setDestinatarioId(Long destinatarioId) { this.destinatarioId = destinatarioId; }
    
    public Long getTrabalhoId() { return trabalhoId; }
    public void setTrabalhoId(Long trabalhoId) { this.trabalhoId = trabalhoId; }
    
    public String getSecao() { return secao; }
    public void setSecao(String secao) { this.secao = secao; }
    
    public TipoMensagem getTipo() { return tipo; }
    public void setTipo(TipoMensagem tipo) { this.tipo = tipo; }
    
    public boolean isLida() { return lida; }
    public void setLida(boolean lida) { this.lida = lida; }
    
    public LocalDateTime getDataEnvio() { return dataEnvio; }
    public void setDataEnvio(LocalDateTime dataEnvio) { this.dataEnvio = dataEnvio; }
    
    @Override
    public String toString() {
        return "Mensagem{" +
                "id=" + id +
                ", remetenteId=" + remetenteId +
                ", destinatarioId=" + destinatarioId +
                ", tipo=" + tipo +
                ", lida=" + lida +
                ", dataEnvio=" + dataEnvio +
                '}';
    }
}
