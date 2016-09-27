import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Daniel Sendula
 */
public class ProxyTest implements Runnable {

	private static Logger logger = LoggerFactory.getLogger(ProxyTest.class);
	
	private InetSocketAddress controlPortAddress;
	
	private Map<InetSocketAddress, ProxyPort> ports = new HashMap<InetSocketAddress, ProxyPort>();
	
    private Selector selector;
	
    private Executor executor;
    
    private boolean doRun = false;
    
    /**
     * Constructor.
     */
    public ProxyTest() {
        
    }

    /**
     * Constructor.
     * @param controlPortAddress control port address
     */
    public ProxyTest(InetSocketAddress controlPortAddress) {
        this.controlPortAddress = controlPortAddress;
    }
    
    public ProxyTest(int port) {
        this.controlPortAddress = new InetSocketAddress(port);
    }
    
    public ProxyTest(String host, int port) {
        this.controlPortAddress = new InetSocketAddress(host, port);
    }
    
    public void setControlPortAddress(InetSocketAddress controlPortAddress) {
        this.controlPortAddress = controlPortAddress;
    }

    public InetSocketAddress getControlPortAddress() {
        return controlPortAddress;
    }
    
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }
    
    public Executor getExecutor() {
        return executor;
    }
    	
	/**
	 * Starts the service.
	 */
	public synchronized void start() throws IOException {
        selector = SelectorProvider.provider().openSelector();
	    ControlPort controlPort = new ControlPort(this, selector, controlPortAddress);
	    controlPort.init();
        Executor executor = getExecutor();
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor();
        }
        doRun = true;
        executor.execute(this);
	}
	
	/**
	 * Stops the service.
	 */
	public synchronized void stop() {
	    doRun = false;
	    try {
	        wait(1000);
	    } catch (InterruptedException ignore) {
	    }
	}
	
	public synchronized void registerInetAddress(InetSocketAddress sourceSocketAddress, InetSocketAddress destinationSocketAddress) throws IOException {
		ProxyPort serverPort = ports.get(sourceSocketAddress);
		if (serverPort == null) {
			serverPort = new ProxyPort(selector, sourceSocketAddress);
			ports.put(sourceSocketAddress, serverPort);
		}
		serverPort.addDestination(destinationSocketAddress);
	}
	
	public synchronized void deregisterInetAddress(InetSocketAddress sourceSocketAddress, InetSocketAddress destinationSocketAddress) throws IOException {
        ProxyPort proxyPort = ports.get(sourceSocketAddress);
        if (proxyPort != null) {
            proxyPort.removeDestination(destinationSocketAddress);
            if (proxyPort.isEmpty()) {
                ports.remove(sourceSocketAddress);
            }
        }
	}
	
	public static InetSocketAddress stringToInetSocketAddress(String socketAddress) {
		int i = socketAddress.indexOf(':');
		if (i < 0) {
			throw new IllegalArgumentException("InetSocketAddress string must contain ':'");
		}
		String address = socketAddress.substring(0, i);
		String portString = socketAddress.substring(i + 1);
		int port = Integer.parseInt(portString);
		return new InetSocketAddress(address, port);
	}
	
	/**
	 * Main method.
	 */
    public synchronized void run() {
        try {

            int keysAdded = selector.select();
            while (doRun) {
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                if (logger.isDebugEnabled() && selectedKeys.size() == 0) {
                    logger.debug("PROXY : selecting keys..." + keysAdded + "/" + selectedKeys.size() + "(" + Thread.currentThread() + ")");
                }
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
//                    Connection c = (Connection)key.attachment();
                    
                    if (!key.isValid()) {
                        Connection c = (Connection)key.attachment();
                        if (logger.isDebugEnabled()) {
                        	logger.debug("PROXY : Key is not valid " + c + " closing channel");
                        }
                        c.close(key);
                    } else if (key.isAcceptable()) {
                        ServerSocketChannel channel = (ServerSocketChannel)key.channel();
                        PortProcessor serverPort = (PortProcessor)key.attachment();
                        serverPort.newConnection(channel);
                    } else if (key.isReadable()) {
                        Connection c = (Connection)key.attachment();
                        if (logger.isDebugEnabled()) {
                        	logger.debug("PROXY : Key is readable " + c);
                        }
                        c.read(key);
                    } else if (key.isWritable()) {
                        Connection c = (Connection)key.attachment();
                        if (logger.isDebugEnabled()) {
                        	logger.debug("PROXY : Key is writable: " + c);
                        }
                        c.write(key);
                    } else if (key.isConnectable()) {
                        SocketChannel socketChannel = (SocketChannel)key.channel();
                        socketChannel.finishConnect();
                        key.interestOps(SelectionKey.OP_READ);
                    } else {
                        Connection c = (Connection)key.attachment();
                        if (logger.isDebugEnabled()) {
                        	logger.debug("PROXY : No idea what! " + c);
                        }
                    }
                }
                
                keysAdded = selector.select();
            }

            notifyAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    protected static interface PortProcessor {
    	
    	void newConnection(ServerSocketChannel channel) throws IOException;
    	
    }
    
    protected static class ProxyPort implements PortProcessor {
    	
    	private Selector selector;
    	private InetSocketAddress socketAddress;
    	private ServerSocketChannel serverSocketChannel;
    	private List<InetSocketAddress> destinations = new ArrayList<InetSocketAddress>();
    	private int current = 0;
    	private int connections = 0;
    	
    	public ProxyPort(Selector selector, InetSocketAddress socketAddress) {
    		this.selector = selector;
    		this.socketAddress = socketAddress;
    	}
    	
    	public void addDestination(InetSocketAddress destination) throws IOException {
    	    if (destinations.isEmpty()) {
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(false);

                serverSocketChannel.socket().bind(socketAddress);

                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, this);
    	    }
    		destinations.add(destination);
    	}
    	
    	public void removeDestination(InetSocketAddress destination) throws IOException {
    		destinations.remove(destination);
    		if (current >= destinations.size()) {
    			current = 0;
    		}
    		if (destinations.isEmpty()) {
    		    serverSocketChannel.close();
    		}
    	}

    	public boolean isEmpty() {
    	    return destinations.isEmpty();
    	}
    	
    	public synchronized void newConnection(ServerSocketChannel channel) throws IOException {
    		if (destinations.size() > 0) {
	            SocketChannel outboundChannel = SocketChannel.open();
	            outboundChannel.configureBlocking(false);

	            InetSocketAddress serverSocketAddress = nextDestination();
	
	            Socket inbound = channel.accept().socket();
	            SocketChannel inboundChannel = inbound.getChannel();
	            inboundChannel.configureBlocking(false);
	            
	            Connection outConnection = new ProxyConnection(outboundChannel, inboundChannel, false);
	            Connection inConnection = new ProxyConnection(inboundChannel, outboundChannel, true);
	            
	            outboundChannel.register(selector, SelectionKey.OP_CONNECT, outConnection);
	            outboundChannel.connect(serverSocketAddress);
	
	            inboundChannel.register(selector, SelectionKey.OP_READ, inConnection);
	            connections++;
    		} else {
    			// TODO
    		}
    	}
    	
    	public synchronized void closeConnection() {
    		connections = connections - 1;
    	}
    	
    	public synchronized InetSocketAddress nextDestination() {
    		if (current >= destinations.size()) {
    			current = 0;
    		}
    		InetSocketAddress ret = destinations.get(current);
    		current++;
    		if (current >= destinations.size()) {
    			current = 0;
    		}
    		return ret;
    	}

    }

    public interface Connection {
        void read(SelectionKey key) throws IOException;
        void write(SelectionKey key) throws IOException;
        void close(SelectionKey key) throws IOException;
    }
    
    public static class ProxyConnection implements Connection {
        private static int counter = 1;
        private ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
        private SocketChannel in;
        private SocketChannel out;
        private boolean inbound;
        private int count;
        
        public ProxyConnection(SocketChannel in, SocketChannel out, boolean inbound) {
            this.in = in;
            this.out = out;
            this.inbound = inbound;
            count = counter;
            if (inbound) {
                counter++;
            }
        }
        
        public void read(SelectionKey key) throws IOException {
            if (out.isConnected()) {
                buffer.clear();
                int size = in.read(buffer);
                if (size == -1) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("PROXY : Closing channel " + this);
                    }
                    key.channel().close();
                    key.cancel();
                    out.close();
                    out.socket().close();
                } else if (size == 0) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("PROXY : read " + this + " "+ buffer.position() + "? What now?");
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("PROXY : read " + this + " " + buffer.position());
                    }
                    buffer.flip();
                    out.write(buffer);
                }
            }
        }
        
        public void write(SelectionKey key) throws IOException {
            
        }
 
        public void close(SelectionKey key) throws IOException {
            in.close();
        }
        
        public String toString() {
            if (inbound) {
                return "Inbound(" + count + ")";
            } else {
                return "Outbound(" + count + ")";
            }
        }
    }
    
    protected static class ControlPort implements PortProcessor {

        private Selector selector;
        private InetSocketAddress socketAddress;
        private ServerSocketChannel serverSocketChannel;
        private ProxyTest proxy;

        public ControlPort(ProxyTest proxy, Selector selector, InetSocketAddress socketAddress) {
            this.proxy = proxy;
            this.selector = selector;
            this.socketAddress = socketAddress;
        }
        
        public void init() throws IOException {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);

            serverSocketChannel.socket().bind(socketAddress);

            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, this);
        }
        
        public void newConnection(ServerSocketChannel channel) throws IOException {
            Socket socket = channel.accept().socket();

            SocketChannel socketChannel = socket.getChannel();
            socketChannel.configureBlocking(false);
            
            Connection connection = new ControlConnection(proxy, socketChannel); 
            socketChannel.register(selector, SelectionKey.OP_READ, connection);
        }
        
    }

    protected static class ControlConnection implements Connection {

        private ByteBuffer buffer = ByteBuffer.allocate(1024);
        private BufferedReader in;
        private PrintWriter out;
        private SocketChannel channel;
        private InetSocketAddress sourceSocketAddress;
        private InetSocketAddress destinationSocketAddress;
        private StringBuffer inputLine = new StringBuffer();
        private ProxyTest proxy;
        
        public ControlConnection(ProxyTest proxy, SocketChannel channel) {
            this.proxy = proxy;
            this.channel = channel;
            // in = new BufferedReader(new InputStreamReader(inbound.getInputStream()));
            // out = new PrintWriter(new OutputStreamWriter(inbound.getOutputStream()));
        }
        
        public void close(SelectionKey key) throws IOException {
            proxy.deregisterInetAddress(sourceSocketAddress, destinationSocketAddress);
        }

        public void read(SelectionKey key) throws IOException {
            buffer.clear();
            int size = channel.read(buffer);
            if (destinationSocketAddress == null) {
                buffer.flip();
                while (size > 0) {
                    char c = (char)buffer.get();
                    if (c != '\n') {
                        if (c != '\r') {
                            inputLine.append(c);
                        }
                    } else {
                        if (sourceSocketAddress == null) {
                            sourceSocketAddress = stringToInetSocketAddress(inputLine.toString());
                            inputLine = new StringBuffer();
                        } else {
                            destinationSocketAddress = stringToInetSocketAddress(inputLine.toString());
                            proxy.registerInetAddress(sourceSocketAddress, destinationSocketAddress);
                        }
                    }
                    size--;
                }
            }
        }

        public void write(SelectionKey key) throws IOException {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    public static void main(String[] args) throws Exception {
        System.out.println("Creating proxy...");
        ProxyTest proxy = new ProxyTest(8044);
        proxy.start();
        System.out.println("Proxy created.");
        
        Thread.sleep(1000);

        System.out.println("Creating server socket...");
        ServerSocket serverSocket = new ServerSocket(9999);
        System.out.println("Server socket created,");
        
        System.out.println("Creating control socket...");
        Socket socket = new Socket("localhost", 8044);
        System.out.println("Control socket created.");
        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        System.out.println("Sending control data...");
        out.println("localhost:8888");
        out.println("localhost:9999");
        out.flush();
        System.out.println("Control data sent.");
        Thread.sleep(1000);

        System.out.println("Creating client socket...");
        Socket testClientSocket = new Socket("localhost", 8888);
        System.out.println("Client socket created.");
        PrintWriter testClientOut = new PrintWriter(new OutputStreamWriter(testClientSocket.getOutputStream()));
        System.out.println("Sending some data...");
        testClientOut.println("Something!");
        testClientOut.flush();
        System.out.println("Data sent.");
        
        System.out.println("Accepting connection...");
        Socket testServerSocket = serverSocket.accept();
        System.out.println("Connection accepted.");
        BufferedReader testServerIn = new BufferedReader(new InputStreamReader(testServerSocket.getInputStream()));
        System.out.println("Receiving line...");
        String line = testServerIn.readLine();
        System.out.println("Got line: " + line);
        System.out.println("Line received.");
        
        System.exit(0);
    }
}