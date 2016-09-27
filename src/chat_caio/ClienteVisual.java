package chat_caio;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ClienteVisual {

	private String key = Constantes.CHAVE_CRIPTOGRAFIA;
	
	private static int linhas = 10;
	private static int colunas = 40;
	
	private JFrame frame = null;
	
	private JList listUsuarios;
	
	private DefaultListModel listModel = new DefaultListModel();
	
	private PrintStream saida = null;

	private JTextField textFieldGeral = null;
	
	private JTextArea textAreaMensagens = null;
	
	private JScrollPane jScrollPaneTextArea;
	
	private JScrollPane jScrollPaneListUsuarios;
	
	private JPanel panelRoot = null;
	
	public ClienteVisual(Socket socket) throws Exception {
		saida = new PrintStream(socket.getOutputStream());
		iniciaComponentes();
	}
	
	private void iniciaComponentes(){
		getFrame();
	}
	
	private JFrame getFrame() {
		if (frame == null){
			frame = new JFrame("Comunicador.");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.getContentPane().setLayout(new java.awt.FlowLayout());
			frame.getContentPane().add(this.getPanelRoot());
			
			getTextFieldGeral().requestFocus();
			frame.setVisible(true);
			frame.pack();
		}
		return frame;
	}
	
	private JTextField getTextFieldGeral() {
		final String padrao = "Digite e aperte <enter>";
		if (this.textFieldGeral == null){
			this.textFieldGeral = new JTextField(padrao,colunas);
			this.textFieldGeral.addFocusListener(new FocusListener() {
				@Override
				public void focusLost(FocusEvent e) {}
				
				@Override
				public void focusGained(FocusEvent e) {
					if (padrao.equals(textFieldGeral.getText())){
						textFieldGeral.setText("");
					}
				}
			});
			this.textFieldGeral.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String pulaLinha = "";
					if (!"".equals(textAreaMensagens.getText())){
						pulaLinha = "\n";
					}
					try {
						enviarMensagem(textFieldGeral.getText());
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					textAreaMensagens.setText(textAreaMensagens.getText()+pulaLinha+"Eu : "+textFieldGeral.getText());
					textFieldGeral.setText("");
				}
			});
		}
		return textFieldGeral;
	}
	

	private JPanel getPanelRoot() {
		if (this.panelRoot == null){
			this.panelRoot = new JPanel(new BorderLayout());
			this.panelRoot.add(getTextFieldGeral(),BorderLayout.PAGE_END);
			this.panelRoot.add(getjScrollPaneTextArea(),BorderLayout.CENTER);
			this.panelRoot.add(getjScrollPaneListUsuarios(),BorderLayout.WEST);
		}
		return panelRoot;
	}

	private JTextArea getTextAreaMensagens() {
		if (this.textAreaMensagens == null){
			this.textAreaMensagens = new JTextArea(linhas,colunas);
			this.textAreaMensagens.setEditable(false);
			this.textAreaMensagens.setLineWrap(true);
			this.textAreaMensagens.setWrapStyleWord(true);
		}
		return textAreaMensagens;
	}
	
	
	
	private JScrollPane getjScrollPaneTextArea() {
		if (this.jScrollPaneTextArea == null){
			jScrollPaneTextArea = new JScrollPane(this.getTextAreaMensagens());
		}
		return jScrollPaneTextArea;
	}

	public void addMensagem(String mensagem){
		String pulaLinha = "";
		if (!"".equals(this.getTextAreaMensagens().getText())){
			pulaLinha = "\n";
		}
		getTextAreaMensagens().setText(getTextAreaMensagens().getText()+pulaLinha+mensagem);
	}
	
	private JList getListUsuarios() {
		if (listUsuarios == null){
			listUsuarios = new JList(listModel);
		}
		return listUsuarios;
	}

	private JScrollPane getjScrollPaneListUsuarios() {
		if (this.jScrollPaneListUsuarios == null){
			jScrollPaneListUsuarios = new JScrollPane(this.getListUsuarios());
		}
		return jScrollPaneListUsuarios;
	}
	
	public void refreshListUsuarios(String[] usuarios){
		listModel.clear();
		for (String string : usuarios) {
			listModel.addElement(string);
		}
	}
	
	private void enviarMensagem(String mensagem) throws Exception{
		byte[] encript = Util.encode(mensagem);
		saida.write(encript);
	}
	
}
