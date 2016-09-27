import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;


public class SocketTeste1 {

	private Socket socketEntradaFromMSN;
	
	private Socket socketSaidaToMSN;
	
	private Socket socketEntradaFromServidor;
	
	private Socket socketSaidaToServidor;
	
	private DataOutputStream outToMsn;
	private DataOutputStream outToServidor;
	
	public static void main(String[] args) {
		new SocketTeste1().init();
	}

	public void abreConexaoDeEntradaDeDadosFromMSN() throws UnknownHostException, IOException{
		System.out.println("Aguardando conexao do msn...");
		socketEntradaFromMSN = new ServerSocket(1081).accept();
		System.out.println("MSN cliente entrou !");
		listenerMSNToServidor(socketEntradaFromMSN.getInputStream());
	}
	
	public void abreConexaoDeSaidaDeDadosToServidor() throws UnknownHostException, IOException{
		socketSaidaToServidor = new Socket("192.168.1.1",1080);
		outToServidor = new DataOutputStream(getSocketSaidaToServidor().getOutputStream());
	}
	
	public void abreConexaoDeEntradaDeDadosFromServidor() throws UnknownHostException, IOException{
		System.out.println("Aguardando retorno do servidor...");
		socketEntradaFromServidor = new ServerSocket(1080).accept();
		System.out.println("Servidor Enviou um sinal !");
		listenerServidorToMsn(socketEntradaFromServidor.getInputStream());
	}
	
	public void abreConexaoDeSaidaDeDadosToMSN() throws UnknownHostException, IOException{
		socketSaidaToMSN = new Socket("127.0.0.1",1081);
		outToMsn = new DataOutputStream(getSocketSaidaToMSN().getOutputStream());
	}
	
	public void init(){
		try {
			abreConexaoDeSaidaDeDadosToServidor();
			abreConexaoDeEntradaDeDadosFromMSN();
			abreConexaoDeEntradaDeDadosFromServidor();
			//in = new DataInputStream(getSocketEntrada().getInputStream());
			
			//out.write(in.read());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	private void listenerMSNToServidor(final InputStream inputStream)
    {        
        new Thread(new Runnable() {
            DataInputStream in = new DataInputStream(inputStream);
            public void run() {        
            	System.out.println("Chegou alguma coisa do msn para o servidor !");
                try{
                	//System.out.println(in.read());
                	while(true){
	                	if (outToServidor != null){
	                		int read = in.read();
	                		System.out.println("Enviou alguma coisa do msn para o servidor : "+read);
	                		outToServidor.write(read);
	                	}else{
	                		System.out.println("Out do msn ta null");
	                	}
                	}
                } catch (IOException e){
                }
            }
        }).start();
    }
	
	private void listenerServidorToMsn(final InputStream inputStream)
    {        
        new Thread(new Runnable() {
            DataInputStream in = new DataInputStream(inputStream);
            public void run() {        
            	System.out.println("Chegou alguma coisa do servidor para o msn!");
                try{
                	//System.out.println(in.read());
                	while(true){
	                	if (outToMsn != null){
	                		System.out.println("Enviou alguma coisa do servidor para o msn");
	                		outToMsn.write(in.read());
	                	}else{
	                		System.out.println("Out do servidor ta null");
	                	}
                	}
                } catch (IOException e){
                }
            }
        }).start();
    }

	public Socket getSocketEntradaFromMSN() {
		return socketEntradaFromMSN;
	}

	public void setSocketEntradaFromMSN(Socket socketEntradaFromMSN) {
		this.socketEntradaFromMSN = socketEntradaFromMSN;
	}

	public Socket getSocketSaidaToMSN() {
		return socketSaidaToMSN;
	}

	public void setSocketSaidaToMSN(Socket socketSaidaToMSN) {
		this.socketSaidaToMSN = socketSaidaToMSN;
	}

	public Socket getSocketEntradaFromServidor() {
		return socketEntradaFromServidor;
	}

	public void setSocketEntradaFromServidor(Socket socketEntradaFromServidor) {
		this.socketEntradaFromServidor = socketEntradaFromServidor;
	}

	public Socket getSocketSaidaToServidor() {
		return socketSaidaToServidor;
	}

	public void setSocketSaidaToServidor(Socket socketSaidaToServidor) {
		this.socketSaidaToServidor = socketSaidaToServidor;
	}

	public DataOutputStream getOutToMsn() {
		return outToMsn;
	}

	public void setOutToMsn(DataOutputStream outToMsn) {
		this.outToMsn = outToMsn;
	}

	public DataOutputStream getOutToServidor() {
		return outToServidor;
	}

	public void setOutToServidor(DataOutputStream outToServidor) {
		this.outToServidor = outToServidor;
	}

}
