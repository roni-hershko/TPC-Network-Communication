package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    
    private byte[] bytes = new byte[1 << 9]; //start with 1k
    private int len = 0;
    //private int IndexMSGPass = 0;
    private final int packetSize = 512;

    //big endian lowest to highest
    //get from the  cilent packet with size 512
    //use get input stream read
    @Override
    public byte[] decodeNextByte(byte nextByte) {
        if(nextByte == 0) {
            if(len==0) {
                return null;
            }
            return bytes;
        }
        bytes[len] = nextByte;
        len++;
        return null;
    }

    //send to cilent packet with size 512, cut if needed
    //use get input stream write
    @Override
    public byte[] encode(byte[] message) {
        byte[] result = new byte[packetSize];
        if(message[0] == 0 && message.length > 1){
            result[0] = 0;
        }
        else{
            result[0] = message[0];
        }

        for(int i=1; i < result.length-1; i++) {
            result[i] = message[i];
        }

        if(message.length > packetSize) {
            byte[] newMSG = new byte[message.length - packetSize +2];
            for(int i=0; i < newMSG.length; i++) {
                newMSG[i] = message[i + packetSize +2];
            }
            message = newMSG;
            return result;
        }
        else{
            result[result.length-1] = 0;
            //check if compile
            message = null;
            return result;
        }
        
    }
}