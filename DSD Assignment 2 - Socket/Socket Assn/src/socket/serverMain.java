package socket;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class serverMain {
	public static void main(String[] args) {
		ArrayList<serverThread> tList = new ArrayList<>();
		try(ServerSocket serSoc = new ServerSocket(9633)) {
				while(true) {
					Socket soc = serSoc.accept();
					serverThread serThrd = new serverThread(soc, tList);
					tList.add(serThrd);
					serThrd.start();
				}
		}catch(Exception ioe) {
					System.out.println("Error occured in main: "+ioe.getStackTrace());
				}
			}
}
