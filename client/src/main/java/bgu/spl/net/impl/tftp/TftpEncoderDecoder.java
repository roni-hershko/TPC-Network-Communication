package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    
    private byte[] bytes = new byte[1 << 9]; //start with 1k
    private int len = 0;
    private final int packetSize = 512;
    private int stopValue = packetSize;
    boolean thereIsZero = false;

    //big endian lowest to highest
    //get from the  cilent packet with size 512
    //use get input stream read
    @Override
    public byte[] decodeNextByte(byte nextByte) {
        Byte nextByteB = nextByte;
        short nextByteShort = nextByteB.shortValue();  
        if(nextByteShort == 0){
            if (len == 0) 
                return null;
            if(thereIsZero){
                thereIsZero = false; 
                return bytes;  
            }    
        }
        bytes[len] = nextByte;
        len++;
        short bytes0 = (short)(bytes[0] & 0xff);
        if(len == 1 && (bytes0 == 6 || bytes0 == 10)) //case 6 or 10 
        {
            stopValue = 1;
        }
        else if(len == 3 )
        {
            if(bytes0==3)//case 3 
                stopValue = (short)((bytes[1] & 0xff) << 8 | (bytes[2] & 0xff));  
            if(bytes0 == 1 || bytes0 == 2 || bytes0 == 5 || bytes0 == 7 || bytes0 == 8 || bytes0 == 9)
                thereIsZero = true; //cases 1,2,5,7,8,9
        }
        else if(bytes0 == 4) //case 4
        {
            stopValue = 3;
        }

        if(len-1 == stopValue)
            return bytes;
        else
            return null;
    }

    @Override
    public byte[] encode(byte[] message) {
      return message;
    }
}