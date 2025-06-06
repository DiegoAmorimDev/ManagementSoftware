package dao;

import factory.ConnectionFactory;
import model.Produto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    private Connection connection;
    private boolean connectionOwner; // Flag to indicate if this DAO instance owns the connection

    // Constructor for standalone use (creates its own connection)
    public ProdutoDAO() throws SQLException {
        this.connection = new ConnectionFactory().getConnection();
        this.connectionOwner = true; // This instance is responsible for closing the connection
    }

    // Constructor for transactional use (uses an existing connection)
    protected ProdutoDAO(Connection connection) {
        this.connection = connection;
        this.connectionOwner = false; // This instance should NOT close the connection
    }

    // Método para cadastrar novo produto (RF03, RF04)
    public Produto criarProduto(Produto produto) {
        String sql = "INSERT INTO produtos (nome, descricao, procedencia, quantidade_atual, is_active) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setString(3, produto.getProcedencia());
            stmt.setInt(4, produto.getQuantidadeAtual()); // Quantidade inicial (RF03)
            stmt.setBoolean(5, true); // Novo produto sempre ativo

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        produto.setCodigoProduto(generatedKeys.getInt(1)); // Define o código gerado (RF04)
                        produto.setActive(true);
                        return produto; // Retorna o produto com o código gerado
                    }
                }
            }
            return null;
        } catch (SQLException e) {
            System.err.println("Erro ao criar produto: " + e.getMessage());
            return null;
        }
    }

    // Método para buscar produto por código (ID)
    public Produto buscarPorCodigo(int codigo) {
        String sql = "SELECT codigo_produto, nome, descricao, procedencia, quantidade_atual, is_active FROM produtos WHERE codigo_produto = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, codigo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToProduto(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar produto por código: " + e.getMessage());
        }
        return null;
    }

    // Método para consultar produtos por código ou nome (RF06)
    public List<Produto> buscarPorCodigoOuNome(String filtro) {
        List<Produto> produtos = new ArrayList<>();
        // Busca por código se o filtro for numérico, senão busca por nome
        String sql;
        boolean buscaPorCodigo = false;
        int codigoFiltro = -1;
        try {
            codigoFiltro = Integer.parseInt(filtro);
            buscaPorCodigo = true;
            // Ajuste: Buscar mesmo se inativo ao buscar por código específico
            sql = "SELECT codigo_produto, nome, descricao, procedencia, quantidade_atual, is_active FROM produtos WHERE codigo_produto = ? ORDER BY nome";
        } catch (NumberFormatException e) {
            // Busca por nome apenas entre ativos
            sql = "SELECT codigo_produto, nome, descricao, procedencia, quantidade_atual, is_active FROM produtos WHERE nome LIKE ? AND is_active = TRUE ORDER BY nome";
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (buscaPorCodigo) {
                stmt.setInt(1, codigoFiltro);
            } else {
                stmt.setString(1, "%" + filtro + "%");
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                produtos.add(mapResultSetToProduto(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar produtos por filtro: " + e.getMessage());
        }
        return produtos;
    }

    // Método para listar todos os produtos ativos (para inventário RF13 e seletores)
    public List<Produto> listarTodosAtivos() {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT codigo_produto, nome, descricao, procedencia, quantidade_atual, is_active FROM produtos WHERE is_active = TRUE ORDER BY nome";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                produtos.add(mapResultSetToProduto(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar produtos ativos: " + e.getMessage());
        }
        return produtos;
    }

     // Método para listar TODOS os produtos (ativos e inativos, para gestão)
    public List<Produto> listarTodos() {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT codigo_produto, nome, descricao, procedencia, quantidade_atual, is_active FROM produtos ORDER BY nome";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                produtos.add(mapResultSetToProduto(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar todos os produtos: " + e.getMessage());
        }
        return produtos;
    }

    // Método para editar informações de um produto existente (RF07)
    public boolean atualizarProduto(Produto produto) {
        // Note: Este método não deve ser usado para alterar quantidade. Quantidade muda via movimentação.
        // Se a intenção for permitir ajuste manual, manter como está, mas não é o ideal.
        String sql = "UPDATE produtos SET nome = ?, descricao = ?, procedencia = ?, is_active = ? WHERE codigo_produto = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, produto.getNome());
            stmt.setString(2, produto.getDescricao());
            stmt.setString(3, produto.getProcedencia());
            stmt.setBoolean(4, produto.isActive());
            stmt.setInt(5, produto.getCodigoProduto());

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar produto: " + e.getMessage());
            return false;
        }
    }

    // Método interno para atualizar apenas a quantidade (usado por MovimentacaoDAO - RF05)
    // Este método DEVE ser chamado dentro de uma transação
    protected boolean atualizarQuantidade(int codigoProduto, int novaQuantidade, Connection conn) throws SQLException {
        // Garante que a conexão transacional seja usada
        String sql = "UPDATE produtos SET quantidade_atual = ? WHERE codigo_produto = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, novaQuantidade);
            stmt.setInt(2, codigoProduto);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                 // Lança exceção para forçar rollback se o produto não for encontrado/atualizado
                 throw new SQLException("Falha ao atualizar quantidade: Produto com código " + codigoProduto + " não encontrado ou nenhuma linha afetada.");
            }
            return affectedRows > 0;
        }
        // A exceção SQLException será propagada para ser tratada na transação
    }

    // Método para remover produto (lógica ou física - RF08)
    // Implementando remoção lógica (inativação)
    public boolean definirStatusAtivo(int codigoProduto, boolean isActive) {
        String sql = "UPDATE produtos SET is_active = ? WHERE codigo_produto = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, isActive);
            stmt.setInt(2, codigoProduto);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erro ao definir status ativo do produto: " + e.getMessage());
            return false;
        }
    }

    // Método auxiliar para mapear ResultSet para Objeto Produto
    private Produto mapResultSetToProduto(ResultSet rs) throws SQLException {
        Produto produto = new Produto();
        produto.setCodigoProduto(rs.getInt("codigo_produto"));
        produto.setNome(rs.getString("nome"));
        produto.setDescricao(rs.getString("descricao"));
        produto.setProcedencia(rs.getString("procedencia"));
        produto.setQuantidadeAtual(rs.getInt("quantidade_atual"));
        produto.setActive(rs.getBoolean("is_active"));
        return produto;
    }

    // Fechar conexão apenas se este DAO for o dono dela
    public void close() {
        if (connectionOwner) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Erro ao fechar conexão DAO: " + e.getMessage());
            }
        }
    }

    
    // Retorna a conexão associada a essa instância
public Connection getConnection() {
    return this.connection;
}

}

