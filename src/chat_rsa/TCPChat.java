package chat_rsa;

import java.lang.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;
import java.net.*;

////////////////////////////////////////////////////////////////////

// Action adapter for easy event-listener coding
public class TCPChat implements Runnable {
	// Connect status constants
	public final static int NULL = 0;
	public final static int DISCONNECTED = 1;
	public final static int DISCONNECTING = 2;
	public final static int BEGIN_CONNECT = 3;
	public final static int CONNECTED = 4;

	// Other constants
	public final static String statusMessages[] = {
			" Erro! N�o foi poss�vel conectar!", " Desconectado",
			" Desconectando...", " Conectando...", " Conectado" };
	public final static TCPChat tcpObj = new TCPChat();
	public final static String END_CHAT_SESSION = new Character((char) 0)
			.toString(); // Indicates the end of a session

	// Connection atate info
	public static String hostIP = "localhost";
	public static int port = 1234;
	public static int connectionStatus = DISCONNECTED;
	public static boolean isHost = true;
	public static String statusString = statusMessages[connectionStatus];
	public static StringBuffer toAppend = new StringBuffer("");
	public static StringBuffer toAppend2 = new StringBuffer("");
	public static StringBuffer toSend = new StringBuffer("");

	// Various GUI components and info
	public static JFrame mainFrame = null;
	public static JTextArea chatText = null;
	public static JTextField chatLine = null;
	public static JPanel statusBar = null;
	public static JLabel statusField = null;
	public static JTextField statusColor = null;
	public static JTextField ipField = null;
	public static JTextField portField = null;
	public static JTextField nameField = null;
	public static JRadioButton hostOption = null;
	public static JRadioButton guestOption = null;
	public static JButton connectButton = null;
	public static JButton disconnectButton = null;

	// TCP Components
	public static ServerSocket hostServer = null;
	public static Socket socket = null;
	public static BufferedReader in = null;
	public static PrintWriter out = null;
	private static JTextArea chatText2;
	private static JTextField chatLine2;
	private static PrivateKey chave_privada;
	private static PublicKey chave_publica;
	private static byte[] chave_simetrica;

	// ///////////////////////////////////////////////////////////////

	private static JPanel initOptionsPane() {
		JPanel pane = null;
		ActionAdapter buttonListener = null;

		// Create an options pane
		JPanel optionsPane = new JPanel(new GridLayout(5, 1));

		// IP address input
		pane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		pane.add(new JLabel("Host IP:"));
		ipField = new JTextField(10);
		ipField.setText(hostIP);
		ipField.setEnabled(false);
		ipField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				ipField.selectAll();
				// Should be editable only when disconnected
				if (connectionStatus != DISCONNECTED) {
					changeStatusNTS(NULL, true);
				} else {
					hostIP = ipField.getText();
				}
			}
		});
		pane.add(ipField);

		optionsPane.add(pane);

		// Port input
		pane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		pane.add(new JLabel("Porta:"));
		portField = new JTextField(10);
		portField.setEditable(true);
		portField.setText((new Integer(port)).toString());
		portField.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				// should be editable only when disconnected
				if (connectionStatus != DISCONNECTED) {
					changeStatusNTS(NULL, true);
				} else {
					int temp;
					try {
						temp = Integer.parseInt(portField.getText());
						port = temp;
					} catch (NumberFormatException nfe) {
						portField.setText((new Integer(port)).toString());
						mainFrame.repaint();
					}
				}
			}
		});
		pane.add(portField);
		optionsPane.add(pane);

		// Host/guest option
		buttonListener = new ActionAdapter() {
			public void actionPerformed(ActionEvent e) {
				if (connectionStatus != DISCONNECTED) {
					changeStatusNTS(NULL, true);
				} else {
					isHost = e.getActionCommand().equals("host");

					// Cannot supply host IP if host option is chosen
					if (isHost) {
						ipField.setEnabled(false);
						ipField.setText("localhost");
						hostIP = "localhost";
					} else {
						ipField.setEnabled(true);
					}
				}
			}
		};
		ButtonGroup bg = new ButtonGroup();
		hostOption = new JRadioButton("Servidor", true);
		hostOption.setMnemonic(KeyEvent.VK_H);
		hostOption.setActionCommand("host");
		hostOption.addActionListener(buttonListener);
		guestOption = new JRadioButton("Cliente", false);
		guestOption.setMnemonic(KeyEvent.VK_G);
		guestOption.setActionCommand("guest");
		guestOption.addActionListener(buttonListener);
		bg.add(hostOption);
		bg.add(guestOption);
		pane = new JPanel(new GridLayout(1, 2));
		pane.add(hostOption);
		pane.add(guestOption);
		optionsPane.add(pane);

		// Connect/disconnect buttons
		JPanel buttonPane = new JPanel(new GridLayout(1, 2));
		buttonListener = new ActionAdapter() {
			public void actionPerformed(ActionEvent e) {
				// Request a connection initiation
				if (e.getActionCommand().equals("connect")) {
					changeStatusNTS(BEGIN_CONNECT, true);
				}
				// Disconnect
				else {
					changeStatusNTS(DISCONNECTING, true);
				}
			}
		};
		connectButton = new JButton("Conectar");
		connectButton.setMnemonic(KeyEvent.VK_C);
		connectButton.setActionCommand("connect");
		connectButton.addActionListener(buttonListener);
		connectButton.setEnabled(true);
		disconnectButton = new JButton("Desconectar");
		disconnectButton.setMnemonic(KeyEvent.VK_D);
		disconnectButton.setActionCommand("disconnect");
		disconnectButton.addActionListener(buttonListener);
		disconnectButton.setEnabled(false);
		buttonPane.add(connectButton);
		buttonPane.add(disconnectButton);
		optionsPane.add(buttonPane);

		return optionsPane;
	}

	// ///////////////////////////////////////////////////////////////

	// Initialize all the GUI components and display the frame
	private static void initGUI() {
		// Set up the status bar
		statusField = new JLabel();
		statusField.setText(statusMessages[DISCONNECTED]);
		statusColor = new JTextField(1);
		statusColor.setBackground(Color.red);
		statusColor.setEditable(false);
		statusBar = new JPanel(new BorderLayout());
		statusBar.add(statusColor, BorderLayout.WEST);
		statusBar.add(statusField, BorderLayout.CENTER);

		// Set up the options pane
		JPanel optionsPane = initOptionsPane();

		// Set up the chat pane
		JPanel chatPane = new JPanel(new BorderLayout());
		chatText = new JTextArea(10, 20);
		chatText.setLineWrap(true);
		chatText.setEditable(false);
		chatText.setForeground(Color.blue);
		JScrollPane chatTextPane = new JScrollPane(chatText,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		chatLine = new JTextField();
		chatLine.setEnabled(true);
		chatLine.addActionListener(new ActionAdapter() {
			public void actionPerformed(ActionEvent e) {
				String s = chatLine.getText();
				if (!s.equals("")) {
					appendToChatBox("ENVIADO: " + s + "\n");
					chatLine.selectAll();

					// Send the string
					sendString(s);
				}
			}
		});
		chatPane.add(chatLine, BorderLayout.SOUTH);
		chatPane.add(chatTextPane, BorderLayout.WEST);
		chatPane.setPreferredSize(new Dimension(200, 200));

		chatText2 = new JTextArea(10, 20);
		chatText2.setLineWrap(true);
		chatText2.setEditable(false);
		chatText2.setForeground(Color.blue);
		JScrollPane chatTextPane2 = new JScrollPane(chatText2,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		chatLine = new JTextField();
		chatLine.setEnabled(false);
		chatLine.addActionListener(new ActionAdapter() {
			public void actionPerformed(ActionEvent e) {
				String s = chatLine.getText();
				if (!s.equals("")) {
					appendToChatBox("ENVIADO: " + s + "\n");
					chatLine.selectAll();
					// Send the string
					sendString(s);
				}
			}
		});

		chatPane.add(chatLine, BorderLayout.SOUTH);
		chatPane.add(chatTextPane2, BorderLayout.EAST);
		chatPane.setPreferredSize(new Dimension(480, 200));

		// Set up the main pane
		JPanel mainPane = new JPanel(new BorderLayout());
		mainPane.add(statusBar, BorderLayout.SOUTH);
		mainPane.add(optionsPane, BorderLayout.WEST);
		mainPane.add(chatPane, BorderLayout.CENTER);

		// Set up the main frame
		mainFrame = new JFrame("RSA MESSENGER");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setContentPane(mainPane);
		mainFrame.setSize(mainFrame.getPreferredSize());
		mainFrame.setLocation(200, 200);
		mainFrame.pack();
		mainFrame.setVisible(true);

	}

	// ///////////////////////////////////////////////////////////////

	// The thread-safe way to change the GUI components while
	// changing state
	private static void changeStatusTS(int newConnectStatus, boolean noError) {
		// Change state if valid state
		if (newConnectStatus != NULL) {
			connectionStatus = newConnectStatus;
		}

		// If there is no error, display the appropriate status message
		if (noError) {
			statusString = statusMessages[connectionStatus];
		}
		// Otherwise, display error message
		else {
			statusString = statusMessages[NULL];
		}

		// Call the run() routine (Runnable interface) on the
		// error-handling and GUI-update thread
		SwingUtilities.invokeLater(tcpObj);
	}

	// ///////////////////////////////////////////////////////////////

	// The non-thread-safe way to change the GUI components while
	// changing state
	private static void changeStatusNTS(int newConnectStatus, boolean noError) {
		// Change state if valid state
		if (newConnectStatus != NULL) {
			connectionStatus = newConnectStatus;
		}

		// If there is no error, display the appropriate status message
		if (noError) {
			statusString = statusMessages[connectionStatus];
		}
		// Otherwise, display error message
		else {
			statusString = statusMessages[NULL];
		}

		// Call the run() routine (Runnable interface) on the
		// current thread
		tcpObj.run();
	}

	// ///////////////////////////////////////////////////////////////

	// Thread-safe way to append to the chat box
	private static void appendToChatBox(String s) {
		synchronized (toAppend) {
			toAppend.append(s);
		}
	}

	// ///////////////////////////////////////////////////////////////

	// Add text to send-buffer
	private static void sendString(String s) {
		synchronized (toSend) {
			toSend.append(s + "\n");
			System.out.println("enviando");
		}
	}

	// ///////////////////////////////////////////////////////////////

	// Cleanup for disconnect
	private static void cleanUp() {
		try {
			if (hostServer != null) {
				hostServer.close();
				hostServer = null;
			}
		} catch (IOException e) {
			hostServer = null;
		}

		try {
			if (socket != null) {
				socket.close();
				socket = null;
			}
		} catch (IOException e) {
			socket = null;
		}

		try {
			if (in != null) {
				in.close();
				in = null;
			}
		} catch (IOException e) {
			in = null;
		}

		if (out != null) {
			out.close();
			out = null;
		}
	}

	private static byte[][] cifrado;

	// ///////////////////////////////////////////////////////////////

	// Checks the current state and sets the enables/disables
	// accordingly
	public void run() {
		switch (connectionStatus) {
		case DISCONNECTED:
			connectButton.setEnabled(true);
			disconnectButton.setEnabled(false);
			ipField.setEnabled(true);
			portField.setEnabled(true);
			hostOption.setEnabled(true);
			guestOption.setEnabled(true);
			chatLine.setText("");
			chatLine.setEnabled(false);
			statusColor.setBackground(Color.red);
			break;

		case DISCONNECTING:
			connectButton.setEnabled(false);
			disconnectButton.setEnabled(false);
			ipField.setEnabled(false);
			portField.setEnabled(false);
			hostOption.setEnabled(false);
			guestOption.setEnabled(false);
			chatLine.setEnabled(false);
			statusColor.setBackground(Color.orange);
			break;

		case CONNECTED:
			connectButton.setEnabled(false);
			disconnectButton.setEnabled(true);
			ipField.setEnabled(false);
			portField.setEnabled(false);
			hostOption.setEnabled(false);
			guestOption.setEnabled(false);
			chatLine.setEnabled(true);
			statusColor.setBackground(Color.green);
			break;

		case BEGIN_CONNECT:
			connectButton.setEnabled(false);
			disconnectButton.setEnabled(false);
			ipField.setEnabled(false);
			portField.setEnabled(false);
			hostOption.setEnabled(false);
			guestOption.setEnabled(false);
			chatLine.setEnabled(false);
			chatLine.grabFocus();
			statusColor.setBackground(Color.orange);
			break;
		}

		// Make sure that the button/text field states are consistent
		// with the internal states

		Cifrador cf = new Cifrador();

		ipField.setText(hostIP);
		portField.setText((new Integer(port)).toString());
		hostOption.setSelected(isHost);
		guestOption.setSelected(!isHost);
		statusField.setText(statusString);
		chatText.append(toAppend.toString() + "\n");
		try {
			cifrado = cf.cifra(chave_publica, toAppend.toString().getBytes());
			chatText2.append("CRIPTOGRAFADO: " + new String(cifrado[0]) + "\n");
		} catch (NoSuchAlgorithmException ex) {
			Logger.getLogger(TCPChat.class.getName()).log(Level.SEVERE, null,
					ex);
		} catch (NoSuchPaddingException ex) {
			Logger.getLogger(TCPChat.class.getName()).log(Level.SEVERE, null,
					ex);
		} catch (InvalidKeyException ex) {
			Logger.getLogger(TCPChat.class.getName()).log(Level.SEVERE, null,
					ex);
		} catch (IllegalBlockSizeException ex) {
			Logger.getLogger(TCPChat.class.getName()).log(Level.SEVERE, null,
					ex);
		} catch (BadPaddingException ex) {
			Logger.getLogger(TCPChat.class.getName()).log(Level.SEVERE, null,
					ex);
		} catch (InvalidAlgorithmParameterException ex) {
			Logger.getLogger(TCPChat.class.getName()).log(Level.SEVERE, null,
					ex);
		}

		toAppend.setLength(0);

		mainFrame.repaint();
	}

	// ///////////////////////////////////////////////////////////////

	public static byte[] convertStringToByteArray(String s) {

		byte[] theByteArray = s.getBytes();

		return theByteArray;
	}

	public static final byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16),
				(byte) (value >>> 8), (byte) value };
	}

	// The main procedure
	public static void main(String args[]) {
		String s = null;
		Integer is = 0;

		initGUI();
		CarregadorChavePrivada carrega_priv = new CarregadorChavePrivada();
		CarregadorChavePublica carrega_publ = new CarregadorChavePublica();
		try {
			chave_privada = carrega_priv.carregaChavePrivada(new File(
					"chaves\\privada.key"));
			chave_publica = carrega_publ.carregaChavePublica(new File(
					"chaves\\publica.key"));
		} catch (IOException ex) {
			Logger.getLogger(TCPChat.class.getName()).log(Level.SEVERE, null,
					ex);
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(TCPChat.class.getName()).log(Level.SEVERE, null,
					ex);
		}
		chave_simetrica = null;

		while (true) {
			try { // Poll every ~10 ms
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}

			switch (connectionStatus) {
			case BEGIN_CONNECT:
				try {
					// Try to set up a server if host
					if (isHost) {
						hostServer = new ServerSocket(port);
						socket = hostServer.accept();
					}

					// If guest, try to connect to the server
					else {
						socket = new Socket(hostIP, port);
					}

					in = new BufferedReader(new InputStreamReader(
							socket.getInputStream()));
					out = new PrintWriter(socket.getOutputStream(), true);
					changeStatusTS(CONNECTED, true);
				}
				// If error, clean up and output an error message
				catch (IOException e) {
					cleanUp();
					changeStatusTS(DISCONNECTED, false);
				}
				break;

			case CONNECTED:
				try {
					// Send data

					if (toSend.length() != 0) {
						out.print(cifrado[0].toString());
						out.flush();
						toSend.setLength(0);
						changeStatusTS(NULL, true);
					}
					// Receive data
					if (in.ready()) {
						s = in.readLine();

						if ((s != null) && (s.length() != 0)) {
							// Check if it is the end of a trasmission
							if (s.equals(END_CHAT_SESSION)) {
								changeStatusTS(DISCONNECTING, true);
							}
							// Otherwise, receive what text
							else {
								appendToChatBox("INCOMING: " + s + "\n");
								changeStatusTS(NULL, true);
							}
						}
					}
				} catch (IOException e) {
					cleanUp();
					changeStatusTS(DISCONNECTED, false);
				}
				break;

			case DISCONNECTING:
				// Tell other chatter to disconnect as well
				out.print(END_CHAT_SESSION);
				out.flush();

				// Clean up (close all streams/sockets)
				cleanUp();
				changeStatusTS(DISCONNECTED, true);
				break;

			default:
				break; // do nothing
			}
		}
	}
}