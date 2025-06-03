package view;

import dao.ProdutoDAO;
import model.Produto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;

public class ProdutoView extends JDialog { // Usar JDialog para ser modal em relação ao MainMenu

    private ProdutoDAO produtoDAO;
    private JTable produtoTable;
    private DefaultTableModel tableModel;
    private JTextField codigoField, nomeField, descricaoField, procedenciaField, quantidadeField, buscaField;
    private JCheckBox ativoCheckBox;
    private JButton addButton, editButton, saveButton, cancelButton, searchButton, clearButton, toggleActiveButton;
    private JPanel formPanel, buttonPanel, searchPanel;
    private boolean editando = false;

    public ProdutoView(Frame owner) { // Recebe o Frame pai (MainMenuView)
        super(owner, "Gerenciamento de Produtos", true); // Modal
        try {
            produtoDAO = new ProdutoDAO();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao conectar ao banco de dados: " + e.getMessage(), "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
            dispose(); // Fecha se não conectar
            return;
        }

        setSize(800, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        // --- Tabela de Produtos ---
        tableModel = new DefaultTableModel(new Object[]{"Código", "Nome", "Quantidade", "Procedência", "Ativo"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Não editável diretamente na tabela
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                 if (columnIndex == 4) return Boolean.class; // Coluna "Ativo" é booleana
                 if (columnIndex == 0 || columnIndex == 2) return Integer.class; // Código e Quantidade são inteiros
                 return String.class;
            }
        };
        produtoTable = new JTable(tableModel);
        produtoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        produtoTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    selecionarProduto();
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(produtoTable);
        add(scrollPane, BorderLayout.CENTER);

        // --- Painel de Busca ---
        searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Buscar (Código ou Nome):"));
        buscaField = new JTextField(20);
        searchButton = new JButton("Buscar");
        clearButton = new JButton("Limpar Busca");
        searchPanel.add(buscaField);
        searchPanel.add(searchButton);
        searchPanel.add(clearButton);
        add(searchPanel, BorderLayout.NORTH);

        // --- Formulário de Edição/Adição ---
        formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Detalhes do Produto"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.anchor = GridBagConstraints.WEST;

        // Linha 0
        gbc.gridx = 0; gbc.gridy = 0; formPanel.add(new JLabel("Código:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; codigoField = new JTextField(5); codigoField.setEditable(false); formPanel.add(codigoField, gbc);
        gbc.gridx = 2; gbc.gridy = 0; formPanel.add(new JLabel("Ativo:"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; ativoCheckBox = new JCheckBox(); ativoCheckBox.setEnabled(false); formPanel.add(ativoCheckBox, gbc);

        // Linha 1
        gbc.gridx = 0; gbc.gridy = 1; formPanel.add(new JLabel("Nome:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; nomeField = new JTextField(30); formPanel.add(nomeField, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;

        // Linha 2
        gbc.gridx = 0; gbc.gridy = 2; formPanel.add(new JLabel("Descrição:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; descricaoField = new JTextField(30); formPanel.add(descricaoField, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;

        // Linha 3
        gbc.gridx = 0; gbc.gridy = 3; formPanel.add(new JLabel("Procedência:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; procedenciaField = new JTextField(30); formPanel.add(procedenciaField, gbc);
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.NONE;

        // Linha 4
        gbc.gridx = 0; gbc.gridy = 4; formPanel.add(new JLabel("Qtd. Inicial/Atual:"), gbc);
        gbc.gridx = 1; gbc.gridy = 4; quantidadeField = new JTextField(5); formPanel.add(quantidadeField, gbc);

        habilitarFormulario(false);
        add(formPanel, BorderLayout.SOUTH);

        // --- Painel de Botões ---
        buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        addButton = new JButton("Adicionar Novo");
        editButton = new JButton("Editar Selecionado");
        saveButton = new JButton("Salvar");
        cancelButton = new JButton("Cancelar");
        toggleActiveButton = new JButton("Ativar/Inativar");

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(toggleActiveButton);
        
        // Adiciona o painel de botões abaixo do formulário
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(formPanel, BorderLayout.CENTER);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);

        configurarEstadoInicialBotoes();

        // --- Ações dos Botões ---
        addButton.addActionListener(e -> adicionarNovoProduto());
        editButton.addActionListener(e -> editarProdutoSelecionado());
        saveButton.addActionListener(e -> salvarProduto());
        cancelButton.addActionListener(e -> cancelarEdicao());
        searchButton.addActionListener(e -> buscarProdutos());
        clearButton.addActionListener(e -> limparBuscaECarregarTodos());
        toggleActiveButton.addActionListener(e -> toggleAtivoProdutoSelecionado());

        // Carregar dados iniciais
        carregarProdutos();

        //setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // Fecha apenas o diálogo
    }

    private void carregarProdutos() {
        try {
            List<Produto> produtos = produtoDAO.listarTodos(); // Carrega todos (ativos e inativos)
            atualizarTabela(produtos);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar produtos: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void buscarProdutos() {
        String filtro = buscaField.getText().trim();
        if (filtro.isEmpty()) {
            carregarProdutos(); // Se vazio, carrega todos
            return;
        }
        try {
            List<Produto> produtos = produtoDAO.buscarPorCodigoOuNome(filtro);
             if (produtos.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nenhum produto encontrado para: " + filtro, "Busca", JOptionPane.INFORMATION_MESSAGE);
            }
            atualizarTabela(produtos);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao buscar produtos: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void limparBuscaECarregarTodos() {
        buscaField.setText("");
        carregarProdutos();
    }

    private void atualizarTabela(List<Produto> produtos) {
        tableModel.setRowCount(0); // Limpa tabela
        for (Produto p : produtos) {
            tableModel.addRow(new Object[]{
                    p.getCodigoProduto(),
                    p.getNome(),
                    p.getQuantidadeAtual(),
                    p.getProcedencia(),
                    p.isActive()
            });
        }
    }

    private void selecionarProduto() {
        int selectedRow = produtoTable.getSelectedRow();
        if (selectedRow >= 0) {
            codigoField.setText(tableModel.getValueAt(selectedRow, 0).toString());
            nomeField.setText(tableModel.getValueAt(selectedRow, 1).toString());
            quantidadeField.setText(tableModel.getValueAt(selectedRow, 2).toString());
            procedenciaField.setText(tableModel.getValueAt(selectedRow, 3) != null ? tableModel.getValueAt(selectedRow, 3).toString() : "");
            ativoCheckBox.setSelected((Boolean) tableModel.getValueAt(selectedRow, 4));
            
            // Busca descrição separadamente, pois não está na tabela
            try {
                 Produto p = produtoDAO.buscarPorCodigo(Integer.parseInt(codigoField.getText()));
                 if(p != null) {
                     descricaoField.setText(p.getDescricao() != null ? p.getDescricao() : "");
                 }
            } catch (Exception e) {
                 descricaoField.setText(""); // Limpa se der erro
            }

            habilitarFormulario(false);
            editButton.setEnabled(true);
            toggleActiveButton.setEnabled(true);
            cancelButton.setEnabled(false);
            saveButton.setEnabled(false);
            addButton.setEnabled(true);
        } else {
            limparFormulario();
            configurarEstadoInicialBotoes();
        }
    }

    private void adicionarNovoProduto() {
        editando = false;
        limparFormulario();
        habilitarFormulario(true);
        quantidadeField.setEditable(true); // Permite definir qtd inicial
        ativoCheckBox.setSelected(true); // Novo produto começa ativo
        ativoCheckBox.setEnabled(false); // Não pode mudar status na criação
        nomeField.requestFocus();

        addButton.setEnabled(false);
        editButton.setEnabled(false);
        saveButton.setEnabled(true);
        cancelButton.setEnabled(true);
        toggleActiveButton.setEnabled(false);
        produtoTable.clearSelection();
    }

    private void editarProdutoSelecionado() {
        if (produtoTable.getSelectedRow() < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um produto na tabela para editar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        editando = true;
        habilitarFormulario(true);
        quantidadeField.setEditable(false); // Quantidade só muda via movimentação
        ativoCheckBox.setEnabled(false); // Status muda pelo botão Ativar/Inativar
        nomeField.requestFocus();

        addButton.setEnabled(false);
        editButton.setEnabled(false);
        saveButton.setEnabled(true);
        cancelButton.setEnabled(true);
        toggleActiveButton.setEnabled(false);
    }

    private void salvarProduto() {
        // Validações
        String nome = nomeField.getText().trim();
        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "O nome do produto é obrigatório.", "Erro de Validação", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int quantidade;
        try {
             // Quantidade só é editável na criação
            if (!editando) {
                 quantidade = Integer.parseInt(quantidadeField.getText().trim());
                 if (quantidade < 0) throw new NumberFormatException();
            } else {
                // Na edição, pega a quantidade atual que não foi alterada no form
                 quantidade = Integer.parseInt(tableModel.getValueAt(produtoTable.getSelectedRow(), 2).toString());
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantidade inválida. Deve ser um número inteiro não negativo.", "Erro de Validação", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setDescricao(descricaoField.getText().trim());
        produto.setProcedencia(procedenciaField.getText().trim());
        produto.setQuantidadeAtual(quantidade);
        produto.setActive(ativoCheckBox.isSelected()); // Pega o status atual

        boolean sucesso = false;
        try {
            if (editando) {
                produto.setCodigoProduto(Integer.parseInt(codigoField.getText()));
                sucesso = produtoDAO.atualizarProduto(produto);
            } else {
                Produto novoProduto = produtoDAO.criarProduto(produto);
                if (novoProduto != null) {
                    sucesso = true;
                }
            }

            if (sucesso) {
                JOptionPane.showMessageDialog(this, "Produto " + (editando ? "atualizado" : "cadastrado") + " com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                limparFormulario();
                habilitarFormulario(false);
                configurarEstadoInicialBotoes();
                carregarProdutos(); // Recarrega a tabela
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao salvar o produto.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar produto: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelarEdicao() {
        limparFormulario();
        habilitarFormulario(false);
        configurarEstadoInicialBotoes();
        produtoTable.clearSelection();
    }
    
    private void toggleAtivoProdutoSelecionado() {
         int selectedRow = produtoTable.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um produto na tabela para ativar/inativar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int codigo = (int) tableModel.getValueAt(selectedRow, 0);
        boolean statusAtual = (boolean) tableModel.getValueAt(selectedRow, 4);
        String nomeProduto = tableModel.getValueAt(selectedRow, 1).toString();
        
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Deseja " + (statusAtual ? "INATIVAR" : "ATIVAR") + " o produto '" + nomeProduto + "'?",
                "Confirmar Alteração de Status", 
                JOptionPane.YES_NO_OPTION);
                
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean sucesso = produtoDAO.definirStatusAtivo(codigo, !statusAtual);
                if (sucesso) {
                    JOptionPane.showMessageDialog(this, "Status do produto alterado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    carregarProdutos(); // Recarrega para mostrar o novo status
                    limparFormulario();
                    configurarEstadoInicialBotoes();
                } else {
                     JOptionPane.showMessageDialog(this, "Erro ao alterar status do produto.", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                 JOptionPane.showMessageDialog(this, "Erro ao alterar status: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void habilitarFormulario(boolean habilitar) {
        nomeField.setEditable(habilitar);
        descricaoField.setEditable(habilitar);
        procedenciaField.setEditable(habilitar);
        quantidadeField.setEditable(habilitar && !editando); // Só editável na criação
        // ativoCheckBox não é editável diretamente no form
    }

    private void limparFormulario() {
        codigoField.setText("");
        nomeField.setText("");
        descricaoField.setText("");
        procedenciaField.setText("");
        quantidadeField.setText("");
        ativoCheckBox.setSelected(false);
        editando = false;
    }

    private void configurarEstadoInicialBotoes() {
        addButton.setEnabled(true);
        editButton.setEnabled(false);
        saveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        toggleActiveButton.setEnabled(false);
    }

}

