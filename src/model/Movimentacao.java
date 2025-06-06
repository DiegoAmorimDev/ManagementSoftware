package model;

import java.sql.Timestamp;

public class Movimentacao {
    private int idMovimentacao;
    private Timestamp dataHora;
    private int codigoProduto;
    private int quantidade;
    private TipoMovimentacao tipoMovimentacao;
    private int idUsuario;

    // Enum para o tipo de movimentação
    public enum TipoMovimentacao {
        ENTRADA, SAIDA
    }

    // Construtores
    public Movimentacao() {}

    public Movimentacao(int idMovimentacao, Timestamp dataHora, int codigoProduto, int quantidade, TipoMovimentacao tipoMovimentacao, int idUsuario) {
        this.idMovimentacao = idMovimentacao;
        this.dataHora = dataHora;
        this.codigoProduto = codigoProduto;
        this.quantidade = quantidade;
        this.tipoMovimentacao = tipoMovimentacao;
        this.idUsuario = idUsuario;
    }

    // Getters e Setters
    public int getIdMovimentacao() {
        return idMovimentacao;
    }

    public void setIdMovimentacao(int idMovimentacao) {
        this.idMovimentacao = idMovimentacao;
    }

    public Timestamp getDataHora() {
        return dataHora;
    }

    public void setDataHora(Timestamp dataHora) {
        this.dataHora = dataHora;
    }

    public int getCodigoProduto() {
        return codigoProduto;
    }

    public void setCodigoProduto(int codigoProduto) {
        this.codigoProduto = codigoProduto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public TipoMovimentacao getTipoMovimentacao() {
        return tipoMovimentacao;
    }

    public void setTipoMovimentacao(TipoMovimentacao tipoMovimentacao) {
        this.tipoMovimentacao = tipoMovimentacao;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    @Override
    public String toString() {
        return "Movimentacao{" +
                "idMovimentacao=" + idMovimentacao +
                ", dataHora=" + dataHora +
                ", codigoProduto=" + codigoProduto +
                ", quantidade=" + quantidade +
                ", tipoMovimentacao=" + tipoMovimentacao +
                ", idUsuario=" + idUsuario +
                '}';
    }
}

