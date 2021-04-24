package naming;

import java.util.ArrayList;
import java.util.LinkedList;

import rmi.Stub;
import storage.Command;
import storage.Storage;

/*
 * Leaf class for directory tree
 * A leaf represents a file in the tree
 */

public class Leaf extends Node {

	/* Command and storage stubs for accessing the storage server where original
	 * file is held
	 */
	Command command;
	Storage storage;

	/* List of command and storage stubs for accessing the storage servers where the replicas
	 * of file are held
	 */
	ArrayList<Storage> storage_list;
	ArrayList<Command> command_list;

	//number of replicas of file
	int num_replicas;
	
	//number of requests to file
	int num_requests;

	//Leaf constructor for tree
	public Leaf(String name, Command command_stub, Storage storage_stub, LinkedList<Lock> list) {

		this.name = name;
		this.command = command_stub;
		this.storage = storage_stub;
		this.request_list = list;
		this.num_requests = 0;
		this.num_replicas = 0;
		this.storage_list = new ArrayList<Storage>();
		this.command_list = new ArrayList<Command>();

	}

}
