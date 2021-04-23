package socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class clientThread extends Thread {
	private Socket socket;
	private BufferedReader input;
	
	public clientThread(Socket soc) throws IOException {
		this.socket = soc;
		this.input = new BufferedReader( new InputStreamReader(socket.getInputStream()));
	}
	
	@Override
	public void run() {

		try {
			while(true) {
				String response = input.readLine();
				System.out.println(response);
			}
		
		}catch (IOException ioe) {
			ioe.printStackTrace();
		}finally {
			try {
				input.close();
				
			}catch(Exception ioe) {
				ioe.printStackTrace();
			}
		}
	}
}
