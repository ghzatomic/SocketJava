package chat_caio;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

public class ServidorSocket extends Thread {
	
	private static int portaConexao = 5555;
	
	private String key = Constantes.CHAVE_CRIPTOGRAFIA;
	
	// Parte que controla as conexões por meio de threads.
	private static Vector CLIENTES;
	// socket deste cliente
	private Socket conexao;
	// nome deste cliente
	private String nomeCliente;
	// lista que armazena nome de CLIENTES
	private static List<String> LISTA_DE_NOMES = new ArrayList();

	// construtor que recebe o socket deste cliente
	public ServidorSocket(Socket socket) {
		this.conexao = socket;
	}

	// testa se nomes são iguais, se for retorna true
	public boolean armazena(String newName) {
		// System.out.println(LISTA_DE_NOMES);
		for (int i = 0; i < LISTA_DE_NOMES.size(); i++) {
			if (LISTA_DE_NOMES.get(i).equals(newName))
				return true;
		}
		// adiciona na lista apenas se não existir
		LISTA_DE_NOMES.add(newName);
		return false;
	}

	// remove da lista os CLIENTES que já deixaram o chat
	public void remove(String oldName) {
		for (int i = 0; i < LISTA_DE_NOMES.size(); i++) {
			if (LISTA_DE_NOMES.get(i).equals(oldName))
				LISTA_DE_NOMES.remove(oldName);
		}
	}

	public static void main(String args[]) {
		// instancia o vetor de CLIENTES conectados
		CLIENTES = new Vector();
		try {
			// cria um socket que fica escutando a porta 5555.
			ServerSocket server = new ServerSocket(portaConexao);
			System.out.println("ServidorSocket rodando na porta "+portaConexao);
			// Loop principal.
			while (true) {
				// aguarda algum cliente se conectar.
				// A execução do servidor fica bloqueada na chamada do método
				// accept da
				// classe ServerSocket até que algum cliente se conecte ao
				// servidor.
				// O próprio método desbloqueia e retorna com um objeto da
				// classe Socket
				Socket conexao = server.accept();
				// cria uma nova thread para tratar essa conexão
				Thread t = new ServidorSocket(conexao);
				t.start();
				// voltando ao loop, esperando mais alguém se conectar.
			}
		} catch (IOException e) {
			// caso ocorra alguma excessão de E/S, mostre qual foi.
			System.out.println("IOException: " + e);
		}
	}

	// execução da thread
	public void run() {
		PrintStream saida = null;
		try {
			saida = new PrintStream(this.conexao.getOutputStream());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			// objetos que permitem controlar fluxo de comunicação que vem do
			// cliente
			// recebe o nome do cliente
			this.nomeCliente = Util.decode(Util.getData(this.conexao.getInputStream()));
			// chamada ao metodo que testa nomes iguais
			if (armazena(this.nomeCliente)) {
				saida.println("Este nome ja existe! Conecte novamente com outro Nome.");
				CLIENTES.add(saida);
				// fecha a conexao com este cliente
				//this.conexao.close();
				return;
			}else {
				sendToAllCript(saida, " ", "entrou !");
				CLIENTES.add(saida);
				pushUserList();
				// mostra o nome do cliente conectado ao servidor
				System.out.println(this.nomeCliente
						+ " : Conectado ao Servidor!");
			}
			// igual a null encerra a execução
			if (this.nomeCliente == null) {
				return;
			}
			// adiciona os dados de saida do cliente no objeto CLIENTES
			// recebe a mensagem do cliente
			byte[] data = Util.getData(this.conexao.getInputStream());
			//String msg = entrada.read();
			// Verificar se linha é null (conexão encerrada)
			// Se não for nula, mostra a troca de mensagens entre os CLIENTES
			while (data != null) {
				// reenvia a linha para todos os CLIENTES conectados
				sendToAll(saida, " diz: ", data);
				// espera por uma nova linha.
			    data = Util.getData(this.conexao.getInputStream());
			}
			// se cliente enviar linha em branco, mostra a saida no servidor
			System.out.println(this.nomeCliente + " saiu do bate-papo!");
			// se cliente enviar linha em branco, servidor envia mensagem de
			// saida do chat aos CLIENTES conectados
			sendToAllCript(saida, " saiu", " do bate-papo!");
			// remove nome da lista
			remove(this.nomeCliente);
			// exclui atributos setados ao cliente
			pushUserList();
			CLIENTES.remove(saida);
			// fecha a conexao com este cliente
			//this.conexao.close();
		} catch (Exception e) {
			try {
				sendToAllCript(saida, " saiu", " do bate-papo!");
				remove(this.nomeCliente);
				pushUserList();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			CLIENTES.remove(saida);
			// Caso ocorra alguma excessão de E/S, mostre qual foi.
			System.out.println("Falha na Conexao... .. ." + " IOException: "
					+ e);
		}
	}

	public void sendToAllCript(PrintStream saida, String acao, String msg)
			throws IOException {
		Enumeration e = CLIENTES.elements();
		while (e.hasMoreElements()) {
			// obtém o fluxo de saída de um dos CLIENTES
			PrintStream chat = (PrintStream) e.nextElement();
			// envia para todos, menos para o próprio usuário
			if (chat != saida) {
				byte[] encript = Util.encode(this.nomeCliente + acao + msg);
				chat.write(encript);
			}
		}
	}
	
	public void pushUserList()
			throws IOException {
		Enumeration e = CLIENTES.elements();
		String lista = "";
		for (String nome : LISTA_DE_NOMES) {
			if ("".equals(nome)){
				lista = nome;
			}else{
				lista = lista+";"+nome;
			}
		}
		while (e.hasMoreElements()) {
			// obtém o fluxo de saída de um dos CLIENTES
			PrintStream chat = (PrintStream) e.nextElement();
			// envia para todos, menos para o próprio usuário
			byte[] encript = Util.encode("{[LISTAUSR]}"+lista);
			chat.write(encript);
		}
	}
	
	// enviar uma mensagem para todos, menos para o próprio
	public void sendToAll(PrintStream saida, String acao, byte[] msg)
			throws IOException {
		Enumeration e = CLIENTES.elements();
		while (e.hasMoreElements()) {
			// obtém o fluxo de saída de um dos CLIENTES
			PrintStream chat = (PrintStream) e.nextElement();
			// envia para todos, menos para o próprio usuário
			if (chat != saida) {
				String msgDecript = Util.decode(msg);
				byte[] encript = Util.encode(this.nomeCliente + acao+msgDecript);
				chat.write(encript);
			}
		}
	}
	
}
