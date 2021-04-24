package naming;

import java.util.LinkedList;

/* This class defines a node type for the directory tree
 * 
 * The directory tree consists of branches and leafs
 * 
 * - Each branch is a directory and each leaf is a file 
 *
 * - Each branch has a list of nodes, contains all its immediate files/directories
 * 
 * - Each branch/leaf contains a queue of lock objects which is implemented using a linked list
 * 
 * - The request_list hold a list of lock requests that have been requested by multiple threads
*/
public class Node{
		
	//Name of node	
	String name;
	
	//Queue of lock requests
	LinkedList<Lock> request_list;
	
	}

	
	
/*
 * 		Sample Directory Tree:
 * 
 * 					root
 * 					/ \
 * 		   directory1  directory2
 * 				/			\
 * 			subdir1		another_file
 * 			/ 	\
 * 		file3   file4
 * 
 */