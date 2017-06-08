package org.fogbowcloud.manager.experiments.data;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class CsvGenerator {
	
	public static FileWriter createHeader(String outputFile, String ... labels ) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(outputFile, true);
			for(int i = 0; i < labels.length; i++){
				writer.append(labels[i]);
				if(i < labels.length-1)
					writer.append(",");
			}			
			writer.append('\n');
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writer;
	}
	
	public static void removeLastLine(String outputFile){
		RandomAccessFile f;
		try {
			f = new RandomAccessFile(outputFile, "rw");
			long length = f.length() - 1;
			byte b;
			do {                     
			  length -= 1;
			  f.seek(length);
			  b = f.readByte();
			} while(b != 10);
			f.setLength(length+1);
			f.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static FileWriter getFile(String outputFile) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(outputFile, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writer;
	}
	
	public static void outputValues(FileWriter writer, String ... values){
		try {
			String output = "";
			for(int i = 0; i < values.length; i++){
				output += values[i];
				if(i == values.length-1)
					output += "\n";
				else
					output += ",";
			}
			writer.append(output);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void flushFile(FileWriter writer) {
		try {
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
