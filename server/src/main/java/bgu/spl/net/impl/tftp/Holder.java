package bgu.spl.net.impl.tftp;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class Holder {
	public static ConcurrentHashMap<Integer, String> logedInUserNames = new ConcurrentHashMap<>(); 
	public static ConcurrentHashMap<String, File> fileMap = new ConcurrentHashMap<>(); 


	static void printMap(){
		for (ConcurrentHashMap.Entry<Integer, String> entry : logedInUserNames.entrySet()) {
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		}
	}
}
