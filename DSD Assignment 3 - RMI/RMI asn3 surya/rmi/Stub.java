package rmi;

import java.net.*;
import java.util.Arrays;
import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;

/**
 * RMI stub factory.
 * 
 * <p>
 * RMI stubs hide network communication with the remote server and provide a
 * simple object-like interface to their users. This class provides methods for
 * creating stub objects dynamically, when given pre-defined interfaces.
 * 
 * <p>
 * The network address of the remote server is set when a stub is created, and
 * may not be modified afterwards. Two stubs are equal if they implement the
 * same interface and carry the same remote server address - and would therefore
 * connect to the same skeleton. Stubs are serializable.
 */
public abstract class Stub {

	/**
	 * Creates ae stub, given a skeleton with an assigned adress.
	 * 
	 * <p>
	 * The stub is assigned the address of the skeleton. The skeleton must either
	 * have been created with a fixed address, or else it must have already been
	 * started.
	 * 
	 * <p>
	 * This method should be used when the stub is created together with the
	 * skeleton. The stub may then be transmitted over the network to enable
	 * communication with the skeleton.
	 * 
	 * @param c        A <code>Class</code> object representing the interface
	 *                 implemented by the remote object.
	 * @param skeleton The skeleton whose network address is to be used.
	 * @return The stub created.
	 * @throws IllegalStateException If the skeleton has not been assigned an
	 *                               address by the user and has not yet been
	 *                               started.
	 * @throws UnknownHostException  When the skeleton address is a wildcard and a
	 *                               port is assigned, but no address can be found
	 *                               for the local host.
	 * @throws NullPointerException  If any argument is <code>null</code>.
	 * @throws Error                 If <code>c</code> does not represent a remote
	 *                               interface - an interface in which each method
	 *                               is marked as throwing
	 *                               <code>RMIException</code>, or if an object
	 *                               implementing this interface cannot be
	 *                               dynamically created.
	 */
	public static <T> T create(Class<T> c, Skeleton<T> skeleton) throws UnknownHostException {

		/* Null checks */
		
		if (c == null) {
			throw new NullPointerException("Interface class cannot be null");
		}

		if (skeleton == null) {
			throw new NullPointerException("Skeleton cannot be null");
		}

		if (skeleton.SkeletonAddress == null) {
			throw new IllegalStateException("Skeleton address has not be assigned");
		}
		
		//Check if skeleton started

		if (!skeleton.isConnected) {
			throw new IllegalStateException("Skeleton has not been started");
		}
		
		// Check if address can be found on local host
		if (skeleton.SkeletonAddress.isUnresolved()) {
			throw new UnknownHostException("Skeleton Address is unresolved");

		}

		//Check if c is a remote interface 
		if (!isRemoteInterface(c)) {
			throw new Error("C is not a remote interface");

		}

		T proxyInstance = null;
		
		//Create new Proxy instance with the skeleton adddress
		try {
			proxyInstance = (T) Proxy.newProxyInstance(c.getClassLoader(), new Class[] { c },
					new ProxyHandler(skeleton.SkeletonAddress));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new Error("Could not create proxy");
			// e.printStackTrace();
		}

		return proxyInstance;

	}

	/**
	 * Creates a stub, given a skeleton with an assigned address and a hostname
	 * which overrides the skeleton's hostname.
	 * 
	 * <p>
	 * The stub is assigned the port of the skeleton and the given hostname. The
	 * skeleton must either have been started with a fixed port, or else it must
	 * have been started to receive a system-assigned port, for this method to
	 * succeed.
	 * 
	 * <p>
	 * This method should be used when the stub is created together with the
	 * skeleton, but firewalls or private networks prevent the system from
	 * automatically assigning a valid externally-routable address to the skeleton.
	 * In this case, the creator of the stub has the option of obtaining an
	 * externally-routable address by other means, and specifying this hostname to
	 * this method.
	 * 
	 * @param c        A <code>Class</code> object representing the interface
	 *                 implemented by the remote object.
	 * @param skeleton The skeleton whose port is to be used.
	 * @param hostname The hostname with which the stub will be created.
	 * @return The stub created.
	 * @throws IllegalStateException If the skeleton has not been assigned a port.
	 * @throws NullPointerException  If any argument is <code>null</code>.
	 * @throws Error                 If <code>c</code> does not represent a remote
	 *                               interface - an interface in which each method
	 *                               is marked as throwing
	 *                               <code>RMIException</code>, or if an object
	 *                               implementing this interface cannot be
	 *                               dynamically created.
	 */
	public static <T> T create(Class<T> c, Skeleton<T> skeleton, String hostname) {

		//Null checks
		
		if (c == null) {
			throw new NullPointerException("Interface class cannot be null");
		}

		if (skeleton == null) {
			throw new NullPointerException("Skeleton cannot be null");
		}

		if (hostname == null) {
			throw new NullPointerException("Hostname cannot be null");
		}

		
		//Check if c is remote interface 
		
		if (!isRemoteInterface(c)) {
			throw new Error("C is not a remote interface");
		}

		//Check if skeleton address port is 0, if yes then set new skeleton address 
		if (skeleton.SkeletonAddress.getPort() == 0) {
			throw new IllegalStateException("Skeleton port not assigned");
		}

		int Port = skeleton.SkeletonAddress.getPort();

		skeleton.SkeletonAddress = new InetSocketAddress(hostname, Port);

		
		//Create new Proxy instance with the skeleton adddress
		T proxyInstance = null;
		try {
			proxyInstance = (T) Proxy.newProxyInstance(c.getClassLoader(), new Class[] { c },
					new ProxyHandler(skeleton.SkeletonAddress));

		} catch (Exception e) {
			throw new Error("Could not create proxy");
		}

		return proxyInstance;

	}

	/**
	 * Creates a stub, given the address of a remote server.
	 * 
	 * <p>
	 * This method should be used primarily when bootstrapping RMI. In this case,
	 * the server is already running on a remote host but there is not necessarily a
	 * direct way to obtain an associated stub.
	 * 
	 * @param c       A <code>Class</code> object representing the interface
	 *                implemented by the remote object.
	 * @param address The network address of the remote skeleton.
	 * @return The stub created.
	 * @throws NullPointerException If any argument is <code>null</code>.
	 * @throws Error                If <code>c</code> does not represent a remote
	 *                              interface - an interface in which each method is
	 *                              marked as throwing <code>RMIException</code>, or
	 *                              if an object implementing this interface cannot
	 *                              be dynamically created.
	 */
	public static <T> T create(Class<T> c, InetSocketAddress address) {
		
		//Null checks
		
		if (c == null) {
			throw new NullPointerException("Interface class cannot be null");
		}

		if (address == null) {
			throw new NullPointerException("Skeleton cannot be null");
		}

		
		//Check if c is a remote interface

		if (!isRemoteInterface(c)) {
			throw new Error("C is not a remote interface");

		}

		//Create new Proxy instance with the skeleton adddress
		T proxyInstance = null;

		try {
			proxyInstance = (T) Proxy.newProxyInstance(c.getClassLoader(), new Class[] { c },
					new ProxyHandler(address));
		} catch (Exception e) {
			throw new Error("Could not create proxy");
		}

		return proxyInstance;

	}

	
	/* isRemoteInterface - Function checks if interface is a remote interface - same functionality of 
	*					   isRemoteInterface in skeleton
	*/
	public static <T> boolean isRemoteInterface(Class<T> testInterface) {

		Method[] methods = testInterface.getMethods();

		for (int i = 0; i < methods.length; i++) {

			Class[] exceptions = methods[i].getExceptionTypes();

			String[] tmp = new String[exceptions.length];

			for (int j = 0; j < exceptions.length; j++) {

				tmp[j] = exceptions[j].toString();
			}

			if ((Arrays.asList(tmp).contains("class rmi.RMIException"))) {
				return true;
			}

		}

		return false;
	}

	/* Class for InvocationHandler for Proxy */
	/* This class implements invoke that tries to invoke a method by sending methodname, args, argtypes
	 * to the skeleton via the outstream
	 */
	
	public static class ProxyHandler implements InvocationHandler, Serializable {

		public InetSocketAddress skeleton_address = null;
		
	
		//Proxy handler constructor
		public ProxyHandler(InetSocketAddress address) {

			this.skeleton_address = address;

		}

		/* 
		 * invoke - checks if method given is local, if yes, executes with in the function
		 * else - marshals the required data (methodname, arg, argtypes) and sends it to skeleton
		 */
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			// Check if local method, if yes then execute
			
			String methodname = method.getName();

			Object result = null;

			if (methodname.equals("equals")) {
				result = this.equals(args[0]);
				return result;
			}

			if (methodname.equals("toString")) {
				result = this.toString();
				return result;

			}

			if (methodname.equals("hashCode")) {
				result = this.hashCode();
				return result;
			}

			//Not a local method
			
			else {
				
				//Create new socket binded to skeleton address
				
				Socket stubSocket = new Socket();

				try {

					stubSocket.connect(skeleton_address);
					
				} catch (IOException e2) {
					
					//e2.printStackTrace();
				}

				
				ObjectOutputStream out_stub = null;
				ObjectInputStream in_stub = null;

				try {
					
					
					//Create output and input streams for marshaling data
					
					out_stub = new ObjectOutputStream(stubSocket.getOutputStream());
					in_stub = new ObjectInputStream(stubSocket.getInputStream());
			
					
					//Write methodname, args, argtypes to outstream
					out_stub.writeObject(methodname);
					out_stub.writeObject(args);
					out_stub.writeObject(method.getParameterTypes());
				
					//Get result back
					result = in_stub.readObject();
					
					//System.out.println("This is stub - " + result);
					

				  //Check for any exceptions
				} catch (Exception e) {

					//e.printStackTrace();
					throw new RMIException("Error in creating input/output streams", e);
				}
				
				//If invoke on the skeleton end caused an exception, throw that exception
				if (result instanceof Throwable) {
					
					throw ((Throwable)result).getCause();

				}

				// Clean up - close all sockets and streams
				if (stubSocket != null && in_stub != null && out_stub != null) {

					try {
						// stubSocket.close();
						stubSocket.close();
						in_stub.close();
						out_stub.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}

				}

	

			return result;
			
			
			}
			
			
			}

		
		// Checks if two stubs are equal
		
		public boolean equals(Object other) {

			if (this == other) {
				return true;
			}
			
			// Null check
			if(other == null || this == null) {

				return false;
			}

			if (!(other instanceof Proxy)) {
				return false;
			}
			
			//Get proxy handler for other object
			ProxyHandler other_handler = (ProxyHandler) Proxy.getInvocationHandler(other);
		
			//If stub address are equal, then return true
			if ((other_handler.skeleton_address.equals(this.skeleton_address))) {
				return true;
			}
			return false;
		}

			

		//Return hashcode of stub
		public int hashCode() {
			return skeleton_address.hashCode();

		}
		//Returns string representation of stub (the skeleton address)
		public String toString() {

			String msg = skeleton_address.toString();
			return msg;

		}

	}

}
