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

    private static final byte RRQ_OPCODE = 1;
    private static final byte WRQ_OPCODE = 2;
    private static final byte DATA_OPCODE = 3;
    private static final byte DIRQ_OPCODE = 6;
    private static final byte LOGRQ_OPCODE = 7;
    private static final byte DELRQ_OPCODE = 8;
    private static final byte DISC_OPCODE = 10;

    Map<String, File> myFiles = new HashMap<>();
    private boolean shouldTerminate = false;
    private boolean waitingForDirq = false;
    boolean waitingForUpload = false;
    private final int packetSize = 512;
    private String fileName;
    private int blockNum = 0;
    private int indexData = 0;


    public byte[] process(byte[] msg) {
        short message0 = (short)(msg[0] & 0xff);
        //the size o the packet
        int packetSizeofData = ((msg[1] & 0xff) << 8 | (msg[2] & 0xff));
        if(message0 == 3){
            int blockNum = ((msg[2] & 0xff) << 8 | (msg[3] & 0xff));
            System.out.println("DATA " + blockNum);
            //if we get a data that has 0 byte and continue we know it is dirq and need to print it
            if(waitingForDirq)
            {
                printDirq(msg);
                if(packetSizeofData < packetSize)
                    waitingForDirq = false;
            }
            else
            {
                createFile(msg);
                if(packetSizeofData < packetSize)
                    waitingForDirq = false;
            }
        }
        else if(message0 == 4) //ack
        {
            if(waitingForUpload){
                return sendFile(blockNum, indexData);
            }
            int ackNum = ((msg[1] & 0xff) << 8 | (msg[2] & 0xff));
            System.out.println("ACK " + ackNum);
            return null;
        }
        else if(message0 == 5)
        {
            waitingForDirq = false;
            waitingForUpload = false;
            fileName = null;
            ERROR(msg);
            return null;
        }
        else if(message0 ==9)
        {
            System.out.println("Broadcast message: " + new String(msg, 2, msg.length));
            return null;
        }
        else if(message0 == 10)
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
                        opcode = RRQ_OPCODE;
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

        // Concatenate the opcode, size (if needed), and data bytes
        if(opcode!=0){
            byte[] messageBytes = new byte[dataBytes.length + 2];
            messageBytes[0] = opcode;
            if (dataBytes != null) {
            for (int i = 0; i < dataBytes.length; i++) {
                messageBytes[i + 1] = dataBytes[i];
            }
            }
            messageBytes[messageBytes.length - 1] = 0;
            return messageBytes;
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

    public void createFile(byte[] msg) {
        String filePath = "/File/"+ fileName; 

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
        dataPacket[0] = (byte)((0 >> 8) & 0xff);
        dataPacket[1] = (byte)((3 >> 8) & 0xff);
        dataPacket[2] = (byte)((0 >> 8) & 0xff);
        dataPacket[3] = (byte)((dataPacket.length >> 8) & 0xff);
        dataPacket[4] = (byte)((0 & 0xff));
        dataPacket[5] = (byte)((blockNum >> 8) & 0xff);
        for(int i = 6; i < dataPacket.length; i++){
            dataPacket[i] = data[indexData +i];
        }
        return dataPacket;
    } 
        
    private boolean isFileExist(String fileName){
        File file = new File("/File/"+fileName);
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
}


