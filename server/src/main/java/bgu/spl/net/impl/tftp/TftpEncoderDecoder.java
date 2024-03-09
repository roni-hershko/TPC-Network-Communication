package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    
    private byte[] bytes = new byte[1 << 9]; //start with 512
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
        //short nextByteShort = nextByteB.shortValue();  

        if(nextByteB== 0){
            if (len == 0) 
                return null;
            if(thereIsZero){
                thereIsZero = false;
				byte[]resultArray= resultArray();  //cut the array to the message size
				len=0;
                return resultArray; 
            }    
        }
        bytes[len] = nextByte;
        len++;
        Byte opcode = bytes[0];

		//case 6 or 10 
        if(opcode == 6 || opcode == 10){
            stopValue = 1;
        }
		//case 3 
        else if(len == 3 && opcode==3){
            stopValue = (short) (((short) bytes [1]) << 8 | (short) (bytes [2])); 
        }
		//case 4
        else if(opcode == 4) {
            stopValue = 3;
        }
		//cases 1,2,5,7,8,9
		else if(opcode == 1 || opcode == 2 || opcode == 5 || opcode == 7 || opcode == 8 || opcode == 9) {
			thereIsZero = true; 
		}

        if(len == stopValue){//need to check for user name with one char and for user name with 0 init
			byte[]resultArray= resultArray();  //cut the array to the message size
			len=0;
			return resultArray; 
		}
    	else{
            return null;
		}
    }


    @Override
    public byte[] encode(byte[] message) {
      return message;
    }


	//added
	public byte[] resultArray(){
		byte[] result = new byte[len];
		for(int i = 0; i < len; i++){
			result[i] = bytes[i];
		}
		return result;
	}
}