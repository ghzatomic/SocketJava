package chatJanelas;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Client extends JFrame {

	private JTextField enterField;
	private JTextArea displayArea;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private String message = "";
	private String chatServer;
	private java.net.Socket client;

	public Client(String host) {

		super("Client");

		chatServer = host;
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

	public void runClient() {

		try {

			connectToServer();
			getStreams();
			processConnection();
			closeConnection();

		}

		catch (EOFException eofException) {

			System.out.println("O Servidor encerrou a conexão");

		}

		catch (IOException ioException) {

			ioException.printStackTrace();

		}

	}

	private void getStreams() throws IOException {

		output = new ObjectOutputStream(client.getOutputStream());
		output.flush();

		input = new ObjectInputStream(client.getInputStream());

		displayArea.append("\nFluxo de entrada e saída carregados");

	}

	private void connectToServer() throws IOException {

		displayArea.setText("Esperando a conexão");

		client = new java.net.Socket(InetAddress.getByName(chatServer), 5000);

		displayArea.append("Conectado com: "
				+ client.getInetAddress().getHostName());

	}

	private void processConnection() throws IOException {

		enterField.setEnabled(true);

		do {

			try {

				message = (String) input.readObject();
				displayArea.append("\n" + message);
				displayArea.setCaretPosition(displayArea.getText().length());

			}

			catch (ClassNotFoundException classNotFoundException) {

				displayArea.append("Ojeto digitado desconhecido");

			}

		} while (!message.equals("SERVER: TERMINATE"));
	}

	private void closeConnection() throws IOException {

		displayArea.append("\nFechando a conexão...");
		output.close();
		input.close();
		client.close();

	}

	private void sendData(String message) {

		try {

			output.writeObject("CLIENT: " + message);
			output.flush();
			displayArea.append("\nCLIENT: " + message);

		}

		catch (IOException ioException) {

			displayArea.append("\nErro ao escrever o objeto");

		}

	}

	public static void main(String args[]) {

		Client application;

		if (args.length == 0) {

			application = new Client("127.0.0.1");

		} else {

			application = new Client(args[0]);

			application.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			application.runClient();

		}

	}

}