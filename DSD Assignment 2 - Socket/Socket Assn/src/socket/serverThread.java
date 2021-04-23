package socket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class serverThread extends Thread{
	private Socket socket;
	private ArrayList<serverThread>threadList;
	private PrintWriter output;
	
	public serverThread(Socket soc, ArrayList<serverThread> thrd) {
		this.socket = soc;
		this.threadList = thrd;
	}
	
	@Override
	public void run() {
		try {
			BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			output = new PrintWriter(socket.getOutputStream(),true);
			
			while(true) {
				String outStr = input.readLine();
				
				if(outStr.equals("exit")) {
					break;
					
				}
				printToALlClients(outStr);
				System.out.println("Server connected to Client " + outStr);
				}
			
		}catch(Exception ioe) {
			System.out.println("Error occured "+ ioe.getStackTrace());
		}
	}
	
	private void printToALlClients(String outputString) {
		for (serverThread serverThread: threadList) {
			serverThread.output.println(outputString);
		}
	}
}
