import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Set;


public class Gateway {

	static String encoding_Msg = "ISO-8859-1";

	public static void main(String[] args) throws IOException {
		// Set up Server Socket and bind to the port 8000
		ServerSocketChannel server = ServerSocketChannel.open();
		SocketAddress endpoint = new InetSocketAddress(1081);
		server.socket().bind(endpoint);

		server.configureBlocking(false);

		// Set up selector so we can run with a single thread but multiplex
		// between 2 channels
		Selector selector = Selector.open();
		server.register(selector, SelectionKey.OP_ACCEPT);

		ByteBuffer buffer = ByteBuffer.allocate(1024);

		while (true) {
			// block until data comes in
			selector.select();

			Set<SelectionKey> keys = selector.selectedKeys();

			for (SelectionKey key : keys) {
				if (!key.isValid()) {
					// not valid or writable so skip
					continue;
				}

				if (key.isAcceptable()) {
					// Accept socket channel for client connection
					ServerSocketChannel channel = (ServerSocketChannel) key
							.channel();
					SocketChannel accept = channel.accept();
					setupConnection(selector, accept);
				} else if (key.isReadable()) {
					try {
						// Read into the buffer from the socket and then write
						// the buffer into the attached socket.
						SocketChannel recv = (SocketChannel) key.channel();
						SocketChannel send = (SocketChannel) key.attachment();
						int numBytesRead = recv.read(buffer);
						buffer.flip();
						String message = new String(buffer.array(), "ISO-8859-1");
						System.out.println(message);
						send.write(buffer);
						buffer.rewind();
					} catch (IOException e) {
						e.printStackTrace();

						// Close sockets
						if (key.channel() != null)
							key.channel().close();
						if (key.attachment() != null)
							((SocketChannel) key.attachment()).close();
					}
				}
			}

			// Clear keys for next select
			keys.clear();
		}
	}

	public static void setupConnection(Selector selector, SocketChannel client)
			throws IOException {
		// Connect to the remote server
		SocketAddress address = new InetSocketAddress("192.168.1.1", 1080);
		SocketChannel remote = SocketChannel.open(address);

		// Make sockets non-blocking (should be better performance)
		client.configureBlocking(false);
		remote.configureBlocking(false);

		client.register(selector, SelectionKey.OP_READ, remote);
		remote.register(selector, SelectionKey.OP_READ, client);
	}

	/**
	 * 
	 * @param in
	 * @return
	 */
	public static String readString(ByteBuffer in) {
		try {
			Charset charset = Charset.defaultCharset();
			CharsetDecoder decoder = charset.newDecoder();
			ByteBuffer buf = ByteBuffer.allocate(in.capacity());
			byte b;
			b = in.get();
			while (b != 0) {
				buf.put(b);
				b = in.get();
			}
			buf.rewind();
			return decoder.decode(buf).toString();
		} catch (CharacterCodingException e) {
			System.out.println("Communicator =>");
			System.out.println("\t Erro de codificacao. \n");
			System.exit(1);
			return null;
		}
	}

}
