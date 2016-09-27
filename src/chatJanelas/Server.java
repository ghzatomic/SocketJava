package chatJanelas;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Server extends JFrame {

	private JTextField enterField;
	private JTextArea displayArea;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private ServerSocket server;
	private java.net.Socket connection;
	private int counter = 1;

	public Server() {

		super("Server");

		Container container = getContentPane();

		enterField = new JTextField();
		enterField.setEnabled(false);

		enterField.addActionListener(

		new ActionListener() {

			public void actionPerformed(ActionEvent event) {

				sendData(event.getActionCommand());

			}

		}

		);

		container.add(enterField, BorderLayout.NORTH);

		displayArea = new JTextArea();
		container.add(new JScrollPane(displayArea), BorderLayout.CENTER);

		setSize(300, 150);
		setVisible(true);

	}

	public void runServer() {

		try {

			server = new ServerSocket(5000, 100);

			while (true) {

				waitForConnection();
				getStreams();
				processConnection();
				closeConnection();
				++counter;

			}

		}

		catch (EOFException eofException) {
			System.out.println("O Cliente Encerrou a Conexão.");
		}

		catch (IOException ioException) {

			ioException.printStackTrace();

		}

	}

	private void waitForConnection() throws IOException {

		displayArea.setText("Esperando pela conexão...\n");
		connection = server.accept();
		displayArea.append("Conexão " + counter + " recebida por: "
				+ connection.getInetAddress().getHostName());

	}

	private void getStreams() throws IOException {

		output = new ObjectOutputStream(connection.getOutputStream());
		output.flush();
		input = new ObjectInputStream(connection.getInputStream());
		displayArea.append("fluxos de entrada e saída recebidos...");

	}

	private void processConnection() throws IOException {

		String message = "SERVER: CONEXÃO EFETUADA COM SUCESSO!";
		output.writeObject(message);
		output.flush();

		enterField.setEnabled(true);
		do {

			try {

				message = (String) input.readObject();
				displayArea.append("\n" + message);
				displayArea.setCaretPosition(displayArea.getText().length());

			}

			catch (ClassNotFoundException classNotFoundException) {

				displayArea.append("Objeto digitado desconhecido...");

			}
		} while (!message.equals("CLIENT: TERMINATE"));

	}

	private void closeConnection() throws IOException {

		displayArea.append("O Usuário terminou a sessão.");
		enterField.setEnabled(false);
		output.close();
		input.close();
		connection.close();

	}

	private void sendData(String message) {

		try {

			output.writeObject("\nSERVER: " + message);
			output.flush();
			displayArea.append("\nSERVER: " + message);

		}

		catch (IOException ioException) {

			displayArea.append("Erro ao escrever o objeto");

		}

	}

	public static void main(String args[]) {

		Server application = new Server();
		application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		application.runServer();

	}

}