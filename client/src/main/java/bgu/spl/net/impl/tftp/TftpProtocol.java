package bgu.spl.net.impl.tftp;
import bgu.spl.net.api.MessagingProtocol;

import java.time.LocalDateTime;

public class TftpProtocol implements MessagingProtocol<byte[]>{

    private boolean shouldTerminate = false;

    public byte[] process(byte[] msg) {
        return null;
    }

    private byte[] creatRequest(byte[] message) {
        
        return null;
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
    
}
