package naming;

import java.util.ArrayList;
import java.util.LinkedList;

/* This class reprents a branch in the directory tree 
 * Each branch represents a directory within the directory tree
 * Each branch contains a list of nodes that contains all its immediate directors/files
 */

public class Branch extends Node{
	
	ArrayList<Node> node_list;
	
	// Branch constructor
	public Branch(String name,LinkedList<Lock> list) {
		
		this.name = name;
		this.node_list = new ArrayList<Node>();
		this.request_list = list;
	}
	
	// Returns node with name "name" from branch node_list
	
	public Node get_directory(String name) {
		
		// Go through node list, check if the node names are equal
		for (int i = 0; i < node_list.size(); i++) {
			
			// Return if equal
			if (node_list.get(i).name.equals(name)) {
				
				return node_list.get(i);
			}
			
		}
		
		return null;
	}
	
}
