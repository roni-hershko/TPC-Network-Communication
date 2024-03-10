package bgu.spl.net.impl.tftp;
import bgu.spl.net.api.MessagingProtocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class TftpProtocol implements MessagingProtocol<byte[]>{

    private static final byte RRQ_OPCODE = 1; ///do cast to byte
    private static final byte WRQ_OPCODE = 2;
    private static final byte DATA_OPCODE = 3;
    private static final byte DIRQ_OPCODE = 6;
    private static final byte LOGRQ_OPCODE = 7;
    private static final byte DELRQ_OPCODE = 8;
    private static final byte DISC_OPCODE = 10;

    Map<String, File> myFiles = new HashMap<>();
    private boolean shouldTerminate = false;
    boolean waitingForDirq = false;
    boolean waitingForData = false;
    boolean waitingForUpload = false;
    private final int packetSize = 512;
    private String fileName;
    private int blockNum = 0;
    private int indexData = 0;
    private byte[] dirqData = new byte[0];


    public byte[] process(byte[] msg) {
        short message0 = (short)(msg[0] & 0xff); 
        //the size of the packet
        int packetSizeofData = byteToShort(msg, 1, 2);

        if(message0 == 3) //data
        {
            int blockNum = byteToShort(msg, 2, 3);
            System.out.println("DATA " + blockNum);
            
            if(waitingForDirq)
            {
                addDataToDirq(msg, dirqData);
                if(packetSizeofData < packetSize){
                    printDirq(msg);
                    waitingForDirq = false;
                    dirqData = new byte[0];
                }
            }
            else
            {
                addContentToFile(msg, blockNum);
                if(packetSizeofData < packetSize){
                    waitingForData = false;
                    fileName = null;
                }
            }
            return ACKSend((short)blockNum);
        }
        else if(message0 == 4) //ack
        {
            if(waitingForUpload){
                return sendFile(blockNum, indexData);
            }
            int ackNum = byteToShort(msg, 1, 2);
            System.out.println("ACK " + ackNum);
            return null;
        }
        else if(message0 == 5) //Error
        {
            waitingForDirq = false;
            waitingForUpload = false;
            fileName = null;
            if(waitingForData){
                waitingForData = false;
                File file = new File("client/Flies/"+fileName);
                file.delete();
            }
            ERROR(msg);
            return null;
        }
        else if(message0 ==9)//broadcast
        {
            System.out.println("Broadcast message: " + new String(msg, 2, msg.length));
            return null;
        }
        else if(message0 == 10)//disc
        {
            shouldTerminate = true;
            return null;
        }
       
        return null;
    }

    public byte[] creatRequest(String message) {
        String[] parts = message.split("\\s+", 2); // Split by first space 

        // Determine opcode and data
        byte opcode = 0; // Default to invalid opcode
        byte[] dataBytes = null;
        if (parts.length == 2) {
            String command = parts[0];
            String data = parts[1];
            switch (command) {
                case "RRQ":
                    if(!isFileExist(data)){
                        dataBytes = data.getBytes(StandardCharsets.UTF_8);
                        fileName = data;
                        waitingForData = true;
                        opcode = RRQ_OPCODE;
                        createFile();
                    }
                    else{
                        selfError(5);
                    }
                    break;
                case "WRQ":
                    if(isFileExist(data)){
                        dataBytes = data.getBytes(StandardCharsets.UTF_8);
                        fileName = data;
                        waitingForUpload =true;
                        opcode = WRQ_OPCODE;
                    }
                    else{
                        selfError(1);
                    }
                    break;
                case "DIRQ":
                    opcode = DIRQ_OPCODE;
                    waitingForDirq = true;
                    break;
                case "LOGRQ":
                    opcode = LOGRQ_OPCODE;
                    dataBytes = data.getBytes(StandardCharsets.UTF_8);
                    break;
                case "DELRQ":
                    opcode = DELRQ_OPCODE;
                    dataBytes = data.getBytes(StandardCharsets.UTF_8);
                    break;
                case "DISC":
                    opcode = DISC_OPCODE;
                    break;

                default:
                    selfError(0);
            }
        } else {
            selfError(0);
        }

        
        if(opcode!=0){
            if(opcode == RRQ_OPCODE || opcode == WRQ_OPCODE || opcode == LOGRQ_OPCODE || opcode == DELRQ_OPCODE){
                byte[] messageBytes = new byte[dataBytes.length + 2];
                messageBytes[0] = (byte) 0;
                messageBytes[1] = opcode;
                for (int i = 0; i < dataBytes.length; i++) {
                    messageBytes[i + 2] = dataBytes[i];
                }
                messageBytes[messageBytes.length - 1] = 0;
                return messageBytes;
            }
            else //opcode == DIRQ_OPCODE || opcode == DISC_OPCODE)
            {
                byte[] messageBytes = new byte[2];
                messageBytes[0] = (byte) 0;
                messageBytes[1] = opcode;
                return messageBytes;
            }
        }
        else
            return null;
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
   
    public void ERROR(byte[] msg) {
        String errorMsg = new String(msg, 4, msg.length);
        System.out.println("Error: " + errorMsg);
    }

    public void printDirq(byte[] msg) {
        int lastIndex=0;
        while(lastIndex < msg.length){
            int firstIndex = lastIndex;
            while(msg[lastIndex]!=0){
                lastIndex++;
            }
            String fileNameDirq = new String(msg, firstIndex, lastIndex-firstIndex);
            System.out.println(fileNameDirq);
        }
    }

    public void createFile(){

        String directoryPath = "client/File/";
        File file = new File(directoryPath + fileName);

        try {
            // Create the file
            if (file.createNewFile()) {
                System.out.println("File created successfully.");
            } else {
                System.out.println("File already exists.");
            }
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public void addContentToFile(byte[] msg, int blockNum) {

        String filePath = "client//File/"+ fileName; 

        try {
            // Create a FileWriter object with the specified file path
            FileWriter writer = new FileWriter(filePath);
            String dataToFile = new String(msg, 5, msg.length, StandardCharsets.UTF_8);

            for(int i = 0; i < dataToFile.length(); i++){
                writer.write(dataToFile.charAt(i));
            }
            // Close the writer to release resources
            writer.close(); //need to check 
        } catch (IOException e) {
        }  
    }

    public byte[] sendFile(int blockNum, int indexData){
        byte[] dataPacket = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(myFiles.get(fileName));
            byte[] fileBytes = fileInputStream.readAllBytes();
            dataPacket = opcodeDATA(blockNum, fileBytes,indexData);  

            if(fileBytes.length > packetSize*blockNum){//there is still file to send
                blockNum++;
            }
            else{//end of file
                waitingForUpload = false;
                fileName = null;
            }
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataPacket;
    }
    
    private byte[] opcodeDATA(int blockNum, byte[] data , int indexData){
        //create data packet in the size of the packet remain to send
        int min = Math.min(packetSize, data.length - indexData);
        min = min + 6;
        byte[] dataPacket = new byte[min];
        dataPacket[0] = (byte)0;
        dataPacket[1] = (byte)3;
        dataPacket[2] = shortTobyte((short)dataPacket.length)[0];
        dataPacket[3] = shortTobyte((short)dataPacket.length)[1];
        dataPacket[4] = shortTobyte((short)blockNum)[0];
        dataPacket[5] = shortTobyte((short)blockNum)[1];
        for(int i = 6; i < dataPacket.length; i++){
            dataPacket[i] = data[indexData + i - 6];
        }
        indexData = indexData + dataPacket.length - 6;
        return dataPacket;
    } 
        
    private boolean isFileExist(String fileName){
        File file = new File("client/File/"+fileName);
        return file.exists();
    }
    
    private void selfError(int errNum){
        String err0= "Not defined, see error message (if any).";
        String err1= "File not found -RRQ, DELRQ of non existing file";
        String err2= "Access violation.";
        String err3= "Disk full or allocation exceeded.";
        String err4= "Illegal TFTP operation.";
        String err5= "File already exists- file name exists for WRQ.";
        String err6= "User not logged in -any opcode received before Login completes.";
        String err7= "User already logged in -Login username already connected.";
        if(errNum == 0)
            System.out.println("Error: " + err0);
        else if(errNum == 1)
            System.out.println("Error: " + err1);
        else if(errNum == 2)
            System.out.println("Error: " + err2);
        else if(errNum == 3)
            System.out.println("Error: " + err3);
        else if(errNum == 4)
            System.out.println("Error: " + err4);
        else if(errNum == 5)
            System.out.println("Error: " + err5);
        else if(errNum == 6)
            System.out.println("Error: " + err6);
        else if(errNum == 7)
            System.out.println("Error: " + err7);
    }

    private byte[] ACKSend(short blockNum){ //check
        byte[] ack = new byte[4];
        ack[0] = (byte)0; 
        ack[1] = (byte)4;
        ack[2] = shortTobyte(blockNum)[0];
        ack[3] = shortTobyte(blockNum)[1];
        return ack;
    }

    private byte[] addDataToDirq(byte[] newData, byte[] dirqData){
        byte[] ans = new byte[dirqData.length + newData.length];
        for (int i = 0; i < dirqData.length; i++) {
            ans[i] = dirqData[i];
        }
        for (int i = 0; i < newData.length; i++) {
            ans[i + dirqData.length] = newData[i];
        }
        return ans;
    }   

    private byte[] shortTobyte(short a){
		return new byte []{(byte)(a >> 8) , (byte)(a & 0xff)};
	}
	
    private short byteToShort(byte[] byteArr, int fromIndex, int toIndex){//check
        return (short) (((short) byteArr [fromIndex]) << 8 | (short) (byteArr [toIndex])); 
    }



}


