package testeproxyserver;

import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.*;

public class ProxyThread extends Thread {
	Socket incoming, outgoing;

	// Thread constructor

	ProxyThread(Socket in, Socket out) {
		incoming = in;
		outgoing = out;
	}

	// Overwritten run() method of thread -- does the data transfers

	public void run() {
		byte[] buffer = new byte[1024];
		int numberRead = 0;
		OutputStream ToClient;
		InputStream FromClient;

		try {
			ToClient = outgoing.getOutputStream();
			FromClient = incoming.getInputStream();
			while (true) {
				numberRead = FromClient.read(buffer);
				System.out.println("read " + new String(buffer));
				// buffer[numberRead] = buffer[0] = (byte)'+';

				if (numberRead == -1) {
					incoming.close();
					outgoing.close();
				}

				ToClient.write(buffer, 0, numberRead);

			}

		} catch (IOException e) {
		} catch (ArrayIndexOutOfBoundsException e) {
		}

	}

}