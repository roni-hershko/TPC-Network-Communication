package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.FileInputStream;
import java.io.File;

//we can copy the code from echo except the process method
class holder{
    static ConcurrentHashMap<Integer, Boolean> ids_login = new ConcurrentHashMap<>();
}

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private boolean shouldTerminate = false;
    int connectionId =0;
    Connections<byte[]> connections;
    private Map<Integer, ConnectionHandler<byte[]>> connectionsMap;
    boolean waitingForFile = false;
    String fileNameString;
    private final int packetSize = 512;



    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        this.shouldTerminate = false;
        this.connectionId = connectionId;
        this.connections = connections;
        holder.ids_login.put(connectionId, true);
    }

    //byte to short and opposite
    @Override
    public void process(byte[] message) {
        //DISC
        if(message.length == 2)
            shouldTerminate =true;
        if(message[0] == 1)
        {
            //RRQ
            oneRRQ(message);
        }
        else if(message[0] == 2)
        {
            //WRQ
            twoWRQ(message);
        }
        else if(message[0] == 3)
        {
            //DATA
            threeData( message);
        }
        else if(message[0] == 4)
        {
            //ACK
            //send next block
        }
        else if(message[0] == 5)
        {
            //ERROR
            //print error
        }
        else if(message[0] == 6)
        {
            //ERROR
            //print error
        }
        else if(message[0] == 7)
        {
            sevenLOGRQ(message);
        }
        else if(message[0] == 8)
        {
            //ERROR
            //print error
        }
        else if(message[0] == 9)
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
        if(shouldTerminate){
            this.connections.disconnect(connectionId);
            holder.ids_login.remove(connectionId);
        }
        return shouldTerminate;
    }

    private void oneRRQ(byte[] message){
        byte[] response = new byte[1]; 
        response[0] = -1;  
        
        //have not log in yet
        if(connectionId == 0){
            response[0] = 6;
        }
        else{
            //convert the byte array to a string
            String fileName = new String(message, 1, message.length -1, StandardCharsets.UTF_8);
            String directoryPath = "/Files/";
            File directory = new File(directoryPath);
            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();

                // Iterate through the files to find the file we are looking for
                for (File file : files) {
                    if (file.isFile() && file.getName().equals(fileName)) {
                        try {
                            FileInputStream fileInputStream = new FileInputStream(file);
                            connections.send(connectionId, fileInputStream.readAllBytes());
                            fileInputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break; // Exit the loop once the file is found
                    }
                    else{
                        //file not found
                        response[0] = 1;
                    }
                }
            }
            else{
                //files directory not found
                response[0] = 1;
            }
            //send error
        }
        if(response[0] > -1){
            connections.send(connectionId, response);  
        }

    }

    private void twoWRQ(byte[] message){
        byte[] response = new byte[1]; 
        byte[] fileNametoUpload = new byte[message.length - 1];
        for(int i = 0; i < message.length-1 ; i++){
            fileNametoUpload[i] = message[i+1];
        }

        response[0] = -1;  
        
        //have not log in yet
        if(connectionId == 0){
            response[0] = 6;
        }

        else{
            String directoryPath = "/Files/";
            File directory = new File(directoryPath);
            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();

                // Iterate through the files to find the file we are looking for
                for (File file : files) {
                    if (file.isFile() && file.getName().equals(fileNametoUpload)) {
                        try {
                            FileInputStream fileInputStream = new FileInputStream(file);
                            response[0] = 5;
                            fileInputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break; // Exit the loop once the file is found
                    }
            }
        }
        if(response[0] > -1){
            connections.send(connectionId, response);  
        }
        else{
            //create the file
            fileNameString = new String(fileNametoUpload, 0, fileNametoUpload.length, StandardCharsets.UTF_8);
            File newFile = new File(directory, fileNameString);
            try {
                newFile.createNewFile();
            } catch (IOException e) {
                System.err.println("An error occurred: " + e.getMessage());
            }
            waitingForFile = true;
            response[0] = 0;
            connections.send(connectionId, response);
        }
    }
}

    private void threeData(byte[] message){

        if(waitingForFile){
            //short packetSizeOfMSG = (short)(((short) message[0]) << 8 | (short) message[1]);
            String filePath = "/File/"+ fileNameString; 

            try {
                // Create a FileWriter object with the specified file path
                FileWriter writer = new FileWriter(filePath);
                String dataToFile = new String(message, 5, message.length, StandardCharsets.UTF_8);

                for(int i = 0; i < dataToFile.length(); i++){
                    if(dataToFile.charAt(i) != 0){
                        writer.write(dataToFile.charAt(i));
                    }
                    else{
                        writer.close(); 
                        waitingForFile = false;
                        fileNameString = "";
                        break;
                    } 
                }
                // Close the writer to release resources
                writer.close(); //need to check 
            } catch (IOException e) {
            }          

        }
    }

    private void sevenLOGRQ(byte[] message){
        connectionId = byteToShort(message, 1);
        byte[] response = new byte[1];      
        //if the user is already logged in
        if(connectionsMap.containsKey(connectionId)){
            response[0] = 7; //check if works
        }
        else{
            response[0] = 0;
            //add the user to the connectionsMap
            start(connectionId, connections);
        }
        connections.send(connectionId, response);  
    }

    private int byteToShort(byte[] byteArr, int index){
        return ((byteArr[index] & 0xff) << 8) + (byteArr[index + 1] & 0xff);
    }
}

