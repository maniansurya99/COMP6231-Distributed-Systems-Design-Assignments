package naming;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;

import rmi.*;
import common.*;
import storage.*;

/**
 * Naming server.
 * 
 * <p>
 * Each instance of the filesystem is centered on a single naming server. The
 * naming server maintains the filesystem directory tree. It does not store any
 * file data - this is done by separate storage servers. The primary purpose of
 * the naming server is to map each file name (path) to the storage server which
 * hosts the file's contents.
 * 
 * <p>
 * The naming server provides two interfaces, <code>Service</code> and
 * <code>Registration</code>, which are accessible through RMI. Storage servers
 * use the <code>Registration</code> interface to inform the naming server of
 * their existence. Clients use the <code>Service</code> interface to perform
 * most filesystem operations. The documentation accompanying these interfaces
 * provides details on the methods supported.
 * 
 * <p>
 * Stubs for accessing the naming server must typically be created by directly
 * specifying the remote network address. To make this possible, the client and
 * registration interfaces are available at well-known ports defined in
 * <code>NamingStubs</code>.
 */

public class NamingServer implements Service, Registration {

	public static double ALPHA = 0.3;

	/*
	 * Naming server has - service skeleton for client - registration skeleton for
	 * storage server - List of storage stubs and list of command stubs (to check if
	 * storage server registers twice - Directory Tree (tree of files)
	 */

	Skeleton<Service> service_skeleton = null;
	Skeleton<Registration> registration_skeleton = null;

	Branch tree;

	ArrayList<Command> command_stubs = null;
	ArrayList<Storage> storage_stubs = null;

	/**
	 * Creates the naming server object.
	 * 
	 * <p>
	 * The naming server is not started.
	 * 
	 */
	public NamingServer() {
		// Create new skeletons with service and registration interfaces

		InetSocketAddress service_address = new InetSocketAddress(NamingStubs.SERVICE_PORT);
		this.service_skeleton = new Skeleton(Service.class, this, service_address);

		InetSocketAddress registration_address = new InetSocketAddress(NamingStubs.REGISTRATION_PORT);
		this.registration_skeleton = new Skeleton(Registration.class, this, registration_address);

		// Create root node
		LinkedList<Lock> list = new LinkedList<Lock>();
		this.tree = new Branch("/", list);

		// Create command and storage stub lists
		storage_stubs = new ArrayList<Storage>();
		command_stubs = new ArrayList<Command>();

	}

	/**
	 * Starts the naming server.
	 * 
	 * <p>
	 * After this method is called, it is possible to access the client and
	 * registration interfaces of the naming server remotely.
	 * 
	 * @throws RMIException If either of the two skeletons, for the client or
	 *                      registration server interfaces, could not be started.
	 *                      The user should not attempt to start the server again if
	 *                      an exception occurs.
	 */
	public synchronized void start() throws RMIException {
		// Start the skeletons
		try {
			this.service_skeleton.start();
			this.registration_skeleton.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new RMIException("Naming Server could not be started");
		}
	}

	/**
	 * Stops the naming server.
	 * 
	 * <p>
	 * This method waits for both the client and registration interface skeletons to
	 * stop. It attempts to interrupt as many of the threads that are executing
	 * naming server code as possible. After this method is called, the naming
	 * server is no longer accessible remotely. The naming server should not be
	 * restarted.
	 */
	public void stop() {
		// Check if skeletons are null, and then close
		if (service_skeleton != null && registration_skeleton != null) {
			this.service_skeleton.stop();
			this.registration_skeleton.stop();
		}

		stopped(null);

	}

	/**
	 * Indicates that the server has completely shut down.
	 * 
	 * <p>
	 * This method should be overridden for error reporting and application exit
	 * purposes. The default implementation does nothing.
	 * 
	 * @param cause The cause for the shutdown, or <code>null</code> if the shutdown
	 *              was by explicit user request.
	 */
	protected void stopped(Throwable cause) {
	}

	// The following methods are documented in Service.java.

	@Override

	public boolean isDirectory(Path path) throws FileNotFoundException {

		// Null check
		if (path == null) {
			throw new NullPointerException();
		}

		// if root, then its a directory, return true;
		if (path.isRoot()) {
			return true;
		}

		// Go through the path and check if each components (node) exists, if not throw
		// exception
		Node dir = this.tree;

		Iterator<String> itr = path.iterator();

		while (itr.hasNext()) {

			String component = itr.next();

			// if component is a file (file), return false
			if (get_branch(dir, component) instanceof Leaf) {
				return false;
			}

			// if component does not exist, throw exception
			if (get_branch(dir, component) == null) {

				throw new FileNotFoundException("File not found");

			}

			// Go to next node
			else
				dir = get_branch(dir, component);

		}
		// if at the end the node is leaf (file) return false
		if (dir instanceof Leaf) {
			return false;
		}

		else
			return true;

	}

	@Override
	public String[] list(Path directory) throws FileNotFoundException {

		// Null check
		if (directory == null) {
			throw new NullPointerException();
		}

		Branch curr_dir = this.tree;

		// list immediate files from root
		if (directory.name.equals("/")) {

			curr_dir = this.tree;

			// Create list of string to store paths in
			ArrayList<String> temp = new ArrayList<>();

			for (int k = 0; k < curr_dir.node_list.size(); k++) {

				// Add each file from the node of the directory
				temp.add(curr_dir.node_list.get(k).name);
			}

			// Convert array list into array of strings
			String[] p = new String[temp.size()];

			for (int j = 0; j < temp.size(); j++) {

				p[j] = temp.get(j);

			}

			return p;

		}

		// If directory not root
		else {

			Node dir = this.tree;

			Iterator<String> itr = directory.iterator();

			// Following code block goes to the node representing the directory

			while (itr.hasNext()) {

				String component = itr.next();

				if (get_branch(dir, component) == null) {
					throw new FileNotFoundException("File is given");
				}

				if (get_branch(dir, component) instanceof Leaf) {
					throw new FileNotFoundException("File is given");
				}

				else
					dir = get_branch(dir, component);

			}

			// Node reached, save all immediate paths

			ArrayList<String> temp = new ArrayList<>();

			for (int k = 0; k < ((Branch) dir).node_list.size(); k++) {

				temp.add(((Branch) dir).node_list.get(k).name);
			}

			String[] p = new String[temp.size()];

			for (int j = 0; j < temp.size(); j++) {

				p[j] = temp.get(j);

			}

			return p;

		}

	}

	@Override
	public boolean createFile(Path file) throws RMIException, FileNotFoundException {
		// Null Check
		if (file == null) {
			throw new NullPointerException();
		}

		// Cant create root
		if (file.isRoot()) {
			return false;
		}

		// Check if parent is directory
		if (!isDirectory(file.parent())) {

			throw new FileNotFoundException();
		}

		/*
		 * if parent is root, and if file doesn't exist add to new leaf (file) to
		 * node_list of root with storage stub and tell storage server to create file on
		 * its end using command stub
		 */
		if (file.parent().isRoot()) {

			if (get_branch(this.tree, file.last()) == null) {
				LinkedList<Lock> list = new LinkedList<Lock>();
				this.tree.node_list.add(new Leaf(file.last(), command_stubs.get(0), storage_stubs.get(0), list));
				command_stubs.get(0).create(file);
				return true;
			}

			else
				return false;
		}

		else {

			Node cur_dir = this.tree;

			// Create iterator on parent path
			Iterator<String> itr = file.parent().iterator();

			// Go through path and check if all components (nodes) exist
			while (itr.hasNext()) {

				String component = itr.next();

				if (get_branch((Branch) cur_dir, component) == null) {

					throw new FileNotFoundException("Not found");

				}

				cur_dir = get_branch(cur_dir, component);
			}

			// If the current node is a leaf, the return false
			if (cur_dir instanceof Leaf) {
				return false;
			}

			/*
			 * if file doesn't exist in current node, add to new leaf (file) to node_list of
			 * node with storage stub and tell storage server to create file on its end
			 * using command stub
			 */
			if (get_branch((Branch) cur_dir, file.last()) == null) {

				LinkedList<Lock> list = new LinkedList<Lock>();
				((Branch) cur_dir).node_list
						.add(new Leaf(file.last(), command_stubs.get(0), storage_stubs.get(0), list));
				command_stubs.get(0).create(file);

				return true;

			}

			return false;
		}
	}

	@Override
	public boolean createDirectory(Path directory) throws FileNotFoundException {

		// Null check
		if (directory == null) {
			throw new NullPointerException();
		}

		// Cannot create directory
		if (directory.isRoot()) {

			return false;
		}

		// Check if parent of directory is a directory
		if (!isDirectory(directory.parent())) {

			throw new FileNotFoundException();
		}

		// if parent is directory, check if directory exists in the node_list of root
		// if not create new branch and add to node_list
		if (directory.parent().isRoot()) {

			if (get_branch((Branch) this.tree, directory.last()) == null) {

				LinkedList<Lock> list = new LinkedList<Lock>();
				((Branch) this.tree).node_list.add(new Branch(directory.last(), list));
				return true;

			}

			else {
				return false;
			}

		}

		// Start at root go to parent directory
		Node cur_dir = this.tree;

		Iterator<String> itr = directory.parent().iterator();

		while (itr.hasNext()) {

			String component = itr.next();

			if (get_branch((Branch) cur_dir, component) == null) {

				throw new FileNotFoundException("Not found");

			}

			cur_dir = get_branch(cur_dir, component);

		}

		// if current node is leaf, return false
		if (cur_dir instanceof Leaf) {
			return false;
		}

		// If directory doesnt exists in current node, create new branch and add to
		// node_list
		// of current node and return true
		if (get_branch(cur_dir, directory.last()) == null) {

			LinkedList<Lock> list = new LinkedList<Lock>();
			((Branch) cur_dir).node_list.add(new Branch(directory.last(), list));

			return true;
		}

		else

			return false;

	}

	/*
	 * node_index - returns a index of node from a list of nodes using the name of
	 * the node
	 */
	public int node_index(ArrayList<Node> list, String name) {

		// Null check
		if (list == null) {

			throw new NullPointerException();
		}

		// Go through list and check if name of node == name, if yes then return index
		for (int i = 0; i < list.size(); i++) {

			if (list.get(i).name.equals(name)) {

				return i;
			}

		}

		return -1;
	}

	/*
	 * node_index - returns a node from a list of nodes using the name of the node
	 */
	public Node get_node(ArrayList<Node> list, String name) {

		// Null check

		if (list == null) {

			throw new NullPointerException();
		}

		// Go through list and check if name of node == name, if yes then return index
		for (int i = 0; i < list.size(); i++) {

			if (list.get(i).name.equals(name)) {

				return list.get(i);
			}

		}

		return null;
	}

	@Override
	public boolean delete(Path path) throws FileNotFoundException {

		// Null Check
		if (path == null) {
			throw new NullPointerException("File cannot be null");
		}

		// Check if path exists
		if (!exist(path)) {
			throw new FileNotFoundException("File does not exist");
		}

		// If path is root, cannot delete it
		if (path.isRoot()) {
			return false;
		}

		// If parent is root, call delete_helper with root and file/dir node to be
		// deleted and name of node to be deleted
		if (path.parent().isRoot()) {

			Node prev = tree;

			Node cur = get_node(((Branch) prev).node_list, path.last());

			return delete_helper(path, prev, cur, cur.name);

		}

		// Else go to the node to be deleted and call the delete_helper with parent of
		// node, the node itself and the name of the node to be deleted

		else {

			Node prev = this.tree;

			Iterator<String> itr = path.parent().iterator();

			while (itr.hasNext()) {

				prev = get_branch(prev, itr.next());

			}

			Node curr = get_branch(prev, path.last());

			return delete_helper(path, prev, curr, curr.name);

		}

	}

	/*
	 * delete_helper - deletes a file/dir from the directory tree and informs the
	 * respective storage server to delete the fil/dir as well
	 */
	public synchronized boolean delete_helper(Path path, Node prev, Node curr, String name)
			throws FileNotFoundException {

		// Check if file exists

		if (!exist(path)) {
			throw new FileNotFoundException("File does not exist");
		}

		// Cannot delete root

		if (path.isRoot()) {
			return false;
		}

		// If path leads to a file
		if (!isDirectory(path)) {

			// Get the index of its position in its parents node list
			int index_remove = node_index(((Branch) prev).node_list, name);

			// Get actual node from the parent node list
			Node node = get_node(((Branch) prev).node_list, name);

			try {

				// Tell the storage server where this file is housed to delete the file
				((Leaf) node).command.delete(path);

			} catch (RMIException e) {

			}

			// Command all the other storage servers where the replicas of this file are
			// house to delete the file as well
			if (((Leaf) node).command_list.size() != 0) {

				for (int i = 0; i < ((Leaf) node).command_list.size(); i++) {

					try {
						((Leaf) node).command_list.get(i).delete(path);
					} catch (RMIException e) {

					}

				}

			}

			// Remove the directory tree by removing the node from the parent node list
			((Branch) prev).node_list.remove(index_remove);

			return true;

		}

		// If the path leads to a directory
		if (isDirectory(path)) {

			// Go through the current node list to find the storage servers where this
			// directory is housed
			// and command them to delete the directory as whole

			for (int i = 0; i < ((Branch) curr).node_list.size(); i++) {

				// Get child node from current node list
				Node node = get_node(((Branch) curr).node_list, ((Branch) curr).node_list.get(i).name);

				if (node instanceof Leaf) {

					try {

						// Command directory to be deleted
						((Leaf) node).command.delete(path);

					} catch (RMIException e) {

					}

					// Delete all replicas on the storage servers
					if (((Leaf) node).command_list.size() != 0) {

						for (int j = 0; j < ((Leaf) node).command_list.size(); j++) {

							try {
								((Leaf) node).command_list.get(j).delete(path);
							} catch (RMIException e) {

							}

						}

					}

				}

			}

			// Remove the directory from the directory by getting the index of its position
			// in its parent node list and removing the ndoe at that index

			int index_remove = node_index(((Branch) prev).node_list, name);

			((Branch) prev).node_list.remove(index_remove);

			return true;

		}

		return false;

	}

	@Override
	public Storage getStorage(Path file) throws FileNotFoundException {

		// Null Check
		if (file == null) {
			throw new NullPointerException();
		}

		// Cannot get storage stub for director, only files
		if (isDirectory(file)) {
			throw new FileNotFoundException("Cannot send directories");
		}

		// Iterate through the path until reach leaf (file)
		Iterator<String> itr = file.iterator();

		// Start checking from root
		Node root = this.tree;

		// Get current node
		Node curr_dir = get_branch(root, itr.next());

		if (curr_dir == null) {

			throw new FileNotFoundException();
		}

		// Keep iterating and checking if all nodes (directories) exist
		while (itr.hasNext()) {

			curr_dir = get_branch(curr_dir, itr.next());

			if (curr_dir == null) {

				throw new FileNotFoundException();

			}

		}
		// Found leaf, so return storage stub of leaf
		return ((Leaf) curr_dir).storage;

	}

	/*
	 * get_branch - returns a node from a node given the name of node to be found
	 */
	public static Node get_branch(Node root, String name) {

		Branch temp = (Branch) root;

		ArrayList<Node> list = temp.node_list;

		// Go through list of all nodes of root, if found then return node

		for (int i = 0; i < list.size(); i++) {

			if (list.get(i).name.equals(name)) {

				return (Node) list.get(i);
			}

		}

		return null;

	}

	// The method register is documented in Registration.java.
	@Override
	public Path[] register(Storage client_stub, Command command_stub, Path[] files) {

		// Null Check
		if (client_stub == null || command_stub == null || files == null) {
			throw new NullPointerException("Arg cannot be null");
		}

		// Check if the storage server has already been registered by
		// checking storage stubs in storage stub list

		for (int i = 0; i < this.storage_stubs.size(); i++) {

			if (storage_stubs.get(i).equals(client_stub)) {

				throw new IllegalStateException("Storage Server already start");

			}
		}

		// Add the storage stubs and client stubs
		this.storage_stubs.add(client_stub);

		this.command_stubs.add(command_stub);

		ArrayList<Path> dupe = new ArrayList<>();

		// Create the directory tree and result back a list of all duplicates found
		dupe = create_tree(files, client_stub, command_stub);

		// Convert array list to array
		Path[] duplicate_paths = new Path[dupe.size()];

		for (int i = 0; i < dupe.size(); i++) {

			duplicate_paths[i] = dupe.get(i);

		}

		return duplicate_paths;

	}

	/*
	 * create tree - creates the directory tree of the naming server and returns
	 * back any files that have already been registered
	 */
	public ArrayList<Path> create_tree(Path[] files, Storage stub_storage, Command stub_command) {

		ArrayList<Path> duplicates = new ArrayList<>();

		// Go through all files

		for (int i = 0; i < files.length; i++) {

			// Always start at root node
			Branch curr_node = this.tree;

			Iterator<String> itr = files[i].iterator();

			// Go through each component of path
			while (itr.hasNext()) {

				String next_component = itr.next();

				// If has next, then it is a directory
				if (itr.hasNext()) {

					// Directory already exists, then point to this directory
					if (curr_node.get_directory(next_component) != null) {

						curr_node = (Branch) curr_node.get_directory(next_component);
					}

					// If directory does not exist then create a new branch (directory)
					// add branch to node_list of current directory
					else {

						LinkedList<Lock> list = new LinkedList<Lock>();
						Branch newbranch = new Branch(next_component, list);
						curr_node.node_list.add(newbranch);
						curr_node = newbranch;
					}

				}

				// If doesnt have next, then its a file
				if (!itr.hasNext()) {

					// If node already exists in list of nodes, add to duplicates list
					if (curr_node.get_directory(next_component) != null) {

						duplicates.add(files[i]);

					}

					// Else node does not exist, then create a new leaf (file) with
					// given storage and command stub
					else {

						LinkedList<Lock> list = new LinkedList<Lock>();
						Leaf newleaf = new Leaf(next_component, stub_command, stub_storage, list);
						curr_node.node_list.add(newleaf);

					}

				}

			}

		}
		// Return duplicate files
		return duplicates;

	}

	/*
	 * exist - checks if the file/dir given by path, exists in the directory tree or
	 * not returns true if exists, false otherwise
	 */

	public synchronized boolean exist(Path path) {

		// If path is root, root exists, return true
		if (path.isRoot()) {
			return true;
		}

		// Starting at the root, go through each component of the path and check if each
		// node with the component name exists in the tree

		Node cur_dir = this.tree;

		Iterator<String> itr = path.iterator();

		while (itr.hasNext()) {

			String component = itr.next();

			if (get_branch((Branch) cur_dir, component) == null) {

				// Node is not present, return false
				return false;

			}

			cur_dir = get_branch(cur_dir, component);

		}

		return true;

	}

	/*
	 * lock_node - locks a given node with read/write lock
	 */
	public synchronized void lock_node(Path file, Node node, boolean exclusive) throws RMIException {

		// Check for nullity
		if (node == null) {
			throw new NullPointerException("Path cannot be null");
		}

		// If write lock wanted
		if (exclusive == true) {

			// If locking a file(leaf), invalidate all its replicas by commanding the
			// storage server that hold the replicas to delete the file
			if (node instanceof Leaf) {

				// Reset the number of requests
				((Leaf) node).num_requests = 0;

				// Delete all replicas
				for (int i = 0; i < ((Leaf) node).command_list.size(); i++) {

					((Leaf) node).command_list.get(i).delete(file);
				}

			}

			// If there are no threads currently having the lock of the node, add the lock
			// to the queue, give the lock to thread and return
			if (node.request_list.size() == 0) {

				node.request_list.add(new Lock(Thread.currentThread().getId(), exclusive, 0));

				return;

			}

			else {

				// Add lock to the queue of requests
				node.request_list.add(new Lock(Thread.currentThread().getId(), exclusive, 0));

				// Wait till the request is at the head of the queue
				while (node.request_list.peek().id != Thread.currentThread().getId()) {

					try {
						wait();
					} catch (InterruptedException e) {
					}

				}

				// Request now at head, give the lock to the thread
				return;

			}

		}

		// If read lock wanted
		if (exclusive == false) {

			// If requesting a lock on a file
			if (node instanceof Leaf) {

				// Increase the number of requests for that file
				((Leaf) node).num_requests++;

				// Calculate the number of replicas that are needed
				double requests = ((Leaf) node).num_requests;
				int num_requesters_coarse = (int) (Math.round(requests / 20) * 20);
				int num_replicas = (int) Math.min(ALPHA * num_requesters_coarse, storage_stubs.size());

				// If the num_replicas needed are are greater than the actual number of
				// replicas, create the replicas on other available storage servers
				if (((Leaf) node).num_replicas < num_replicas) {

					((Leaf) node).num_replicas = num_replicas;

					Storage storage = ((Leaf) node).storage;

					// Command other storage servers to copy the original file from the storage
					// server it was housed it
					for (int i = 0; i < storage_stubs.size(); i++) {

						if (!storage_stubs.get(i).equals(storage)) {

							try {

								command_stubs.get(i).copy(file, storage);

							} catch (IOException e) {
							}

							// Add the storage servers to the list of storage_stubs to keep a track of where
							// the files are replicated
							((Leaf) node).storage_list.add(storage_stubs.get(i));
							((Leaf) node).command_list.add(command_stubs.get(i));

							break;

						}

					}

				}

			}

			// Check if request list is empty, add request to request_list and return
			if (node.request_list.size() == 0) {

				node.request_list.add(new Lock(Thread.currentThread().getId(), exclusive, 1));
				return;

			}
			// Check there is only one request and its a read request, increment number of
			// readers for the current reader and return
			if (node.request_list.size() == 1 && (node.request_list.peek().lock == false)) {

				// increment readers and give lock
				node.request_list.peek().readers++;
				return;

			}
			// Check if the last request in the queue is a read, increment the number of
			// readers for the last request and wait till that request is at the head

			if (node.request_list.peekLast().lock == false) {

				// increment readers and wait till at head of queue
				node.request_list.peekLast().readers++;

				while (node.request_list.peek().id != node.request_list.peekLast().id) {

					try {
						wait();
					} catch (InterruptedException e) {
					}

				}

				return;
			}

			// If the last request is a write request, add a new read lock request to the
			// request_list and wait till that request is at the end of the queue
			if (node.request_list.peekLast().lock == true) {

				// add new lock to request list and wait till thread at head of queue
				node.request_list.add(new Lock(Thread.currentThread().getId(), exclusive, 1));

				while (node.request_list.peek().id != Thread.currentThread().getId()) {
					try {
						wait();
					} catch (InterruptedException e) {
					}

				}

				return;

			}
		}

	}

	@Override
	public void lock(Path path, boolean exclusive) throws RMIException, FileNotFoundException {
		// TODO Auto-generated method stub

		// Check for nullity
		if (path == null) {
			throw new NullPointerException("Path cannot be null");
		}

		// Check if path exists
		if (!exist(path)) {
			throw new FileNotFoundException("Path does not exist");
		}

		// If path is root, only root needs to be locked
		if (path.isRoot()) {

			lock_node(path, tree, exclusive);

			return;
		}

		// If parent is root, read lock root then write lock file itself
		if (path.parent().isRoot() && exclusive == true) {

			lock_node(path, tree, !exclusive);
			Iterator<String> itr = path.iterator();
			Node cur_dir = get_branch(tree, itr.next());
			lock_node(path, cur_dir, exclusive);

			return;

		}

		// If parent is root, read lock root then read lock file itself
		if (path.parent().isRoot() && exclusive == false) {

			lock_node(path, tree, exclusive);
			Iterator<String> itr = path.iterator();
			Node cur_dir = get_branch(tree, itr.next());
			lock_node(path, cur_dir, exclusive);

			return;

		}

		// Else go through each node of the parent path and lock them with a read lock
		Node cur_dir = this.tree;
		lock_node(path, tree, false);
		Iterator<String> itr = path.parent().iterator();

		while (itr.hasNext()) {

			cur_dir = get_branch(cur_dir, itr.next());
			lock_node(path, cur_dir, false);

		}

		// Get actual dir/file and lock it with the exclusive boolean lock
		Node node = get_branch(cur_dir, path.last());
		lock_node(path, node, exclusive);

		return;

	}

	public synchronized void unlock_node(Node node, boolean exclusive) throws RMIException {

		// Null check
		if (node == null) {
			throw new NullPointerException();
		}

		// If lock is a write lock, remove the request from the queue and notify all
		// sleeping threads waiting for lock
		if (exclusive) {

			node.request_list.remove();
			notifyAll();

			return;

		}

		// If lock is a read lock, decrement the number of readers
		node.request_list.peek().readers--;

		// if the reader count = 0, there are no more readers and lock needs to be
		// released, so remove the request from the request_list and notify all sleeping
		// threads waiting for the lock
		if (node.request_list.peek().readers == 0) {

			node.request_list.remove();
			notifyAll();

			return;
		}

		// If there are still readers, don't notify
		return;

	}

	@Override
	public void unlock(Path path, boolean exclusive) throws RMIException {

		// Check for nullity
		if (path == null) {

			throw new NullPointerException("Path cannot be null");
		}

		// Check if path exists
		if (!exist(path)) {

			throw new IllegalArgumentException("Path does not exist");
		}

		if (path.isRoot()) {

			unlock_node(tree, exclusive);
			return;
		}

		// Check if parent is root and asking for exclusive lock
		// Read unlock root and write unlock file
		if (path.parent().isRoot() && exclusive == true) {

			unlock_node(tree, false);
			Iterator<String> itr = path.iterator();
			Node cur_dir = get_branch(tree, itr.next());
			unlock_node(cur_dir, exclusive);

			return;

		}

		// Check if parent is root and asking for read lock
		// Read unlock root and file
		if (path.parent().isRoot() && exclusive == false) {

			unlock_node(tree, false);
			Iterator<String> itr = path.iterator();
			Node cur_dir = get_branch(tree, itr.next());
			unlock_node(cur_dir, exclusive);

			return;

		}

		// Else go through each node of the parent path and unlock them with a read lock
		Node cur_dir = tree;
		unlock_node(cur_dir, false);
		Iterator<String> itr = path.parent().iterator();
		
		while (itr.hasNext()) {

			cur_dir = get_branch(cur_dir, itr.next());
			unlock_node(cur_dir, false);

		}
		
		// Get the actual file/dir and unlock it with the exclusive boolean lock
		Node node = get_branch(cur_dir, path.last());
		unlock_node(node, exclusive);
	
		return;

	}
}