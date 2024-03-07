package bgu.spl.net.impl.tftp;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import bgu.spl.net.impl.tftp.TftpProtocol;
import bgu.spl.net.impl.tftp.TftpEncoderDecoder;

	
	public class TftpClient {
	
		public static void main(String[] args) throws IOException {

	
			if (args.length == 0) {
				args = new String[]{"localhost", "hello"};
			}
	
			if (args.length < 2) {
				System.out.println("you must supply two arguments: host, message");
				System.exit(1);
			}

			TftpProtocol protocol = new TftpProtocol();
			TftpEncoderDecoder encdec = new TftpEncoderDecoder();

			//BufferedReader and BufferedWriter automatically using UTF-8 encoding
			try (Socket sock = new Socket(args[0], 7777);
					BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
					BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))) {
				
				System.out.println("sending message to server");
				Object lock = new Object();

				Thread KeyboardThread = new Thread(() -> {
					BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
					while (true) {
						try {
							String line = keyboard.readLine();	
							synchronized (lock) {
								out.write(new String(encdec.encode(protocol.creatRequest(line))));
								out.newLine();
								out.flush();
							}
							}
						catch (IOException e) {
							e.printStackTrace();
						}
						lock.notify();
						try {
							lock.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				});
				KeyboardThread.start();

				Thread ListenThread = new Thread(() -> {
					while (true) {
						try {
							String line = in.readLine();
							byte[] lineToByte = line.getBytes();
							for (int i = 0; i < lineToByte.length; i++) {
								protocol.process(encdec.decodeNextByte(lineToByte[i]));
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
						lock.notify();
					}
				});
	
				ListenThread.start();
			}
		}
	}
	