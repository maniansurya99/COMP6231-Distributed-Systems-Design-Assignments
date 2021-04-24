package common;

import java.io.*;

import java.util.*;

/**
 * Distributed filesystem paths.
 * 
 * <p>
 * Objects of type <code>Path</code> are used by all filesystem interfaces. Path
 * objects are immutable.
 * 
 * <p>
 * The string representation of paths is a forward-slash-delimeted sequence of
 * path components. The root directory is represented as a single forward slash.
 * 
 * <p>
 * The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
 * not permitted within path components. The forward slash is the delimeter, and
 * the colon is reserved as a delimeter for application use.
 */
public class Path implements Iterable<String>, Comparable<Path>, Serializable {
	/** Creates a new path which represents the root directory. */

	public String name = null;

	public Path() {

		this.name = "/";
	}

	/**
	 * Creates a new path by appending the given component to an existing path.
	 * 
	 * @param path      The existing path.
	 * @param component The new component.
	 * @throws IllegalArgumentException If <code>component</code> includes the
	 *                                  separator, a colon, or
	 *                                  <code>component</code> is the empty string.
	 */
	public Path(Path path, String component) {

		// Exception checks

		if (component.contains("/") || component.contains(":") || component.length() == 0) {
			throw new IllegalArgumentException("Incorrect Argument");
		}

		// if path is root, concatenate root and component and return

		if (path.isRoot()) {
			this.name = path.toString() + component;
		}

		else
			this.name = path.toString() + "/" + component;

	}

	/**
	 * Creates a new path from a path string.
	 * 
	 * <p>
	 * The string is a sequence of components delimited with forward slashes. Empty
	 * components are dropped. The string must begin with a forward slash.
	 * 
	 * @param path The path string.
	 * @throws IllegalArgumentException If the path string does not begin with a
	 *                                  forward slash, or if the path contains a
	 *                                  colon character.
	 */
	public Path(String path) {
		// Exception checks
		if (path.length() == 0) {
			throw new IllegalArgumentException("Incorrect Argument");
		}

		if (!path.substring(0, 1).equals("/") || path.contains(":")) {

			throw new IllegalArgumentException("Incorrect Argument");
		}

		// Make sure all empty components are removed
		String temp = path.replaceAll("(.)\\1{1,}", "$1");

		if ((temp.length() != 1) && (temp.lastIndexOf("/") == temp.length() - 1)) {
			this.name = temp.substring(0, temp.length() - 1);
		}

		// if root case
		else
			this.name = temp;
	}

	/**
	 * Returns an iterator over the components of the path.
	 * 
	 * <p>
	 * The iterator cannot be used to modify the path object - the
	 * <code>remove</code> method is not supported.
	 * 
	 * @return The iterator.
	 */
	@Override
	public Iterator<String> iterator() {

		return new StringIterator();

	}

	// Iterator class that implements a string iterator
	public class StringIterator implements Iterator<String> {

		int position = 0;

		// Removing first "/"
		String temp = name.substring(1, name.length());

		// Convert path to a list of components
		String[] path_components = temp.split("/");

		@Override
		// Check iterator has a next component
		public boolean hasNext() {

			// check if pos < length of array, if yes return true
			if (position <= path_components.length - 1) {

				return true;
			} else
				return false;
		}

		@Override
		// Return next component of iterator
		public String next() {

			// If has next, return component with index pos++
			if (this.hasNext()) {

				return path_components[position++];
			}

			// If not found, throw exception
			else {

				throw new NoSuchElementException();

			}
		}

		@Override
		public void remove() {

			throw new UnsupportedOperationException();

		}

	}

	/**
	 * Lists the paths of all files in a directory tree on the local filesystem.
	 * 
	 * @param directory The root directory of the directory tree.
	 * @return An array of relative paths, one for each file in the directory tree.
	 * @throws FileNotFoundException    If the root directory does not exist.
	 * @throws IllegalArgumentException If <code>directory</code> exists but does
	 *                                  not refer to a directory.
	 */
	public static Path[] list(File directory) throws FileNotFoundException {

		// Check if directory exists
		if (!directory.exists()) {
			throw new FileNotFoundException("root does not exist");
		}

		// Check if directory
		if (!directory.isDirectory()) {
			throw new IllegalArgumentException("Not a directory");
		}

		// Create new array list to store list of paths
		ArrayList<Path> temp = new ArrayList<>();

		// Set current path to root
		Path currentdir = new Path();

		// List all files starting from directory
		recursiveList(directory, currentdir, temp);

		// Convert array list to array of paths

		Path[] p = new Path[temp.size()];

		for (int j = 0; j < temp.size(); j++) {

			p[j] = temp.get(j);

		}

		return p;

	}

	/*
	 * recursiveList - recursively lists all files starting from directory and saves
	 * it in p currdir argument allows to maintain a full path to each fill
	 */

	public static void recursiveList(File directory, Path currentdir, ArrayList<Path> p) {

		// Get list of all files from directory
		File[] files = directory.listFiles();

		// Get list of all file names from directory
		String[] file_names = directory.list();

		// Go through each file in filelist of directory and add path to p
		for (int i = 0; i < files.length; i++) {

			// If file is directory, step in to the directory
			if (files[i].isDirectory()) {

				// Alter current path
				Path temp_dir = new Path(currentdir + "/" + file_names[i]);

				recursiveList(files[i], temp_dir, p);
			}

			// If file is a file, then add to the list of paths
			if (files[i].isFile()) {

				Path new_path = new Path(currentdir, file_names[i]);

				p.add(new Path(currentdir, file_names[i]));

			}

		}

	}

	/**
	 * Determines whether the path represents the root directory.
	 * 
	 * @return <code>true</code> if the path does represent the root directory, and
	 *         <code>false</code> if it does not.
	 */
	public boolean isRoot() {
		return this.name.equals("/");
	}

	/**
	 * Returns the path to the parent of this path.
	 * 
	 * @throws IllegalArgumentException If the path represents the root directory,
	 *                                  and therefore has no parent.
	 */
	public Path parent() {
		// If root, throw exception
		if (this.name.equals("/")) {
			throw new IllegalArgumentException("Root does not have parent");
		}

		int fwdslash_index = this.name.lastIndexOf("/");

		// If parent is root
		if (fwdslash_index == 0) {
			return new Path();
		}

		// return a new path with path name from start to last index of "/" (eliminating
		// the last component)
		return new Path(this.name.substring(0, fwdslash_index));

	}

	/**
	 * Returns the last component in the path.
	 * 
	 * @throws IllegalArgumentException If the path represents the root directory,
	 *                                  and therefore has no last component.
	 */
	public String last() {
		if (this.name.equals("/")) {
			throw new IllegalArgumentException("Root does not have parent");
		}

		int fwdslash_index = this.name.lastIndexOf("/");

		// Slice from last "/" to end and return new path with that slice
		return this.name.substring(fwdslash_index + 1, this.name.length());
	}

	/**
	 * Determines if the given path is a subpath of this path.
	 * 
	 * <p>
	 * The other path is a subpath of this path if is a prefix of this path. Note
	 * that by this definition, each path is a subpath of itself.
	 * 
	 * @param other The path to be tested.
	 * @return <code>true</code> If and only if the other path is a subpath of this
	 *         path.
	 */
	public boolean isSubpath(Path other) {

		if (this.name.contains(other.name)) {
			return true;
		}

		else
			return false;
	}

	/**
	 * Converts the path to <code>File</code> object.
	 * 
	 * @param root The resulting <code>File</code> object is created relative to
	 *             this directory.
	 * @return The <code>File</code> object.
	 */
	public File toFile(File root) {
		// Create new file object with path of root
		return new File(root.getPath());
	}

	/**
	 * Compares two paths for equality.
	 * 
	 * <p>
	 * Two paths are equal if they share all the same components.
	 * 
	 * @param other The other path.
	 * @return <code>true</code> if and only if the two paths are equal.
	 */
	@Override
	public boolean equals(Object other) {
		return this.name.equals(other.toString());

	}

	/** Returns the hash code of the path. */
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	/**
	 * Converts the path to a string.
	 * 
	 * <p>
	 * The string may later be used as an argument to the <code>Path(String)</code>
	 * constructor.
	 * 
	 * @return The string representation of the path.
	 */
	@Override
	public String toString() {

		return this.name;
	}

	@Override
	public int compareTo(Path o) {

		if (o == null) {
			throw new NullPointerException();
		}

		if (this.name.equals(o.name)) {
			return 0;
		}
		
		if (o.name.indexOf(this.name) == 0) {
			return -1;
		}

		return 1;
	}
}
