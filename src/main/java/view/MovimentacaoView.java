package view;

import dao.MovimentacaoDAO;
import dao.ProdutoDAO;
import model.Movimentacao;
import model.Produto;
import model.Usuario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

public class MovimentacaoView extends JDialog {

    private JComboBox<ProdutoItem> produtoComboBox;
    private JSpinner quantidadeSpinner;
    private JRadioButton entradaRadioButton, saidaRadioButton;
    private ButtonGroup tipoMovimentacaoGroup;
    private JButton registrarButton;
    private JLabel statusLabel, produtoInfoLabel;
    private MovimentacaoDAO movimentacaoDAO;
    private ProdutoDAO produtoDAO;
    private Usuario usuarioLogado;

    // Classe interna para exibir nome e guardar código no ComboBox
    private static class ProdutoItem {
        private int codigo;
        private String nome;
        private int quantidadeAtual;

        public ProdutoItem(int codigo, String nome, int quantidadeAtual) {
            this.codigo = codigo;
            this.nome = nome;
            this.quantidadeAtual = quantidadeAtual;
        }

        public int getCodigo() {
            return codigo;
        }
        
        public int getQuantidadeAtual() {
            return quantidadeAtual;
        }

        @Override
        public String toString() {
            return nome + " (Cód: " + codigo + ")"; // Exibe nome e código
        }
    }

    public MovimentacaoView(Frame owner, Usuario usuarioLogado) {
        super(owner, "Registrar Movimentação de Estoque", true);
        this.usuarioLogado = usuarioLogado;

        try {
            movimentacaoDAO = new MovimentacaoDAO();
            produtoDAO = new ProdutoDAO(); // Usado para listar produtos
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao conectar ao banco de dados: " + e.getMessage(), "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        setSize(500, 350);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        // --- Painel do Formulário ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Linha 0: Produto
        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Produto:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        produtoComboBox = new JComboBox<>();
        carregarProdutosAtivos();
        produtoComboBox.addActionListener(e -> exibirInfoProdutoSelecionado()); // Mostra qtd atual
        formPanel.add(produtoComboBox, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;
        
        // Linha 1: Info Produto (Quantidade Atual)
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        produtoInfoLabel = new JLabel("Selecione um produto...");
        produtoInfoLabel.setFont(produtoInfoLabel.getFont().deriveFont(Font.ITALIC));
        produtoInfoLabel.setForeground(Color.GRAY);
        formPanel.add(produtoInfoLabel, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;

        // Linha 2: Quantidade
        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Quantidade:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        // Spinner para quantidade (mínimo 1)
        quantidadeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
        formPanel.add(quantidadeSpinner, gbc);

        // Linha 3: Tipo de Movimentação
        gbc.gridx = 0; gbc.gridy = 3; formPanel.add(new JLabel("Tipo:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3;
        entradaRadioButton = new JRadioButton("Entrada");
        entradaRadioButton.setSelected(true); // Default para entrada
        formPanel.add(entradaRadioButton, gbc);
        gbc.gridx = 2; gbc.gridy = 3;
        saidaRadioButton = new JRadioButton("Saída");
        formPanel.add(saidaRadioButton, gbc);
        tipoMovimentacaoGroup = new ButtonGroup();
        tipoMovimentacaoGroup.add(entradaRadioButton);
        tipoMovimentacaoGroup.add(saidaRadioButton);

        // --- Painel de Botões e Status ---
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));
        registrarButton = new JButton("Registrar Movimentação");
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setForeground(Color.BLUE);
        bottomPanel.add(registrarButton, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        add(formPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Ação do botão registrar
        registrarButton.addActionListener(e -> registrar());
        
        // Exibe info do primeiro produto ao abrir
        exibirInfoProdutoSelecionado(); 

        //setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void carregarProdutosAtivos() {
        try {
            List<Produto> produtos = produtoDAO.listarTodosAtivos();
            Vector<ProdutoItem> items = new Vector<>();
            if (produtos.isEmpty()) {
                 items.add(new ProdutoItem(0, "Nenhum produto ativo cadastrado", 0));
                 produtoComboBox.setEnabled(false);
                 registrarButton.setEnabled(false);
            } else {
                 items.add(new ProdutoItem(0, "-- Selecione --", 0)); // Item default
                for (Produto p : produtos) {
                    items.add(new ProdutoItem(p.getCodigoProduto(), p.getNome(), p.getQuantidadeAtual()));
                }
            }
            produtoComboBox.setModel(new DefaultComboBoxModel<>(items));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar produtos: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            produtoComboBox.setEnabled(false);
            registrarButton.setEnabled(false);
        }
    }
    
    private void exibirInfoProdutoSelecionado() {
        Object selectedItem = produtoComboBox.getSelectedItem();
        if (selectedItem instanceof ProdutoItem) {
            ProdutoItem item = (ProdutoItem) selectedItem;
            if (item.getCodigo() > 0) { // Ignora o item "-- Selecione --"
                produtoInfoLabel.setText("Quantidade atual em estoque: " + item.getQuantidadeAtual());
                produtoInfoLabel.setForeground(Color.DARK_GRAY);
            } else {
                 produtoInfoLabel.setText("Selecione um produto...");
                 produtoInfoLabel.setForeground(Color.GRAY);
            }
        } else {
             produtoInfoLabel.setText("Selecione um produto...");
             produtoInfoLabel.setForeground(Color.GRAY);
        }
    }

    private void registrar() {
        // Validações
        Object selectedItem = produtoComboBox.getSelectedItem();
        if (!(selectedItem instanceof ProdutoItem) || ((ProdutoItem) selectedItem).getCodigo() <= 0) {
            statusLabel.setText("Selecione um produto válido.");
            statusLabel.setForeground(Color.RED);
            return;
        }

        ProdutoItem produtoSelecionado = (ProdutoItem) selectedItem;
        int quantidade = (Integer) quantidadeSpinner.getValue();

        if (quantidade <= 0) {
            statusLabel.setText("Quantidade deve ser maior que zero.");
            statusLabel.setForeground(Color.RED);
            return;
        }

        Movimentacao.TipoMovimentacao tipo;
        if (entradaRadioButton.isSelected()) {
            tipo = Movimentacao.TipoMovimentacao.ENTRADA;
        } else if (saidaRadioButton.isSelected()) {
            tipo = Movimentacao.TipoMovimentacao.SAIDA;
        } else {
            statusLabel.setText("Selecione o tipo de movimentação (Entrada/Saída).");
            statusLabel.setForeground(Color.RED);
            return;
        }

        // Cria objeto Movimentacao
        Movimentacao mov = new Movimentacao();
        mov.setCodigoProduto(produtoSelecionado.getCodigo());
        mov.setQuantidade(quantidade);
        mov.setTipoMovimentacao(tipo);
        mov.setIdUsuario(usuarioLogado.getIdUsuario()); // Usuário logado

        // Tenta registrar via DAO
        try {
            boolean sucesso = movimentacaoDAO.registrarMovimentacao(mov);
            if (sucesso) {
                statusLabel.setText("Movimentação registrada com sucesso!");
                statusLabel.setForeground(Color.GREEN.darker());
                // Limpa campos e recarrega produtos para atualizar quantidade no combo
                quantidadeSpinner.setValue(1);
                entradaRadioButton.setSelected(true);
                carregarProdutosAtivos(); // Recarrega a lista de produtos
                produtoComboBox.setSelectedIndex(0); // Volta para "-- Selecione --"
                exibirInfoProdutoSelecionado(); // Limpa info
                
                // Opcional: Fechar o diálogo após sucesso?
                // int option = JOptionPane.showConfirmDialog(this, "Movimentação registrada com sucesso!\nDeseja registrar outra?", "Sucesso", JOptionPane.YES_NO_OPTION);
                // if (option == JOptionPane.NO_OPTION) {
                //     dispose();
                // }
                
            } else {
                // Mensagem de erro específica já deve ter sido logada pelo DAO
                statusLabel.setText("Erro ao registrar movimentação. Verifique o console/logs.");
                statusLabel.setForeground(Color.RED);
                // Recarrega produtos caso a falha tenha sido por estoque insuficiente, para mostrar qtd atual
                carregarProdutosAtivos();
                // Re-seleciona o produto que falhou
                 for (int i = 0; i < produtoComboBox.getItemCount(); i++) {
                    if (produtoComboBox.getItemAt(i).getCodigo() == produtoSelecionado.getCodigo()) {
                        produtoComboBox.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            statusLabel.setText("Erro crítico ao registrar: " + e.getMessage());
            statusLabel.setForeground(Color.RED);
            e.printStackTrace();
            // Recarrega produtos
            carregarProdutosAtivos();
        }
    }
}

