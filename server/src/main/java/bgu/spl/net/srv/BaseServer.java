package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessagingProtocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

import bgu.spl.net.impl.tftp.ConnectionsImpl;
import bgu.spl.net.impl.tftp.TftpEncoderDecoder;
import bgu.spl.net.impl.tftp.TftpProtocol;
import java.util.concurrent.atomic.AtomicInteger;


public abstract class BaseServer<T> implements Server<T> {

    private final int port;
    private final Supplier<BidiMessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private ServerSocket sock;
    private AtomicInteger connectionId;
    private Connections<T> connections;

    public BaseServer(
            int port,
            Supplier<BidiMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.sock = null;
        this.connections = new ConnectionsImpl<T>();
        this.connectionId = new AtomicInteger(0);
    }


    @Override
    public void serve() {
        
        //fix this , we will have to figure out how to insert handlers to connections 
        //and initialize the protocols
        //insert conection to a protocol and protocol to handler

        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");

            this.sock = serverSock; //just to be able to close

            while (!Thread.currentThread().isInterrupted()) {

                Socket clientSock = serverSock.accept();
                int newConnectionId = connectionId.getAndIncrement();

                BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<>(
                    clientSock,
                    encdecFactory.get(),
                    protocolFactory.get(),
                    newConnectionId,
                    connections);

                execute(handler);
            }
        } catch (IOException ex) {
        }

        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
    }

    protected abstract void execute(BlockingConnectionHandler<T>  handler);

}
