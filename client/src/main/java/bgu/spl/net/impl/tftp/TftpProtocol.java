package bgu.spl.net.impl.tftp;
import bgu.spl.net.api.MessagingProtocol;

import java.io.File;
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

    private boolean shouldTerminate = false;
    private boolean waitingForDirq = false;
    private final int packetSize = 512;
    private String fileName;
    Map<String, File> myFiles = new HashMap<>();

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
                //write the data to the file
                

                if(packetSizeofData < packetSize)
                    waitingForDirq = false;
            }
        }
        else if(message0 == 4)
        {
            int ackNum = ((msg[1] & 0xff) << 8 | (msg[2] & 0xff));
            System.out.println("ACK " + ackNum);
            return null;
        }
        else if(message0 == 5)
        {
            ERROR(msg);
            return null;
        }
        else if(message0 ==9){
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
        byte opcode;
        byte[] dataBytes = null;
        if (parts.length == 2) {
            String command = parts[0];
            String data = parts[1];
            switch (command) {
                case "RRQ":
                    opcode = RRQ_OPCODE;
                    dataBytes = data.getBytes(StandardCharsets.UTF_8);
                    fileName = data;
                    break;
                case "WRQ":
                    opcode = WRQ_OPCODE;
                    dataBytes = data.getBytes(StandardCharsets.UTF_8);
                    fileName = data;
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
                    throw new IllegalArgumentException("Invalid command: " + command);
            }
        } else {
            throw new IllegalArgumentException("Invalid message format: " + message);
        }

        // Concatenate the opcode, size (if needed), and data bytes
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

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
   
    public void ERROR(byte[] msg) {
        String errorMsg = new String(msg, 4, msg.length);
        System.out.println("Error: " + errorMsg);
    }

    public void printDirq(byte[] msg) {
        String fileName;
        int lastIndex=0;
        while(lastIndex < msg.length){
            int firstIndex = lastIndex;
            while(msg[lastIndex]!=0){
                lastIndex++;
            }
            fileName = new String(msg, firstIndex, lastIndex-firstIndex);
            System.out.println(fileName);
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
}
