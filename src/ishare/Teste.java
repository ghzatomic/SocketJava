package ishare;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

import chat_caio.Util;

public class Teste {
	public static void main(String[] args) throws Exception {
		Socket socket = new Socket("192.168.1.1", 3307);
		BufferedReader entrada = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		PrintStream saida = new PrintStream(socket.getOutputStream());
		
		saida.write("login user caio.paulucci:condor:192.168.1.250:\r\n".getBytes());
		System.out.println(new String(Util.getData(socket.getInputStream())));
		saida.write("get session 192.168.1.250\r\n".getBytes());
		System.out.println(new String(Util.getData(socket.getInputStream())));
		
		
	}
}
