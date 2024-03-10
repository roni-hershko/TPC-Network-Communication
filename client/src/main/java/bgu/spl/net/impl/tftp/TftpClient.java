package bgu.spl.net.impl.tftp;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
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
				BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
				BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream())) {
				
				System.out.println("sending message to server");
				Object lock = new Object();

				Thread KeyboardThread = new Thread(() -> {
					BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
					while (true) {
						try {
							String line = keyboard.readLine();	
							byte[] lineToByte = protocol.creatRequest(line);
							if(lineToByte != null){ //if the request is valid
								out.write((encdec.encode(lineToByte)));
								out.flush();
								lock.notify();
								try {
									lock.wait();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				KeyboardThread.start();

				Thread ListenThread = new Thread(() -> {
					while (true) {
						try {
							int line = in.read();
							byte[] lineToByte =  new byte []{(byte)(line >> 8) , (byte)(line & 0xff)};
							byte[] ansFromServer = null;
							byte[] DataToServer = null;
							for (int i = 0; i < lineToByte.length; i++) {
								ansFromServer = encdec.decodeNextByte(lineToByte[i]);
							}
							if(protocol.waitingForUpload){
								DataToServer = protocol.process(ansFromServer);
								try {
									out.write((DataToServer));
									out.flush();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							else{
								protocol.process(ansFromServer);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
						if(!protocol.waitingForUpload && !protocol.waitingForDirq && !protocol.waitingForData) //there is more to upload cant take more requests
							lock.notify();
					}
				});
	
				ListenThread.start();
			}
		}
	}
	