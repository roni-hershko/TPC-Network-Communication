package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.Connections;
import java.io.FileInputStream;

//we can copy the code from echo except the process method

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private boolean shouldTerminate = false;
    int connectionId;
    Connections<byte[]> connections;

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        shouldTerminate = false;
        this.connectionId = connectionId;
        this.connections = connections;
    }

    //byte to short and opposite
    @Override
    public void process(byte[] message) {
        //DISC
        if(message.length == 2)
            shouldTerminate =true;
        if(message[1] == 1)
        {
            //RRQ
            //send file
            //ActiveConnections.send(ClientID, new byte[0]);
        }
        else if(message[1] == 2)
        {
            //WRQ
            //recieve file
        }
        else if(message[1] == 3)
        {
            //DATA
            //write to file
        }
        else if(message[1] == 4)
        {
            //ACK
            //send next block
        }
        else if(message[1] == 5)
        {
            //ERROR
            //print error
        }
        else if(message[1] == 6)
        {
            //ERROR
            //print error
        }
        else if(message[1] == 7)
        {
            //ERROR
            //print error
        }
        else if(message[1] == 8)
        {
            //ERROR
            //print error
        }
        else if(message[1] == 9)
        {
            //ERROR
            //print error
        }
        else
        {
            //invalid
        }
        //connection.send()
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
    
}

