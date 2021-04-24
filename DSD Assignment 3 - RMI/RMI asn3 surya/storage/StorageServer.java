package storage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.net.*;

import common.*;
import rmi.*;
import naming.*;

/**
 * Storage server.
 * 
 * <p>
 * Storage servers respond to client file access requests. The files accessible
 * through a storage server are those accessible under a given directory of the
 * local filesystem.
 */
public class StorageServer implements Storage, Command {

	// Each storage server has a storage skeleton and a command skeleton and a root
	Skeleton<Storage> storage_skeleton = null;
	Skeleton<Command> command_skeleton = null;
	File root;

	/**
	 * Creates a storage server, given a directory on the local filesystem.
	 * 
	 * @param root Directory on the local filesystem. The contents of this directory
	 *             will be accessible through the storage server.
	 * @throws NullPointerException If <code>root</code> is <code>null</code>.
	 */
	public StorageServer(File root) {

		// Null check
		if (root == null) {
			throw new NullPointerException("Root cannot be null");
		}

		// Create skeletons with the appropriate classes and give null address
		this.storage_skeleton = new Skeleton<Storage>(Storage.class, this, null);
		this.command_skeleton = new Skeleton<Command>(Command.class, this, null);

		// Set root
		this.root = root;

	}

	public StorageServer(File root, int port1, int port2) {

		// Null check
		if (root == null) {
			throw new NullPointerException("Root cannot be null");
		}

		// Create skeletons with the appropriate classes and give null address

		InetSocketAddress client_address = new InetSocketAddress(port1);
		InetSocketAddress command_address = new InetSocketAddress(port2);

		this.storage_skeleton = new Skeleton<Storage>(Storage.class, this, client_address);
		this.command_skeleton = new Skeleton<Command>(Command.class, this, command_address);

		// Set root
		this.root = root;

	}

	/**
	 * Starts the storage server and registers it with the given naming server.
	 * 
	 * @param hostname      The externally-routable hostname of the local host on
	 *                      which the storage server is running. This is used to
	 *                      ensure that the stub which is provided to the naming
	 *                      server by the <code>start</code> method carries the
	 *                      externally visible hostname or address of this storage
	 *                      server.
	 * @param naming_server Remote interface for the naming server with which the
	 *                      storage server is to register.
	 * @throws UnknownHostException  If a stub cannot be created for the storage
	 *                               server because a valid address has not been
	 *                               assigned.
	 * @throws FileNotFoundException If the directory with which the server was
	 *                               created does not exist or is in fact a file.
	 * @throws RMIException          If the storage server cannot be started, or if
	 *                               it cannot be registered.
	 */
	public synchronized void start(String hostname, Registration naming_server)
			throws RMIException, UnknownHostException, FileNotFoundException {

		// Null checks
		if (hostname == null || naming_server == null) {
			throw new NullPointerException("Arg is null");
		}

		// Start skeletons
		this.storage_skeleton.start();
		this.command_skeleton.start();

		// Create stubs from the skeletons
		Storage storage_stub = Stub.create(Storage.class, this.storage_skeleton, hostname);
		Command command_stub = Stub.create(Command.class, this.command_skeleton, hostname);

		// List all files on the storage server
		Path[] file_list = Path.list(this.root);

		// Register these files with the naming server and get back duplicates
		Path[] duplicates = naming_server.register(storage_stub, command_stub, file_list);

		// Delete the duplicates and prune empty directories
		for (int i = 0; i < duplicates.length; i++) {

			File file = new File(this.root + duplicates[i].name);

			if (!file.delete()) {

				System.out.println("Could not delete");
			}

			// Pruning empty directories
			else {

				File parent = file.getParentFile();

				int len = parent.listFiles().length;

				while (len == 0) {

					File grand_parent = parent.getParentFile();

					parent.delete();

					parent = grand_parent;

					len = parent.listFiles().length;

				}
			}

		}

	}

	/**
	 * Stops the storage server.
	 * 
	 * <p>
	 * The server should not be restarted.
	 */
	public void stop() {
		// Make sure skeletons arent null and stop them
		if (storage_skeleton != null && command_skeleton != null) {
			storage_skeleton.stop();
			command_skeleton.stop();
		}

		stopped(null);
	}

	/**
	 * Called when the storage server has shut down.
	 * 
	 * @param cause The cause for the shutdown, if any, or <code>null</code> if the
	 *              server was shut down by the user's request.
	 */
	protected void stopped(Throwable cause) {
	}

	// The following methods are documented in Storage.java.
	@Override
	public synchronized long size(Path file) throws FileNotFoundException {
		// Null check
		if (file == null) {
			throw new NullPointerException("Path cannot be null");
		}

		// Create new file object
		File f = new File(this.root + file.name);

		// Check if exists
		if (!f.exists()) {
			throw new FileNotFoundException("File does not exist");
		}

		// Check if directory (size of directory cannot be found)
		if (f.isDirectory()) {
			throw new FileNotFoundException("Cannot find size of a directory");
		}

		// return length
		return f.length();
	}

	@Override
	public synchronized byte[] read(Path file, long offset, int length) throws FileNotFoundException, IOException {
		// Null check
		if (file == null) {
			throw new NullPointerException("Path cannot be null");
		}

		File file_temp = new File(this.root + file.name);

		// Check if exits and not a directory
		if (!file_temp.exists() || file_temp.isDirectory()) {
			throw new FileNotFoundException("File does not exist");
		}

		// Check for offset if out of bounds
		if (length < 0 || offset > file_temp.length() || offset + length > file_temp.length()) {
			throw new IndexOutOfBoundsException("Out of bounds");
		}

		byte[] read_part = null;
		FileInputStream f = null;

		try {

			// Create file input stream for reading
			f = new FileInputStream(file_temp);

			// Create byte array from length
			read_part = new byte[length];

			// Read file into byte array
			f.read(read_part, (int) offset, length);

			// If byte array length is not equal to given length, throw exception
			if (read_part.length != length) {

				throw new IOException("Read could not be completed");

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return read_part;

	}

	@Override
	public synchronized void write(Path file, long offset, byte[] data) throws FileNotFoundException, IOException {
		// Null Checks

		if (file == null) {
			throw new NullPointerException("Path cannot be null");
		}

		if (data == null) {

			throw new NullPointerException("Path cannot be null");
		}

		// Check if offset less that zero (cannot write from negative value)
		if (offset < 0) {
			throw new IndexOutOfBoundsException("Offset cannot be less than 0");
		}

		File file_temp = new File(this.root + file.name);

		// Check if file exists or is a directory
		if (!file_temp.exists() || file_temp.isDirectory()) {
			throw new FileNotFoundException("File does not exist");
		}

		// if offset > temp.length, write to file, a pad of (offset-lengthoffile)
		// then write the given to it

		if (offset > file_temp.length()) {

			// Creat file output stream to write to end of file
			FileOutputStream f = new FileOutputStream(file_temp, true);

			int len = (int) file_temp.length();
			int off = (int) offset;

			int diff = off - len;

			byte[] pad = new byte[diff];

			f.write(pad);

			f.write(data);

			f.close();

		}

		else {

			// Create file out put stream and write to file starting from offset

			FileOutputStream f = new FileOutputStream(file_temp);

			f.write(data, (int) offset, data.length);

			f.close();

		}

	}

	// The following methods are documented in Command.java.
	@Override
	public synchronized boolean create(Path file) {

		// Null check

		if (file == null) {

			throw new NullPointerException("Path cannot be null");
		}

		// System.out.println("this is file - " + file.name );

		// If root, create directory from local path and given path
		if (file.isRoot()) {

			File f = new File(this.root + file.name);

			if (f.mkdir()) {
				return true;
			}

			else
				return false;

		}

		// Create new file object and check if already exists
		File check = new File(this.root + file.name);

		if (check.exists()) {
			return false;
		}

		// Create iterator for path
		boolean result = false;

		Iterator<String> itr = file.iterator();

		String cur_path = "/" + itr.next();

		// Iterates through path and checks if directories exist, if not then create it
		while (itr.hasNext()) {

			File temp = new File(this.root + cur_path);

			if (!temp.exists()) {

				temp.mkdir();
			}

			else {

				cur_path = cur_path + "/" + itr.next();
			}
		}

		// Create new file with cur_path and local path

		File f = new File(this.root + "/" + cur_path);

		try {
			if (f.createNewFile()) {
				result = true;
			}

			else {
				result = false;
			}
		} catch (IOException e) {
		}

		return result;
	}

	@Override
	public synchronized boolean delete(Path path) {
		// Null check
		if (path == null) {

			throw new NullPointerException("Path cannot be null");
		}

		// Cannot delete root
		if (path.isRoot()) {
			return false;
		}

		File delete_file = new File(this.root + path.name);

		// Call delete helper function
		return delete_helper(delete_file);

	}

	/*
	 * delete_helper - recursively deletes all files in a directory and at the end
	 * deletes the directory itself
	 */
	public synchronized boolean delete_helper(File file) {

		// If file does not exist, cannot delete it
		if (!file.exists()) {

			return false;
		}

		// Check if its a file if yes then delete it
		if (file.isFile()) {

			// If could not delete, return false
			return file.delete();
		}

		// Check if directory, if directory is empty, then delete it
		if (file.isDirectory()) {

			if (file.listFiles().length == 0) {

				// If could not delete, return false
				return file.delete();

			}

			else {

				// Get list of all files from current directory
				File[] delete_files = file.listFiles();

				// Call delete on each of the files
				for (File f : delete_files) {

					delete_helper(f);

				}
				// If list of files is zero, delete directory
				if (file.list().length == 0) {

					// If could not delete, return false
					return file.delete();

				}

			}

		}

		return false;

	}

	@Override
	public boolean copy(Path file, Storage server) throws RMIException, FileNotFoundException, IOException {

		// Null check

		if (file == null) {

			throw new NullPointerException("File cannot be null");

		}

		// Null Check
		if (server == null) {

			throw new NullPointerException("Server cannot be null");

		}

		// Check if file exists on the server by trying to get size of file, but if
		// storage throws an exception
		// because file does not exist, then throw a file not found exception
		try {

			long size = server.size(file);

		} catch (FileNotFoundException e) {

			throw new FileNotFoundException("File does not exist on the given storage server");
		}

		// File is present, so get the size of the file and read the contents of the
		// file into a byte array
		long file_size = server.size(file);

		// If file size is less that 1MB
		if (((int) file_size / 1000000) == 0) {

			byte[] result = server.read(file, 0, (int) file_size);

			// Check if the file exists on the current storage server, if yes then overwrite
			// data with the data in the byte array
			File file_temp = new File(this.root + file.name);

			if (file_temp.exists()) {

				FileOutputStream f = new FileOutputStream(file_temp);

				f.write(result, 0, (int) file_size);

				f.close();

				return true;

			}

			// Otherwise file does not exist, so create the file, write to the file with the
			// contents in the byte array and return true
			if (create(file)) {

				write(file, 0, result);

				return true;
			}
		}

		// If the file size is more that 1MB then write to the file in 1MB blocks
		else {

			int num_of_arrays;

			// File size is multiple of 1MB, number of 1MB blocks needed is file size / 1M
			if (((int) file_size % 1000000) == 0) {

				num_of_arrays = (int) file_size / 1000000;
			}

			// If file size is not multiple of 1MB, number of 1MB blocks needed is 
			// (file_size / 1M) + 1
			else {

				num_of_arrays = ((int) file_size / 1000000) + 1;
			}

			// Check if the file exists, if it does, then write 1MB into file, num_of_array
			// times
			File file_temp = new File(this.root + file.name);

			if (file_temp.exists()) {

				int offset = 0;

				for (int i = 0; i < num_of_arrays; i++) {

					byte[] result = server.read(file, offset, 1000000);

					FileOutputStream f = new FileOutputStream(file_temp);

					f.write(result, offset, 1000000);

					f.close();

					offset = offset + 1000000;

				}

				return true;

			}

			// Since file does not exist, create the file
			if (!create(file)) {

				return false;
			}

			// Write 1MB blocks into the file, num_of_array times

			int offset = 0;

			for (int i = 0; i < num_of_arrays; i++) {

				byte[] result = server.read(file, offset, 1000000);

				write(file, offset, result);

				offset = offset + 1000000;

			}

			return true;

		}

		return false;

	}

}