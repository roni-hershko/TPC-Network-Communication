package bgu.spl.net.impl.tftp;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import bgu.spl.net.impl.tftp.TftpEncoderDecoder;
import bgu.spl.net.impl.tftp.TftpProtocol;

import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.Server;

public class TftpServer {
    
    private final int port;
    private final Supplier<TftpProtocol> protocolFactory;
    private final Supplier<TftpEncoderDecoder> encdecFactory;
    private ServerSocket sock;
     private AtomicInteger connectionId;
    private Connections<byte[]> connections;

    public TftpServer(
            int port,
            Supplier<TftpProtocol> protocolFactory,
            Supplier<TftpEncoderDecoder> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.sock = null;
        this.connections = new ConnectionsImpl<byte[]>();
        this.connectionId = new AtomicInteger(0);
    }

    //need to check, because implemented in the server
    protected void execute(BlockingConnectionHandler handler) {
        new Thread(handler).start();
    }

    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");

            this.sock = serverSock; //just to be able to close

            while (!Thread.currentThread().isInterrupted()) {

                Socket clientSock = serverSock.accept();
                
                BlockingConnectionHandler<byte[]> handler = new BlockingConnectionHandler<byte[]>(
                        clientSock,
                        encdecFactory.get(),
                        protocolFactory.get(),
                        connectionId.incrementAndGet(),
                        connections);

                execute(handler);
            }
        } catch (IOException ex) {
        }

        System.out.println("server closed!!!");
    }

    public static void main(String[] args) {
        Map<String, File> fileMap;

        TftpServer server = new TftpServer(
            Integer.decode(args[1]).intValue(),
           ()-> new TftpProtocol(fileMap),
           ()-> new TftpEncoderDecoder());
            server.serve();
    }
}

 
