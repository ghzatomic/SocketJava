import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.*;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server<Parametr> {

	private Calendar current;
	private DateFormat dformat;
	private int localport = 0;
	private int remoteport = 80;
	private String remoteAddress = "192.168.1.1";
	private Proxy proxy;
	private ServerSocketChannel servsocketchannel;
	private boolean local = false;

	public Server(int port) {
		setServerPort(port);
		openServerPort();
		checkAndSetRemoteProxyServer();
		dformat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		current = Calendar.getInstance();
		createServerSocketAndTalk();
	}

	public void setServerPort(int port) {
		this.localport = port;
	}

	public void setProxyAddressAndPort(int port, String address) {
		this.remoteport = port;
		this.remoteAddress = address;
	}

	public void openServerPort() {
		try {
			servsocketchannel = ServerSocketChannel.open();
			servsocketchannel.socket().bind(
					new java.net.InetSocketAddress(localport));
		} catch (BindException exc) {
			System.out
					.println("Server isn't started, local port is blocked or used, check firewall or set new port");

		} catch (IOException e) {
			System.out
					.println("Server isn't started, local port is blocked or used, check firewall or set new port");
		}
	}

	public void checkAndSetRemoteProxyServer() {
		boolean ERROR = false;
		Socket proxysocketchannel = null;
		try {
			proxysocketchannel = new Socket(remoteAddress, remoteport);
		} catch (UnknownHostException ex) {
			System.out
					.println("Proxy server address is probably offline, please provide new server address --- RUNNING LOCAL PROXY MODE");
			ERROR = true;
		} catch (IOException exc) {
			System.out
					.println("Connecting to the proxy server failed, please review address and port --- RUNNING LOCAL PROXY MODE");
			ERROR = true;
		}

		if (!ERROR) {
			System.out.println("Proxy state - online");
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
					remoteAddress, remoteport));
			System.out
					.println("Initialization successful - waiting for clients ");
		} else {
			System.out
					.println("Problem with proxy connection, check your settings or set new proxy address --- RUNNING LOCAL PROXY MODE");
			System.out
					.println("Proxy local mode initialization successful - waiting for clients ");
			local = true;
		}
	}

	public void createServerSocketAndTalk() {
		try {
			while (servsocketchannel.isOpen()) {
				Runnable r = new ClientHandler(servsocketchannel.accept());
				Thread t = new Thread(r);
				t.start();
			}
		} catch (NotYetBoundException exc) {
			System.out
					.println("Server not started local port is already used, please choose new local port");
		} catch (AsynchronousCloseException ex) {
			System.out.println("Server Closed");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class ClientHandler implements Runnable {

		SocketChannel client = null;

		private static final int BUFFER_SIZE = 32768;

		public ClientHandler(SocketChannel client) {
			this.client = client;
		}

		@Override
		public void run() {
			letsTalk();
			try {
				this.finalize();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		public void letsTalk() {

			while (true) {

				if (client.socket().isClosed()) {
					break;
				}

				BufferedReader clientin = null;
				DataOutputStream clientout = null;

				try {
					clientin = new BufferedReader(new InputStreamReader(client
							.socket().getInputStream()));
					clientout = new DataOutputStream(client.socket()
							.getOutputStream());
				} catch (IOException e) {
					System.out.println("Error with client connection");
					break;
				}

				String inputline;
				String url = "";

				try {
					while ((inputline = clientin.readLine()) != null) {
						System.out.println(inputline);
						StringTokenizer readerclient = new StringTokenizer(
								inputline);
						if (readerclient.nextToken().equals("GET")) {
							url = readerclient.nextToken();
							System.out.println("Requested URL:  " + url);
							break;
						}
						if (readerclient.nextToken().equals("POST")) {
							System.out.println("POST");
						}
					}
				} catch (IOException e) {
					System.out.println("URL REQUEST ERROR");
					e.printStackTrace();
				} catch (NullPointerException exc) {

				}

				try {
					URL reqURL = new URL(url);
					URLConnection connection = null;
					if (local) {
						connection = reqURL.openConnection();
					} else {
						connection = reqURL.openConnection(proxy);
					}
					connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.5; Windows NT 5.0; H010818)");
					connection.setDoInput(true);
					connection.setDoOutput(false);

					InputStream is = null;
					is = connection.getInputStream();

					byte by[] = new byte[BUFFER_SIZE];
					int index = is.read(by, 0, BUFFER_SIZE);

					while (index != -1) {
						clientout.write(by, 0, index);
						index = is.read(by, 0, BUFFER_SIZE);
					}

					clientout.flush();

				} catch (Exception ex) {

				}

				try {
					clientin.close();
					clientout.close();
					// client.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}

	}
	
	public static void main(String[] args) {
		//Server<Parametr> server = new Server<Parametr>(1080);
	}
	
}
