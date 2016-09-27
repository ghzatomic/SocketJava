package chat_caio;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import javax.swing.JOptionPane;

public class ClienteSocket extends Thread {
	private static int portaConexao = 5555;
	
	private String key = Constantes.CHAVE_CRIPTOGRAFIA;
	
	//O Visual
	private ClienteVisual visual = null;
	
	// parte que controla a recep��o de mensagens do cliente
	private Socket conexao;

	// construtor que recebe o socket do cliente
	public ClienteSocket(Socket socket) {
		this.conexao = socket;
		try {
			visual = new ClienteVisual(this.conexao);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Problema com o socket !");
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {
		try {
			// Instancia do atributo conexao do tipo Socket, conecta a IP do
			// Servidor, Porta
			Socket socket = new Socket("127.0.0.1", portaConexao);
			// Instancia do atributo saida, obtem os objetos que permitem
			// controlar o fluxo de comunica��o
			PrintStream saida = new PrintStream(socket.getOutputStream());
			
			String meuNome = JOptionPane.showInputDialog("Digite seu nome: ");
			// envia o nome digitado para o servidor
			saida.write(Util.encode(meuNome));
			// instancia a thread para ip e porta conectados e depois inicia ela
			Thread thread = new ClienteSocket(socket);
			thread.start();
			// Cria a variavel msg responsavel por enviar a mensagem para o
			// servidor
			/*String msg;
			while (true) {
				// cria linha para digita��o da mensagem e a armazena na
				// variavel msg
				System.out.print("Mensagem > ");
				msg = teclado.readLine();
				// envia a mensagem para o servidor
				saida.println(msg);
			}*/
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Falha na Conexao... .. ." + " IOException: "
					+ e);
		}
	}

	// execu��o da thread
	public void run() {
		try {
			// recebe mensagens de outro cliente atrav�s do servidor
			BufferedReader entrada = new BufferedReader(new InputStreamReader(
					this.conexao.getInputStream()));
			// cria variavel de mensagem
			String msg;
			while (true) {
				// pega o que o servidor enviou
			    byte[] data = Util.getData(this.conexao.getInputStream());
				// se a mensagem contiver dados, passa pelo if, caso contrario
				// cai no break e encerra a conexao
				if (data == null) {
					System.out.println("Conex�o encerrada!");
					System.exit(0);
				}
				// imprime a mensagem recebida
				String msgDecript = Util.decode(data);
				if (msgDecript.contains("{[LISTAUSR]}")){
					msgDecript=msgDecript.replace("{[LISTAUSR]}", "");
					String[] usrs = msgDecript.split(";");
					visual.refreshListUsuarios(usrs);
				}else{
					visual.addMensagem(msgDecript);
				}
			}
		} catch (IOException e) {
			// caso ocorra alguma exce��o de E/S, mostra qual foi.
			visual.addMensagem("Ocorreu uma Falha... .. ." + " IOException: "
					+ e);
		}
	}
}
