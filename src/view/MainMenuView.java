package view;

import model.Usuario;

import javax.swing.*;
import java.awt.*;
// Removido imports não utilizados de ActionEvent e ActionListener, pois usamos lambdas

public class MainMenuView extends JFrame {

    private Usuario usuarioLogado;

    public MainMenuView(Usuario usuario) {
        this.usuarioLogado = usuario;

        setTitle("Sistema de Gestão de Estoque - Menu Principal");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centraliza na tela
        setLayout(new BorderLayout());

        // --- Barra de Menus --- 
        JMenuBar menuBar = new JMenuBar();

        // Menu Arquivo
        JMenu arquivoMenu = new JMenu("Arquivo");
        JMenuItem sairItem = new JMenuItem("Sair");
        sairItem.addActionListener(e -> System.exit(0));
        arquivoMenu.add(sairItem);
        menuBar.add(arquivoMenu);

        // Menu Cadastro
        JMenu cadastroMenu = new JMenu("Cadastros");
        JMenuItem produtosItem = new JMenuItem("Produtos");
        produtosItem.addActionListener(e -> abrirGerenciamentoProdutos());
        cadastroMenu.add(produtosItem);
        menuBar.add(cadastroMenu);

        // Menu Movimentação
        JMenu movimentacaoMenu = new JMenu("Movimentação");
        JMenuItem registrarMovItem = new JMenuItem("Registrar Entrada/Saída");
        registrarMovItem.addActionListener(e -> abrirRegistroMovimentacao());
        JMenuItem historicoMovItem = new JMenuItem("Histórico de Movimentações");
        historicoMovItem.addActionListener(e -> abrirHistoricoMovimentacoes());
        movimentacaoMenu.add(registrarMovItem);
        movimentacaoMenu.add(historicoMovItem);
        menuBar.add(movimentacaoMenu);

        // Menu Relatórios
        JMenu relatoriosMenu = new JMenu("Relatórios");
        JMenuItem inventarioItem = new JMenuItem("Inventário Atual");
        inventarioItem.addActionListener(e -> gerarRelatorioInventario());
        relatoriosMenu.add(inventarioItem);
        menuBar.add(relatoriosMenu);

        // Menu Administração (somente para admin - RF15)
        if (usuarioLogado != null && usuarioLogado.isAdmin()) { // Adiciona verificação de null para segurança
            JMenu adminMenu = new JMenu("Administração");
            JMenuItem gerenciarUsuariosItem = new JMenuItem("Gerenciar Usuários");
            gerenciarUsuariosItem.addActionListener(e -> abrirGerenciamentoUsuarios());
            adminMenu.add(gerenciarUsuariosItem);
            menuBar.add(adminMenu);
        }

        setJMenuBar(menuBar);

        // --- Painel Principal (Pode exibir informações ou ser um container) ---
        JPanel mainPanel = new JPanel(new BorderLayout());
        String welcomeText = "Bem-vindo(a)!" + (usuarioLogado != null ? (" " + usuarioLogado.getEmail()) : "");
        JLabel welcomeLabel = new JLabel(welcomeText, SwingConstants.CENTER);
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        mainPanel.add(welcomeLabel, BorderLayout.NORTH);
        
        // Adiciona um painel central que pode ser substituído pelas telas de gerenciamento
        // JPanel contentPanel = new JPanel(); 
        // mainPanel.add(contentPanel, BorderLayout.CENTER); // Não é necessário se abrirmos JDialogs

        add(mainPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    // --- Métodos para abrir as diferentes telas --- 

    private void abrirGerenciamentoProdutos() {
        // Abre a tela de gerenciamento de produtos como um JDialog modal
        ProdutoView produtoView = new ProdutoView(this); // Passa a referência do menu principal como pai
        produtoView.setVisible(true); // Torna o diálogo visível
    }

    private void abrirRegistroMovimentacao() {
        // Abre a tela para registrar entrada/saída, passando o usuário logado
        MovimentacaoView movimentacaoView = new MovimentacaoView(this, usuarioLogado);
        movimentacaoView.setVisible(true);
    }

    private void abrirHistoricoMovimentacoes() {
        // Abre a tela para visualizar histórico
        HistoricoView historicoView = new HistoricoView(this);
        historicoView.setVisible(true);
    }

    private void gerarRelatorioInventario() {
        // Abre a tela do relatório de inventário
        RelatorioInventarioView relatorioView = new RelatorioInventarioView(this);
        relatorioView.setVisible(true);
    }

    private void abrirGerenciamentoUsuarios() {
        // Abre a tela de gerenciamento de usuários (somente se for admin, já verificado no menu)
        UsuarioAdminView usuarioAdminView = new UsuarioAdminView(this);
        usuarioAdminView.setVisible(true);
    }

}

