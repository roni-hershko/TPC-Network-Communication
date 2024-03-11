package bgu.spl.net.impl.tftp;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

	
	public class TftpClient {
	
		public static void main(String[] args) throws IOException {

	
			if (args.length == 0) {
				args = new String[]{"localhost"};
			}
			try (Socket sock = new Socket(args[0], 7777);
					BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
					BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream())){
						System.out.println("socket created");

			BlockingConnectionHandlerClient handler = new BlockingConnectionHandlerClient(sock, new TftpEncoderDecoder(), new TftpProtocol(), in, out);
			Thread handlThread = new Thread(handler);
			handlThread.start();
			System.out.println("handlThread created");

			
			KeyBoardConnectionHandler keyboardHandler = new KeyBoardConnectionHandler(handler, out, new BufferedReader(new java.io.InputStreamReader(System.in)));		
			Thread keyboardThread = new Thread(keyboardHandler);
			keyboardThread.start();
			System.out.println("keyboardThread created");

			try{
				handlThread.join();
				keyboardThread.interrupt();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

			// //BufferedReader and BufferedWriter automatically using UTF-8 encoding
			// try (Socket sock = new Socket(args[0], 7777)){
			// 	BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
			// 	BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream()); 
				
			// 	System.out.println("sending message to server");
			// 	Object lock = new Object();

			// 	Thread KeyboardThread = new Thread(() -> {
			// 		//creating a BufferedReader object to read input from the keyboard
			// 		BufferedReader keyboard = new BufferedReader(new java.io.InputStreamReader(System.in));
			// 		while (true) {
			// 			synchronized (lock) {
			// 				try {
			// 					String line = keyboard.readLine();	
			// 					byte[] lineToByte = protocol.creatRequest(line);
			// 					if(lineToByte != null){ //if the request is valid
			// 						out.write((encdec.encode(lineToByte)));
			// 						out.flush();
			// 						lock.notify();
			// 					}
			// 				}
			// 				catch (IOException e) {
			// 					e.printStackTrace();
			// 				}
			// 			}
			// 			try {
			// 				lock.wait();
			// 			} catch (InterruptedException e) {
			// 				e.printStackTrace();
			// 			}
			// 		}
			// 	});
			// 	KeyboardThread.start();

			// 	Thread ListenThread = new Thread(() -> {
					
			// 		try {
			// 			int read;
			// 			while (!protocol.shouldTerminate() && (in != null) && (read = in.read()) >= 0){
			// 				byte[] ansFromServer = encdec.decodeNextByte((byte) read);
			// 				if (ansFromServer != null) {
			// 					if(protocol.waitingForUpload){
			// 						byte[] DataToServer = protocol.process(ansFromServer);
			// 						out.write((encdec.encode(DataToServer)));
			// 						out.flush();
			// 					}
			// 					else{
			// 						protocol.process(ansFromServer);
			// 					}
			// 				}
			// 			}
					
				
			// 		synchronized (lock) {
			// 			if(!protocol.waitingForUpload && !protocol.waitingForDirq && !protocol.waitingForData){ //there is more to upload cant take more requests
			// 				lock.notifyAll();
			// 			}
			// 		}
					
			// 	}catch (IOException e) {
			// 		e.printStackTrace();
			// 	}});
				
			// 	ListenThread.start();
			// }
			// catch (IOException e) {
			// 	e.printStackTrace();
			 
		}
	}
	