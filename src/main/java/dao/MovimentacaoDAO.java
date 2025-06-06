package dao;

import factory.ConnectionFactory;
import model.Movimentacao;
import model.Produto;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MovimentacaoDAO {

    private Connection connection;
    private boolean connectionOwner; // Flag to indicate if this DAO instance owns the connection

    // Constructor for standalone use
    public MovimentacaoDAO() throws SQLException {
        this.connection = new ConnectionFactory().getConnection();
        this.connectionOwner = true;
    }

    // Constructor for transactional use (rarely needed externally for MovimentacaoDAO itself)
    protected MovimentacaoDAO(Connection connection) {
        this.connection = connection;
        this.connectionOwner = false;
    }

    // Método para registrar uma movimentação (entrada/saída) (RF05, RF11)
    // Este método DEVE ser transacional para garantir consistência
    public boolean registrarMovimentacao(Movimentacao movimentacao) {
        String sqlInsertMov = "INSERT INTO movimentacoes (data_hora, codigo_produto, quantidade, tipo_movimentacao, id_usuario) VALUES (?, ?, ?, ?, ?)";
        boolean sucesso = false;
        ProdutoDAO produtoDAOTransacional = null; // DAO para usar dentro da transação

        try {
            // 1. Inicia a transação
            connection.setAutoCommit(false);

            // 2. Cria instância do ProdutoDAO DENTRO da transação, usando a MESMA conexão
            produtoDAOTransacional = new ProdutoDAO(this.connection);

            // 3. Busca o produto para verificar estoque (para saídas) e obter quantidade atual
            Produto produto = produtoDAOTransacional.buscarPorCodigo(movimentacao.getCodigoProduto());
            if (produto == null || !produto.isActive()) {
                System.err.println("Erro: Produto não encontrado ou inativo.");
                connection.rollback(); // Desfaz a transação
                return false;
            }

            int quantidadeAtual = produto.getQuantidadeAtual();
            int quantidadeMovimentada = movimentacao.getQuantidade();
            int novaQuantidade;

            // 4. Valida e calcula nova quantidade
            if (movimentacao.getTipoMovimentacao() == Movimentacao.TipoMovimentacao.SAIDA) {
                if (quantidadeAtual < quantidadeMovimentada) {
                    System.err.println("Erro: Quantidade em estoque insuficiente para a saída (" + quantidadeAtual + " disponíveis).");
                    connection.rollback();
                    return false;
                }
                 if (quantidadeMovimentada <= 0) {
                     System.err.println("Erro: Quantidade de saída deve ser positiva.");
                     connection.rollback();
                     return false;
                 }
                novaQuantidade = quantidadeAtual - quantidadeMovimentada;
            } else { // ENTRADA
                 if (quantidadeMovimentada <= 0) {
                     System.err.println("Erro: Quantidade de entrada deve ser positiva.");
                     connection.rollback();
                     return false;
                 }
                novaQuantidade = quantidadeAtual + quantidadeMovimentada;
            }

            // 5. Insere o registro na tabela de movimentações
            try (PreparedStatement stmtMov = connection.prepareStatement(sqlInsertMov, Statement.RETURN_GENERATED_KEYS)) {
                stmtMov.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now())); // Usa data/hora atual (RF11)
                stmtMov.setInt(2, movimentacao.getCodigoProduto());
                stmtMov.setInt(3, movimentacao.getQuantidade());
                stmtMov.setString(4, movimentacao.getTipoMovimentacao().name()); // Salva ENUM como String
                stmtMov.setInt(5, movimentacao.getIdUsuario()); // ID do usuário logado

                int affectedRowsMov = stmtMov.executeUpdate();
                if (affectedRowsMov == 0) {
                    throw new SQLException("Falha ao inserir movimentação, nenhuma linha afetada.");
                }
                // Obter ID gerado (opcional, se necessário)
                 try (ResultSet generatedKeys = stmtMov.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        movimentacao.setIdMovimentacao(generatedKeys.getInt(1));
                    }
                }
            }

            // 6. Atualiza a quantidade na tabela de produtos usando o DAO transacional
            boolean atualizouQtd = produtoDAOTransacional.atualizarQuantidade(movimentacao.getCodigoProduto(), novaQuantidade, this.connection);

            if (!atualizouQtd) {
                 // A exceção SQLException já deve ter sido lançada por atualizarQuantidade
                 // Mas adicionamos uma verificação extra por segurança.
                 throw new SQLException("Falha ao atualizar quantidade do produto (retorno inesperado).");
            }

            // 7. Se tudo deu certo, comita a transação
            connection.commit();
            sucesso = true;
            System.out.println("Movimentação registrada e estoque atualizado com sucesso!"); // Log de sucesso

        } catch (SQLException e) {
            System.err.println("Erro ao registrar movimentação (transação falhou): " + e.getMessage());
            try {
                // 8. Se algo deu errado, faz rollback
                if (connection != null) {
                    connection.rollback();
                    System.err.println("Transação revertida.");
                }
            } catch (SQLException ex) {
                System.err.println("Erro CRÍTICO ao tentar reverter transação: " + ex.getMessage());
            }
            sucesso = false;
        } finally {
            try {
                // 9. Restaura o autoCommit para o estado padrão
                 if (connection != null) {
                    connection.setAutoCommit(true);
                 }
            } catch (SQLException e) {
                System.err.println("Erro ao restaurar autoCommit: " + e.getMessage());
            }
            // Não fechamos a conexão do produtoDAOTransacional pois ele usa a nossa conexão.
        }
        return sucesso;
    }

    // Método para consultar histórico de movimentações (RF12)
    // Adicionando nome do produto e email do usuário para exibição
    public List<MovimentacaoDetalhada> buscarHistoricoDetalhado(Integer codigoProdutoFiltro, LocalDateTime dataInicioFiltro, LocalDateTime dataFimFiltro) {
        List<MovimentacaoDetalhada> historico = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT m.id_movimentacao, m.data_hora, m.codigo_produto, p.nome as nome_produto, m.quantidade, m.tipo_movimentacao, m.id_usuario, u.email as email_usuario ");
        sqlBuilder.append("FROM movimentacoes m ");
        sqlBuilder.append("JOIN produtos p ON m.codigo_produto = p.codigo_produto ");
        sqlBuilder.append("JOIN usuarios u ON m.id_usuario = u.id_usuario ");
        sqlBuilder.append("WHERE 1=1 "); // Cláusula base para facilitar adição de filtros

        List<Object> params = new ArrayList<>();

        if (codigoProdutoFiltro != null && codigoProdutoFiltro > 0) {
            sqlBuilder.append("AND m.codigo_produto = ? ");
            params.add(codigoProdutoFiltro);
        }
        if (dataInicioFiltro != null) {
            sqlBuilder.append("AND m.data_hora >= ? ");
            params.add(Timestamp.valueOf(dataInicioFiltro));
        }
        if (dataFimFiltro != null) {
            // Ajusta para incluir todo o dia final
            LocalDateTime dataFimAjustada = dataFimFiltro.toLocalDate().plusDays(1).atStartOfDay(); 
            sqlBuilder.append("AND m.data_hora < ? "); // Usa '<' com o início do dia seguinte
            params.add(Timestamp.valueOf(dataFimAjustada));
        }

        sqlBuilder.append("ORDER BY m.data_hora DESC");

        try (PreparedStatement stmt = connection.prepareStatement(sqlBuilder.toString())) {
            // Define os parâmetros na query
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                MovimentacaoDetalhada mov = new MovimentacaoDetalhada();
                mov.setIdMovimentacao(rs.getInt("id_movimentacao"));
                mov.setDataHora(rs.getTimestamp("data_hora"));
                mov.setCodigoProduto(rs.getInt("codigo_produto"));
                mov.setNomeProduto(rs.getString("nome_produto"));
                mov.setQuantidade(rs.getInt("quantidade"));
                mov.setTipoMovimentacao(Movimentacao.TipoMovimentacao.valueOf(rs.getString("tipo_movimentacao")));
                mov.setIdUsuario(rs.getInt("id_usuario"));
                mov.setEmailUsuario(rs.getString("email_usuario"));
                historico.add(mov);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar histórico de movimentações: " + e.getMessage());
            e.printStackTrace(); // Ajuda a depurar
        }
        return historico;
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

    // Classe interna ou DTO para carregar dados detalhados do histórico
    public static class MovimentacaoDetalhada extends Movimentacao {
        private String nomeProduto;
        private String emailUsuario;

        public String getNomeProduto() {
            return nomeProduto;
        }

        public void setNomeProduto(String nomeProduto) {
            this.nomeProduto = nomeProduto;
        }

        public String getEmailUsuario() {
            return emailUsuario;
        }

        public void setEmailUsuario(String emailUsuario) {
            this.emailUsuario = emailUsuario;
        }
    }
}

