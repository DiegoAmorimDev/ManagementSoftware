package view;

import dao.MovimentacaoDAO;
import dao.ProdutoDAO;
import model.Movimentacao;
import model.Produto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Vector;

// Using JDatePicker for easier date selection (needs external library)
// Since we can't easily add external libs, we'll use JSpinners for date input
// import org.jdatepicker.impl.JDatePanelImpl;
// import org.jdatepicker.impl.JDatePickerImpl;
// import org.jdatepicker.impl.UtilDateModel;

public class HistoricoView extends JDialog {

    private JComboBox<ProdutoItem> produtoFiltroComboBox;
    private JSpinner dataInicioSpinner, dataFimSpinner;
    private JButton buscarButton, limparFiltrosButton;
    private JTable historicoTable;
    private DefaultTableModel tableModel;
    private MovimentacaoDAO movimentacaoDAO;
    private ProdutoDAO produtoDAO;

    // Classe interna para ComboBox de Produto (similar a MovimentacaoView)
    private static class ProdutoItem {
        private int codigo;
        private String nome;

        public ProdutoItem(int codigo, String nome) {
            this.codigo = codigo;
            this.nome = nome;
        }

        public int getCodigo() {
            return codigo;
        }

        @Override
        public String toString() {
            return nome + " (Cód: " + codigo + ")";
        }
    }

    public HistoricoView(Frame owner) {
        super(owner, "Histórico de Movimentações", true);

        try {
            movimentacaoDAO = new MovimentacaoDAO();
            produtoDAO = new ProdutoDAO();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao conectar ao banco de dados: " + e.getMessage(), "Erro de Conexão", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        setSize(900, 600);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));

        // --- Painel de Filtros ---
        JPanel filtroPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtroPanel.setBorder(BorderFactory.createTitledBorder("Filtros (RF12)"));

        // Filtro Produto
        filtroPanel.add(new JLabel("Produto:"));
        produtoFiltroComboBox = new JComboBox<>();
        carregarProdutosParaFiltro();
        filtroPanel.add(produtoFiltroComboBox);

        // Filtro Data Início
        filtroPanel.add(new JLabel("Data Início:"));
        // Usando JSpinner para data (requer conversão)
        SpinnerDateModel inicioModel = new SpinnerDateModel();
        dataInicioSpinner = new JSpinner(inicioModel);
        dataInicioSpinner.setEditor(new JSpinner.DateEditor(dataInicioSpinner, "dd/MM/yyyy"));
        // Define uma data inicial padrão (ex: início do mês)
        // inicioModel.setValue(Date.from(LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant()));
        filtroPanel.add(dataInicioSpinner);

        // Filtro Data Fim
        filtroPanel.add(new JLabel("Data Fim:"));
        SpinnerDateModel fimModel = new SpinnerDateModel();
        dataFimSpinner = new JSpinner(fimModel);
        dataFimSpinner.setEditor(new JSpinner.DateEditor(dataFimSpinner, "dd/MM/yyyy"));
        // fimModel.setValue(new Date()); // Data atual como padrão
        filtroPanel.add(dataFimSpinner);
        
        // Botões de Ação do Filtro
        buscarButton = new JButton("Buscar Histórico");
        limparFiltrosButton = new JButton("Limpar Filtros");
        filtroPanel.add(buscarButton);
        filtroPanel.add(limparFiltrosButton);

        add(filtroPanel, BorderLayout.NORTH);

        // --- Tabela de Histórico ---
        tableModel = new DefaultTableModel(new Object[]{"ID", "Data/Hora", "Cód. Prod", "Produto", "Qtd", "Tipo", "Usuário"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
             @Override
            public Class<?> getColumnClass(int columnIndex) {
                 if (columnIndex == 0 || columnIndex == 2 || columnIndex == 4) return Integer.class; // IDs e Qtd
                 if (columnIndex == 1) return Timestamp.class; // Data/Hora
                 return String.class; // Produto, Tipo, Usuário
            }
        };
        historicoTable = new JTable(tableModel);
        historicoTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Ajustar largura das colunas (opcional)
        historicoTable.getColumnModel().getColumn(0).setPreferredWidth(40); // ID
        historicoTable.getColumnModel().getColumn(1).setPreferredWidth(140); // Data/Hora
        historicoTable.getColumnModel().getColumn(2).setPreferredWidth(60); // Cód. Prod
        historicoTable.getColumnModel().getColumn(3).setPreferredWidth(200); // Produto
        historicoTable.getColumnModel().getColumn(4).setPreferredWidth(50); // Qtd
        historicoTable.getColumnModel().getColumn(5).setPreferredWidth(70); // Tipo
        historicoTable.getColumnModel().getColumn(6).setPreferredWidth(150); // Usuário
        
        JScrollPane scrollPane = new JScrollPane(historicoTable);
        add(scrollPane, BorderLayout.CENTER);

        // --- Ações dos Botões ---
        buscarButton.addActionListener(e -> buscarHistorico());
        limparFiltrosButton.addActionListener(e -> limparFiltrosECarregarTudo());

        // Carregar histórico inicial (sem filtros)
        buscarHistorico();

        //setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void carregarProdutosParaFiltro() {
        try {
            List<Produto> produtos = produtoDAO.listarTodos(); // Lista todos, inclusive inativos, para filtro
            Vector<ProdutoItem> items = new Vector<>();
            items.add(new ProdutoItem(0, "-- Todos os Produtos --")); // Opção para não filtrar por produto
            for (Produto p : produtos) {
                items.add(new ProdutoItem(p.getCodigoProduto(), p.getNome()));
            }
            produtoFiltroComboBox.setModel(new DefaultComboBoxModel<>(items));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar produtos para filtro: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buscarHistorico() {
        Integer codigoProdutoFiltro = null;
        Object selectedProdutoItem = produtoFiltroComboBox.getSelectedItem();
        if (selectedProdutoItem instanceof ProdutoItem) {
            ProdutoItem item = (ProdutoItem) selectedProdutoItem;
            if (item.getCodigo() > 0) {
                codigoProdutoFiltro = item.getCodigo();
            }
        }

        // Obter datas dos JSpinners e converter para LocalDateTime
        LocalDateTime dataInicio = null;
        Date inicioDate = (Date) dataInicioSpinner.getValue();
        if (inicioDate != null) {
            // Pega o início do dia selecionado
            dataInicio = inicioDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay();
        }

        LocalDateTime dataFim = null;
        Date fimDate = (Date) dataFimSpinner.getValue();
        if (fimDate != null) {
            // Pega o fim do dia selecionado (início do dia seguinte)
            dataFim = fimDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atTime(LocalTime.MAX); // Usa final do dia
        }

        try {
            List<MovimentacaoDAO.MovimentacaoDetalhada> historico = movimentacaoDAO.buscarHistoricoDetalhado(codigoProdutoFiltro, dataInicio, dataFim);
            atualizarTabela(historico);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao buscar histórico: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void limparFiltrosECarregarTudo() {
        produtoFiltroComboBox.setSelectedIndex(0); // Volta para "-- Todos --"
        // Reseta os spinners de data para null ou um valor padrão
        dataInicioSpinner.setValue(null); // Ou new Date() ou outra data padrão
        dataFimSpinner.setValue(null);   // Ou new Date()
        buscarHistorico(); // Busca sem filtros
    }

    private void atualizarTabela(List<MovimentacaoDAO.MovimentacaoDetalhada> historico) {
        tableModel.setRowCount(0); // Limpa tabela
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        for (MovimentacaoDAO.MovimentacaoDetalhada mov : historico) {
            tableModel.addRow(new Object[]{
                    mov.getIdMovimentacao(),
                    mov.getDataHora(), // Deixa como Timestamp para ordenação correta na tabela
                    // sdf.format(mov.getDataHora()), // Ou formata como String
                    mov.getCodigoProduto(),
                    mov.getNomeProduto(),
                    mov.getQuantidade(),
                    mov.getTipoMovimentacao().name(), // ENTRADA ou SAIDA
                    mov.getEmailUsuario()
            });
        }
         // Adiciona um sorter para permitir ordenação por coluna, especialmente data
        historicoTable.setAutoCreateRowSorter(true);
    }
}

