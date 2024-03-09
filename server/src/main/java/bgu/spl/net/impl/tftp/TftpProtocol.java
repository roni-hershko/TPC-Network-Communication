package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.io.FileInputStream;
import java.io.File;
import bgu.spl.net.srv.ConnectionHandler;

class holder{
	static ConcurrentHashMap<Integer, String> logedInUserNames = new ConcurrentHashMap<>(); 

	static void printMap(){
		for (ConcurrentHashMap.Entry<Integer, String> entry : logedInUserNames.entrySet()) {
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		}
	}
}


public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private boolean shouldTerminate = false;
    int connectionId;
    boolean waitingForFile = false;
    String fileNameString;
    private final int packetSize = 512;
    String userName;
	short blockNum = 1;
	ConcurrentHashMap<String, File> fileMap; 
	Connections<byte[]> connections; 



    public TftpProtocol(){
		//insert all the files from the flies folder in the server into the fileMap
		String folderPath = "/flies/";
		File folder = new File(folderPath);
        File[] files = folder.listFiles();
		ConcurrentHashMap<String, File> filesMap = new ConcurrentHashMap<>(); 

        if (files != null) {
            for (File file : files) {
					filesMap.put(file.getName(), file);
				}
            }
        
        this.fileMap = filesMap; 
    }
	

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
	//Used to initiate the current client protocol with it's personal connection ID and the connections implementation
	//Initiate the protocol with the active connections structure of the server and saves the owner client’s connection id
		this.connectionId = connectionId;
		this.connections = connections;

    }


    @Override
    public void process(byte[] message) {
        short message0 = (short)(message[0] & 0xff);
        if(message0 == 1)
        {
            //RRQ
            oneRRQ(message);
        }
        else if(message0 == 2)
        {
            //WRQ
            twoWRQ(message);
        }
        else if(message0 == 3)
        {
            //DATA
            threeDataRecive( message);
        }
        else if(message0 == 4)
        {
            //ACK
            fourACKRecive(message);        
        }
        else if(message0 == 5)
        {
            //ERROR
            fiveERRORRecive(message);
        }
        else if(message0 == 6)
        {
            //DIRQ
            sixDIRQ(message);
        }
        else if(message0 == 7)
        {
            //LOGRQ
            sevenLOGRQ(message);
        }
        else if(message0 == 8)
        {
            //DELRQ
            eightDELRQ(message);
        }
        else if(message0 == 10)
        {
            //DISC  
            tenDISC(message);
        }
        else
        {
            //ERROR 0
            connections.send(connectionId, ERRORSend(0));
        }
    }


    @Override
    public boolean shouldTerminate() {
        if(shouldTerminate){
            this.connections.disconnect(connectionId);
            holder.logedInUserNames.remove(connectionId);
        }
        return shouldTerminate;
    }


    private void oneRRQ(byte[] message){
        int errNum = -1;
        boolean fileFound = false;
        
        //if the user is not logged in
        if(!holder.logedInUserNames.containsKey(connectionId)){
            errNum = 6;
        }
        
        //convert the byte array to a string
        String fileName = new String(message, 1, message.length -1, StandardCharsets.UTF_8);
            for (String key : fileMap.keySet()) {
                if (key.equals(fileName)) {
                    if(errNum != -1){
                        try {
                            FileInputStream fileInputStream = new FileInputStream(fileMap.get(key));
                            byte[] fileBytes = fileInputStream.readAllBytes();
                            short blockcounter = 1;
                            byte[] dataPacket = DATASend(blockcounter, fileBytes,0, false); 
                            connections.send(connectionId, dataPacket);

                            while(fileBytes.length > packetSize*blockcounter){
                                blockcounter++;
                                dataPacket = DATASend(blockcounter, fileBytes, packetSize*blockcounter, false);
                                connections.send(connectionId, dataPacket);
                            }
                            
                            fileInputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    fileFound = true;
                    break; // Exit the loop once the file has found
                }
            }
            if(!fileFound){
                errNum = 1;
            }
        
            if(errNum != -1){
                connections.send(connectionId, ERRORSend(errNum));
            }
    }


    private void twoWRQ(byte[] message){
        int errNum = -1;

        //if the user is not logged in
        if(!holder.logedInUserNames.containsKey(connectionId)){
            errNum = 6;
        }

        byte[] fileNametoUpload = new byte[message.length - 1];
        for(int i = 0; i < message.length-1 ; i++){
            fileNametoUpload[i] = message[i+1];
        }
        
        fileNameString = new String(fileNametoUpload, 0, fileNametoUpload.length, StandardCharsets.UTF_8);
        for (String key : fileMap.keySet()) {
            if (key.equals(fileNameString)) {
                errNum = 5;
                break; // Exit the loop once the file is found
            }
        }
        if(errNum != -1){
			connections.send(connectionId, ERRORSend(errNum));
        }
        else {
			connections.send(connectionId, ACKSend((short)0));
            //create the file
            fileMap.put(fileNameString, new File("/Files/"+fileNameString));
        }     
}


    private void threeDataRecive(byte[] message){
        //short packetSizeOfMSG = (short)(((short) message[0]) << 8 | (short) message[1]);
        String filePath = "/File/"+ fileNameString; 
		

        try {
            // Create a FileWriter object with the specified file path
            FileWriter writer = new FileWriter(filePath);
            String dataToFile = new String(message, 5, message.length, StandardCharsets.UTF_8);

            for(int i = 0; i < dataToFile.length(); i++){
                writer.write(dataToFile.charAt(i));
            }
            // Close the writer to release resources
            writer.close(); //need to check 
        } catch (IOException e) {
        }  
		if(message.length > packetSize){
			connections.send(connectionId, ERRORSend(blockNum));
			blockNum++;
		}

        if(message.length < packetSize){  //remember to check
            nineSendBroadcast( fileNameString.getBytes() ,1);
			blockNum=1;
        }
    }


    private void fourACKRecive(byte[] message){
    short blockNum = byteToShort(message, 2,3); 
        System.out.println("ACK " + blockNum);
    }


    private void fiveERRORRecive(byte[] message){
        //TO DO
    }


    private void sixDIRQ(byte[] message){
		//if the user is not logged in
        if(!holder.logedInUserNames.containsKey(connectionId)){
            ERRORSend(6);
        }
        else{
            String allFileNames= "";
            for (String key : fileMap.keySet()) {
                allFileNames += key + '0'; //check if we need to add \0
            } 
            byte[] allFileNamesBytes = allFileNames.getBytes();
            byte[] dataPacket;
            short index=0;
            while(allFileNamesBytes.length > packetSize*index){
                dataPacket = DATASend(index, allFileNamesBytes, packetSize*index, true);
                connections.send(connectionId, dataPacket);
                index++;
            }

        }
    }


    private void sevenLOGRQ(byte[] message)	{  

        int errNum = -1;
        userName = new String(message, 1, message.length-1, StandardCharsets.UTF_8);
        //if the user is already logged in
        if(holder.logedInUserNames.containsKey(connectionId)){ 
            errNum = 7;
        }
        else{
            //if the user name is taken
            if(holder.logedInUserNames.containsValue(userName)){
                //user name not valid
                errNum = 0;
            }
            else{
                if(errNum == -1)
                //add the user to the connectionsMap
				holder.logedInUserNames.put(connectionId, userName);
				holder.printMap(); //remember to remove
                //start(connectionId, connections); 
                connections.send(connectionId, ACKSend((short)0));
			
            } 
        }
        if(errNum != -1){
            connections.send(connectionId, ERRORSend(errNum));
    }
}


    private void eightDELRQ(byte[] message){
        int errNum = -1;
        //if the user is not logged in
        if(!holder.logedInUserNames.containsKey(connectionId)){
            errNum = 6;
        }
        //loged in
        else{
            boolean fileFound = false;
            String fileName = new String(message, 1,message.length, StandardCharsets.UTF_8);

            for (String key : fileMap.keySet()) {
                if (key.equals(fileName)) {
                    fileMap.remove(key);
                    fileFound = true;
                    break; // Exit the loop once the file is found
                }
            }
            if(!fileFound){
                errNum = 1;
            }
        }
        if(errNum != -1){
            connections.send(connectionId, ERRORSend(errNum));
        }
        else{
            connections.send(connectionId, ACKSend((short)0));         
            nineSendBroadcast( fileNameString.getBytes() ,0);
        }
    }


    private void tenDISC(byte[] message){
		//if the user is not logged in
        if(!holder.logedInUserNames.containsKey(connectionId)){
            connections.send(connectionId,ERRORSend(6));
        }
        else{
            shouldTerminate = true;
            shouldTerminate();
            connections.send(connectionId, ACKSend((short)0));
        }
    }


    private void nineSendBroadcast( byte[] fileNametoUpload ,int flag){
        byte[] announce = new byte[fileNametoUpload.length + 4]; 
        announce[0] = (byte)((0 >> 8) & 0xff);  
        announce[1] = (byte)((9 >> 8) & 0xff);  
        announce[2] = (byte)((flag >> 8) & 0xff); // 0 for added 
        for(int i = 0; i < fileNametoUpload.length; i++){
            announce[i+3] = fileNametoUpload[i];
        }
        announce[announce.length-1] = 0;

        for(int i = 0; i < holder.logedInUserNames.size(); i++){
                connections.send(i, announce);
        }
    }


    private byte[] ERRORSend(int numError){
        String err0= "Not defined, see error message (if any).";
        String err1= "File not found -RRQ, DELRQ of non existing file";
        String err2= "Access violation.";
        String err3= "Disk full or allocation exceeded.";
        String err4= "Illegal TFTP operation.";
        String err5= "File already exists- file name exists for WRQ.";
        String err6= "User not logged in -any opcode received before Login completes.";
        String err7= "User already logged in -Login username already connected.";

        //convert string to byte array
        byte[] error0 = err0.getBytes();
        byte[] error1 = err1.getBytes();
        byte[] error2 = err2.getBytes();
        byte[] error3 = err3.getBytes();
        byte[] error4 = err4.getBytes();
        byte[] error5 = err5.getBytes();
        byte[] error6 = err6.getBytes();
        byte[] error7 = err7.getBytes();
        
        //create the error response in the size of the error message
        byte[] ERRORSend;
        if(numError == 0)
            ERRORSend = new byte[5 + error0.length];
        else if(numError == 1)
            ERRORSend = new byte[5 + error1.length];
        else if(numError == 2)
            ERRORSend = new byte[5 + error2.length];
        else if(numError == 3)
            ERRORSend = new byte[5 + error3.length];
        else if(numError == 4)
            ERRORSend = new byte[5 + error4.length];
        else if(numError == 5)
            ERRORSend = new byte[5 + error5.length];
        else if(numError == 6)
            ERRORSend = new byte[5 + error6.length];
        else
            ERRORSend = new byte[5 + error7.length];
        
        //insert values to the error response
        ERRORSend[0] = (byte)((0 >> 8) & 0xff); 
        ERRORSend[1] = (byte)((5 >> 8) & 0xff);
        ERRORSend[2] = (byte)((0 >> 8) & 0xff); 
        ERRORSend[3] = (byte)((numError >> 8) & 0xff);
        for(int i = 0; i < error0.length; i++){
            ERRORSend[i+4] = error0[i];
        }
        ERRORSend[ERRORSend.length-1] = 0;
        return ERRORSend;
    }


    private byte[] ACKSend(short blockNum){
        byte[] ack = new byte[4];
		byte a = 4;
		byte b = 0;
        ack[0] = b; 
        ack[1] = a;
        ack[2] = b; 
        ack[3] = (byte)blockNum;
        return ack;
    }


    private byte[] DATASend(short blockNum, byte[] data , int indexData, boolean isList){
        //create data packet in the size of the packet remain to send
        int min = Math.min(packetSize, data.length - indexData);
        min = min + 6;
        byte[] dataPacket = new byte[min];
        if(isList){
            for(int i = 0; i < dataPacket.length; i++){
                dataPacket[i] = data[indexData +i];
            }
            return dataPacket;
        }
        else{
			byte a = 3;
			byte b = 0;
            dataPacket[0] = b;
            dataPacket[1] = a;
            dataPacket[2] = b;
            dataPacket[3] = (byte)dataPacket.length;
            dataPacket[4] = b;
            dataPacket[5] = (byte)blockNum;
            for(int i = 6; i < dataPacket.length; i++){
                dataPacket[i] = data[indexData +i];
            }
            return dataPacket;
        }
    }
    

    private short byteToShort(byte[] byteArr, int fromIndex, int toIndex){
        return (short) (((short) byteArr [fromIndex]) << 8 | (short) (byteArr [toIndex])); 
    }

}
