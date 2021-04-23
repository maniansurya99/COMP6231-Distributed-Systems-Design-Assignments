package socket;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class clientMain {
	public static void main(String[] args) {
		try (Socket soc = new Socket("localhost", 9633)){
			PrintWriter output = new PrintWriter(soc.getOutputStream(),true);
			Scanner scan = new Scanner(System.in);
			String userInput;
			String clientName = "empty";
			clientThread clientThread = new clientThread(soc);
			clientThread.start();
			
			do {
				if(clientName.equals("empty")) {
					System.out.println("Enter clients's name:");
					userInput = scan.nextLine();
					clientName = userInput;
					output.println(userInput);
					if (userInput.equals("exit")) {
						break;
					}
				}
				else {
					String chat = ("("+ clientName + ")"+ "'s words : ");
					System.out.println(chat);
					userInput = scan.nextLine();
					output.println(chat + " "+ userInput);
					
					if (userInput.equals("exit")) {
						break;
					}
				}
			}while (!userInput.equals("exit"));
			
			
		}catch (Exception ioe) {
			System.out.println("Exception in client main: "+ioe.getStackTrace());
		}
	}
}
