package view;

import dao.ProdutoDAO;
import model.Produto;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.JasperViewer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

public class RelatorioInventarioView extends JDialog {

    private JTable inventarioTable;
    private DefaultTableModel tableModel;
    private ProdutoDAO produtoDAO;

    public RelatorioInventarioView(Frame owner) {
        super(owner, "Relatório de Inventário Atual (RF13)", true);

        // Faz com que, ao clicar no “X” do próprio diálogo, ele seja efetivamente descartado
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        try {
            produtoDAO = new ProdutoDAO();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Erro ao conectar ao banco de dados: " + e.getMessage(),
                "Erro de Conexão",
                JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        setSize(700, 500);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        // --- Tabela de Inventário ---
        tableModel = new DefaultTableModel(
            new Object[]{"Código", "Nome", "Descrição", "Procedência", "Quantidade Atual"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Só visualização
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0 || columnIndex == 4) return Integer.class;
                return String.class;
            }
        };
        inventarioTable = new JTable(tableModel);
        inventarioTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inventarioTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        inventarioTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        inventarioTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        inventarioTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        inventarioTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        JScrollPane scrollPane = new JScrollPane(inventarioTable);
        add(scrollPane, BorderLayout.CENTER);

        // --- Painel de botões ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton btnGerarPDF = new JButton("Gerar PDF");
        btnGerarPDF.addActionListener(e -> gerarRelatorioPDF());
        bottomPanel.add(btnGerarPDF);

        JButton closeButton = new JButton("Fechar");
        closeButton.addActionListener(e -> dispose());
        bottomPanel.add(closeButton);

        add(bottomPanel, BorderLayout.SOUTH);

        carregarInventario();
    }

    private void carregarInventario() {
        try {
            List<Produto> produtos = produtoDAO.listarTodosAtivos();
            atualizarTabela(produtos);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Erro ao gerar relatório de inventário: " + e.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void atualizarTabela(List<Produto> produtos) {
        tableModel.setRowCount(0);
        for (Produto p : produtos) {
            tableModel.addRow(new Object[]{
                p.getCodigoProduto(),
                p.getNome(),
                p.getDescricao(),
                p.getProcedencia(),
                p.getQuantidadeAtual()
            });
        }
        inventarioTable.setAutoCreateRowSorter(true);
    }

    private void gerarRelatorioPDF() {
        try {
            String caminhoJasper = "src/main/java/view/Blank_A4.jasper";
            File jasperFile = new File(caminhoJasper);
            if (!jasperFile.exists()) {
                JOptionPane.showMessageDialog(this,
                    "Arquivo .jasper não encontrado:\n" + jasperFile.getAbsolutePath(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            Connection conexao = produtoDAO.getConnection();
            JasperPrint print = JasperFillManager.fillReport(caminhoJasper, new HashMap<>(), conexao);

            // 1) Cria o JasperViewer (false para não encerrar a JVM ao fechar)
            JasperViewer viewer = new JasperViewer(print, false);
            viewer.setTitle("Relatório de Inventário");
            viewer.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            // 2) Faz com que este JFrame não seja afetado pelo bloqueio do diálogo modal
            viewer.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);

            // 3) Mantém o JasperViewer sempre à frente
            viewer.setAlwaysOnTop(true);

            // 4) Exibe e traz para frente
            viewer.setVisible(true);
            viewer.toFront();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Erro ao gerar PDF: " + ex.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

}
