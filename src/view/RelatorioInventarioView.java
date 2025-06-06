package view;

import dao.ProdutoDAO;
import model.Produto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class RelatorioInventarioView extends JDialog {

    private JTable inventarioTable;
    private DefaultTableModel tableModel;
    private ProdutoDAO produtoDAO;

    public RelatorioInventarioView(Frame owner) {
        super(owner, "Relatório de Inventário Atual (RF13)", true);

        try {
            produtoDAO = new ProdutoDAO();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao conectar ao banco de dados: " + e.getMessage(), "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        setSize(700, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        // --- Tabela de Inventário ---
        tableModel = new DefaultTableModel(new Object[]{"Código", "Nome", "Descrição", "Procedência", "Quantidade Atual"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Tabela apenas para visualização
            }
             @Override
            public Class<?> getColumnClass(int columnIndex) {
                 if (columnIndex == 0 || columnIndex == 4) return Integer.class; // Código e Quantidade
                 return String.class;
            }
        };
        inventarioTable = new JTable(tableModel);
        inventarioTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Ajustar larguras
        inventarioTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        inventarioTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        inventarioTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        inventarioTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        inventarioTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        JScrollPane scrollPane = new JScrollPane(inventarioTable);
        add(scrollPane, BorderLayout.CENTER);

        // --- Botão para fechar (ou exportar, se implementado) ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton closeButton = new JButton("Fechar");
        closeButton.addActionListener(e -> dispose());
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // Carregar dados do inventário
        carregarInventario();

        //setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void carregarInventario() {
        try {
            // RF13: Lista todos os produtos, suas quantidades e procedências (ativos)
            List<Produto> produtos = produtoDAO.listarTodosAtivos(); 
            atualizarTabela(produtos);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar relatório de inventário: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void atualizarTabela(List<Produto> produtos) {
        tableModel.setRowCount(0); // Limpa tabela
        for (Produto p : produtos) {
            tableModel.addRow(new Object[]{
                    p.getCodigoProduto(),
                    p.getNome(),
                    p.getDescricao(),
                    p.getProcedencia(),
                    p.getQuantidadeAtual()
            });
        }
         inventarioTable.setAutoCreateRowSorter(true); // Permite ordenar
    }
}

