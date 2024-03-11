package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.io.FileInputStream;
import java.io.File;
import bgu.spl.net.srv.ConnectionHandler;

class holder{
	static ConcurrentHashMap<Integer, String> logedInUserNames = new ConcurrentHashMap<>(); 
	static ConcurrentHashMap<String, File> fileMap = new ConcurrentHashMap<>(); 


	static void printMap(){
		for (ConcurrentHashMap.Entry<Integer, String> entry : logedInUserNames.entrySet()) {
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		}
	}
}


public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private boolean shouldTerminate = false;
    int connectionId;
    // boolean waitingForFile = false; //check	if needed
    String fileNameString;
    private final int packetSize = 512;
    String userName;
	short blockNum = 1;
	byte[] fileToSend;
	//ConcurrentHashMap<String, File> fileMap; 
	Connections<byte[]> connections; 



    public TftpProtocol(){
		//insert all the files from the flies folder in the server into the fileMap
		String folderPath = "server/Flies/";
		File folder = new File(folderPath);
        File[] files = folder.listFiles();
		ConcurrentHashMap<String, File> filesMap = new ConcurrentHashMap<>(); 

        if (files != null) {
            for (File file : files) {
					filesMap.put(file.getName(), file);
				}
            }
        
        holder.fileMap = filesMap; 
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
            threeDATARecive( message);
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
    public boolean 
    shouldTerminate() {
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
		// for (String key : fileMap.keySet()) { 
		// 	if (key.equals(fileName)) {
		String folderPath = "server/Flies/";	
		Path filePath = Paths.get(folderPath,fileName);
		if(Files.exists(filePath)){//PROBLEM
			fileFound = true;
			if(errNum == -1){
				try {
					fileToSend = Files.readAllBytes(filePath);
					byte[] dataPacket = DATASend(blockNum, fileToSend,0); 
					connections.send(connectionId, dataPacket);                            
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// break; // Exit the loop once the file has found
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
		String fileName = new String(message, 1, message.length -1, StandardCharsets.UTF_8);
		fileNameString = fileName;
		String folderPath = "server/Flies/";	
		Path filePath = Paths.get(folderPath,fileName);
		if(Files.exists(filePath)){
			errNum = 5;
		}

        // for (String key : fileMap.keySet()) {
        //     if (key.equals(fileNameString)) {
        //         errNum = 5;
        //         break; // Exit the loop once the file is found
        //     }
        if(errNum != -1){
			connections.send(connectionId, ERRORSend(errNum));
        }
        else {
			connections.send(connectionId, ACKSend((short)0));
			//create a file with the name of the file to upload in the files folder in the server
			File file = new File(folderPath,fileName);
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


    private void threeDATARecive(byte[] message){ //check all write 
		short DATAblockNum = byteToShort(message, 3,4);
		String folderPath = "server/Flies/";	
		Path filePath = Paths.get(folderPath,fileNameString);
		
		byte[] data = new byte[message.length-5];
		for(int i = 0; i < data.length; i++){
			data[i] = message[i+5];
		}

        try {
			Files.write(filePath, data);
        } catch (IOException e) {
        }  

		connections.send(connectionId, ACKSend(DATAblockNum));
		// if(message.length > packetSize){
		// 	connections.send(connectionId, ERRORSend(blockNum));//?????
		// 	blockNum++;
		// }

        if(message.length < packetSize){ 
            nineSendBroadcast(fileNameString.getBytes() ,1);
			//add the new file to the fileMap
			File file = new File(folderPath);
			holder.fileMap.put(fileNameString, file);
			fileNameString = null;
			// blockNum=1;
        }
    }


    private void fourACKRecive(byte[] message){
		short ACKblockNum = byteToShort(message, 1,2); 
		blockNum++;
		//if there is more data packets to send
		if(fileToSend.length > packetSize*ACKblockNum){
			byte[] dataPacket = DATASend(blockNum, fileToSend, packetSize*(blockNum-1));// check if the index is correct
			connections.send(connectionId, dataPacket);
		}
		//if the file has been sent
		else{
			blockNum = 1;
			fileToSend = null;
		}
    }


    private void fiveERRORRecive(byte[] message){
        //TO DO
    }


    private void sixDIRQ(byte[] message){ //check
		//if the user is not logged in
        if(!holder.logedInUserNames.containsKey(connectionId)){
            ERRORSend(6);
        }
        else{
            String allFileNames= "";
            for (String key : holder.fileMap.keySet()) {
                allFileNames += key + '0'; //check if we need to add \0
            } 
            fileToSend = allFileNames.getBytes();
            byte[] dataPacket = DATASend(blockNum, fileToSend, 0);
			connections.send(connectionId, dataPacket);
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
		String fileName = new String(message, 1,message.length-1, StandardCharsets.UTF_8);

        //if the user is not logged in
        if(!holder.logedInUserNames.containsKey(connectionId)){
            errNum = 6;
        }
        //loged in
        else{
            boolean fileFound = false;

            for (String key : holder.fileMap.keySet()) {
                if (key.equals(fileName)) {
                    holder.fileMap.remove(key);
                    fileFound = true;
					//delete the file from the server
					File file = new File("server/Flies/"+fileName);
					file.delete();

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
            nineSendBroadcast( fileName.getBytes() ,0); //check
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


    private void nineSendBroadcast( byte[] fileNametoUploadOrDelete ,int flag){ //0 for delete 1 for added
        byte[] announce = new byte[fileNametoUploadOrDelete.length + 4]; 
        announce[0] = (byte)0;
        announce[1] = (byte)9;  
        announce[2] = (byte)flag;
        for(int i = 0; i < fileNametoUploadOrDelete.length; i++){
            announce[i+3] = fileNametoUploadOrDelete[i];
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
        
        //create the error response in the size of the error message and insert the message 
        byte[] ERRORSend;
        if(numError == 0){
			ERRORSend = new byte[5 + error0.length];
			for(int i = 0; i < error0.length; i++){
				ERRORSend[i+4] = error0[i];
			}
		}
        else if(numError == 1){
			ERRORSend = new byte[5 + error1.length];
			for(int i = 0; i < error1.length; i++){
				ERRORSend[i+4] = error1[i];
			}
		}
        else if(numError == 2){
            ERRORSend = new byte[5 + error2.length];
			for(int i = 0; i < error2.length; i++){
				ERRORSend[i+4] = error2[i];
			}
		}
        else if(numError == 3){
            ERRORSend = new byte[5 + error3.length];
			for(int i = 0; i < error3.length; i++){
				ERRORSend[i+4] = error3[i];
			}
		}
        else if(numError == 4){
            ERRORSend = new byte[5 + error4.length];
			for(int i = 0; i < error4.length; i++){
				ERRORSend[i+4] = error4[i];
			}
		}
        else if(numError == 5){
            ERRORSend = new byte[5 + error5.length];
			for(int i = 0; i < error5.length; i++){
				ERRORSend[i+4] = error5[i];
			}
		}
        else if(numError == 6){
            ERRORSend = new byte[5 + error6.length];
			for(int i = 0; i < error6.length; i++){
				ERRORSend[i+4] = error6[i];
			}
		}
        else{
			ERRORSend = new byte[5 + error7.length];
			for(int i = 0; i < error7.length; i++){
				ERRORSend[i+4] = error7[i];
			}

		}
        
        //insert values to the error response
        ERRORSend[0] = (byte)0;
        ERRORSend[1] = (byte)5;
        ERRORSend[2] = shortTobyte((short)numError)[0];
        ERRORSend[3] = shortTobyte((short)numError)[1];
        ERRORSend[ERRORSend.length-1] = (byte)0;
        return ERRORSend;
    }


    private byte[] ACKSend(short blockNum){ //check
        byte[] ack = new byte[4];
        ack[0] = (byte)0; 
        ack[1] = (byte)4;
        ack[2] = shortTobyte(blockNum)[0];
        ack[3] = shortTobyte(blockNum)[1];
        return ack;
    }


    private byte[] DATASend(short blockNum, byte[] data , int indexData){//check
        //create data packet in the size of the packet remain to send
        int dataSectionSize = Math.min(packetSize, data.length - indexData);
        byte[] dataPacket = new byte[dataSectionSize + 6];
		dataPacket[0] = (byte)0;
		dataPacket[1] = (byte)3;
		byte[] dataSection = shortTobyte((short) dataSectionSize);
		dataPacket[2] = dataSection[0];
		dataPacket[3] = dataSection[1];
		byte[] blockNumber = shortTobyte((short)blockNum);
		dataPacket[4] = blockNumber[0];
		dataPacket[5] = blockNumber[1];
		for(int i = 6; i < dataPacket.length; i++){
			dataPacket[i] = data[indexData+i-6];
		}
		return dataPacket;
    }

    private byte[] shortTobyte(short a){
		return new byte []{(byte)(a >> 8) , (byte)(a & 0xff)};
	}
	
	
    private short byteToShort(byte[] byteArr, int fromIndex, int toIndex){
        return (short) ((((short)(byteArr[fromIndex]) & 0XFF)) << 8 | (short)(byteArr[toIndex] & 0XFF)); 
    }

}
