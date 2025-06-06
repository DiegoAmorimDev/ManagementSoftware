package model;

public class Produto {
    private int codigoProduto;
    private String nome;
    private String descricao;
    private String procedencia;
    private int quantidadeAtual;
    private boolean isActive;

    // Construtores
    public Produto() {}

    public Produto(int codigoProduto, String nome, String descricao, String procedencia, int quantidadeAtual, boolean isActive) {
        this.codigoProduto = codigoProduto;
        this.nome = nome;
        this.descricao = descricao;
        this.procedencia = procedencia;
        this.quantidadeAtual = quantidadeAtual;
        this.isActive = isActive;
    }

    // Getters e Setters
    public int getCodigoProduto() {
        return codigoProduto;
    }

    public void setCodigoProduto(int codigoProduto) {
        this.codigoProduto = codigoProduto;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getProcedencia() {
        return procedencia;
    }

    public void setProcedencia(String procedencia) {
        this.procedencia = procedencia;
    }

    public int getQuantidadeAtual() {
        return quantidadeAtual;
    }

    public void setQuantidadeAtual(int quantidadeAtual) {
        this.quantidadeAtual = quantidadeAtual;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return "Produto{" +
                "codigoProduto=" + codigoProduto +
                ", nome=\'" + nome + "\'" +
                ", quantidadeAtual=" + quantidadeAtual +
                ", isActive=" + isActive +
                '}';
    }
}

