package rmi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.Arrays;
import java.lang.reflect.*;

/**
 * RMI skeleton
 * 
 * <p>
 * A skeleton encapsulates a multithreaded TCP server. The server's clients are
 * intended to be RMI stubs created using the <code>Stub</code> class.
 * 
 * <p>
 * The skeleton class is parametrized by a type variable. This type variable
 * should be instantiated with an interface. The skeleton will accept from the
 * stub requests for calls to the methods of this interface. It will then
 * forward those requests to an object. The object is specified when the
 * skeleton is constructed, and must implement the remote interface. Each method
 * in the interface should be marked as throwing <code>RMIException</code>, in
 * addition to any other exceptions that the user desires.
 * 
 * <p>
 * Exceptions may occur at the top level in the listening and service threads.
 * The skeleton's response to these exceptions can be customized by deriving a
 * class from <code>Skeleton</code> and overriding <code>listen_error</code> or
 * <code>service_error</code>.
 */
public class Skeleton<T> {

	// The a listen socket to which Clients connect to
	public ServerSocket listenSocket = null;

	// The address to which the server will 'bind' to
	public InetSocketAddress SkeletonAddress = null;

	// Skeleton Interface
	public Class<T> ServerInterface = null;

	// Object Implementing Skeleton Interface
	public T ServerImpl = null;

	// Boolean to check if skeleton has started
	public boolean isConnected = false;

	/**
	 * Creates a <code>Skeleton</code> with no initial server address. The address
	 * will be determined by the system when <code>start</code> is called.
	 * Equivalent to using <code>Skeleton(null)</code>.
	 * 
	 * <p>
	 * This constructor is for skeletons that will not be used for bootstrapping RMI
	 * - those that therefore do not require a well-known port.
	 * 
	 * @param c      An object representing the class of the interface for which the
	 *               skeleton server is to handle method call requests.
	 * @param server An object implementing said interface. Requests for method
	 *               calls are forwarded by the skeleton to this object.
	 * @throws Error                If <code>c</code> does not represent a remote
	 *                              interface - an interface whose methods are all
	 *                              marked as throwing <code>RMIException</code>.
	 * @throws NullPointerException If either of <code>c</code> or
	 *                              <code>server</code> is <code>null</code>.
	 */

	public Skeleton(Class<T> c, T server) {
		/* Check if c is null */

		if (c == null) {
			throw new NullPointerException("Interface cannot be null");
		}

		/* Check if methods throw RMIException */

		if (!isRemoteInterface(c)) {
			throw new Error("C is not a remote interface");
		}

		/* Check if server is null */

		if (server == null) {
			throw new NullPointerException("Object implementing interface cannot be null ");
		}

		/* Set skeleton attributes */

		InetAddress temp = null;

		this.SkeletonAddress = null;
		this.ServerInterface = c;
		this.ServerImpl = server;

	}

	/**
	 * Creates a <code>Skeleton</code> with the given initial server address.
	 * 
	 * <p>
	 * This constructor should be used when the port number is significant.
	 * 
	 * @param c       An object representing the class of the interface for which
	 *                the skeleton server is to handle method call requests.
	 * @param server  An object implementing said interface. Requests for method
	 *                calls are forwarded by the skeleton to this object.
	 * @param address The address at which the skeleton is to run. If
	 *                <code>null</code>, the address will be chosen by the system
	 *                when <code>start</code> is called.
	 * @throws Error                If <code>c</code> does not represent a remote
	 *                              interface - an interface whose methods are all
	 *                              marked as throwing <code>RMIException</code>.
	 * @throws NullPointerException If either of <code>c</code> or
	 *                              <code>server</code> is <code>null</code>.
	 */
	public Skeleton(Class<T> c, T server, InetSocketAddress address) {
		/* Check for nullity */

		if (c == null) {
			throw new NullPointerException("Interface cannot be null");
		}

		/* Check if methods throw RMIException */

		if (!isRemoteInterface(c)) {
			throw new Error("C is not a remote interface");
		}

		/* Check if server arg is null */
		if (server == null) {
			throw new NullPointerException("Object implementing interface cannot be null ");
		}

		/* Set attributes of Skeleton */
		this.ServerInterface = c;
		this.ServerImpl = server;

		this.SkeletonAddress = address;

	}

	/*
	 * isRemoteInterface - check to see if all methods in testInterface throw an RMI
	 * exception
	 */

	public boolean isRemoteInterface(Class<T> testInterface) {

		// Get methods of interface
		Method[] methods = testInterface.getMethods();

		for (int i = 0; i < methods.length; i++) {

			// get exception of each method
			Class[] exceptions = methods[i].getExceptionTypes();

			String[] tmp = new String[exceptions.length];

			for (int j = 0; j < exceptions.length; j++) {

				tmp[j] = exceptions[j].toString();
			}

			// if exceptions list contains rmi exception, return true
			if ((Arrays.asList(tmp).contains("class rmi.RMIException"))) {
				return true;
			}

		}

		return false;

	}

	/**
	 * Starts the skeleton server.
	 * 
	 * <p>
	 * A thread is created to listen for connection requests, and the method returns
	 * immediately. Additional threads are created when connections are accepted.
	 * The network address used for the server is determined by which constructor
	 * was used to create the <code>Skeleton</code> object.
	 * 
	 * @throws RMIException When the listening socket cannot be created or bound,
	 *                      when the listening thread cannot be created, or when the
	 *                      server has already been started and has not since
	 *                      stopped.
	 * 
	 */

	public synchronized void start() throws RMIException {
		/* Create new listen thread and start it */
		try {

			Thread newListenerThread = new Thread(new Listen(this.SkeletonAddress));
			newListenerThread.start();
		} catch (IOException e) {

			// e.printStackTrace();
			throw new RMIException("Listen thread could not be started");
		}
	}

	/* Class that can be run in a thread to listen to incoming clients */

	private class Listen implements Runnable {

		/*
		 * Create a new listen socket with address given and sets isConnected to true
		 */

		private Listen(InetSocketAddress skeletonAddress) throws IOException {

			try {

				listenSocket = new ServerSocket();
				listenSocket.bind(skeletonAddress);
				SkeletonAddress = (InetSocketAddress) listenSocket.getLocalSocketAddress();

				isConnected = true;

			} catch (Exception e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}

		/*
		 * run() - this function has the logic of the listen thread While the skeleton
		 * is running, it continuously accepts clients and creates a service thread to
		 * service the client
		 */
		public void run() {

			while (isConnected) {

				Socket serviceSocket;

				try {

					serviceSocket = listenSocket.accept();
					Thread newServiceThread = new Thread(new Service(serviceSocket));
					newServiceThread.start();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}

			}

		}

	}

	/* Class that can be run in a thread to service clients */

	private class Service implements Runnable {

		Socket serviceSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		private Service(Socket serviceSocket) throws IOException {

			/* Sets the serviceSocket, in and out attributes from the serviceSocket given */

			this.serviceSocket = serviceSocket;
			this.out = new ObjectOutputStream(this.serviceSocket.getOutputStream());
			this.in = new ObjectInputStream(this.serviceSocket.getInputStream());
			// this.out.flush();

		}

		/*
		 * run() - this function has the logic of the service thread The service thread
		 * reads the method, arg and argtypes from the input stream Gets the method from
		 * the Interface that the skeleton can handle and invokes it Writes the object
		 * to the output stream
		 */

		public void run() {

			try {

				// get method name, arguments, argument types

				String method_name = (String) (in.readObject());
				Object[] Args = (Object[]) (in.readObject());
				Class[] Argtypes = (Class[]) (in.readObject());

				Method m = null;

				Object result_skel = null;

				try {

					// get method from the interface
					m = ServerInterface.getMethod(method_name, Argtypes);

					// invoke method and get result
					result_skel = m.invoke(ServerImpl, Args);

				} catch (Exception e) {

					// System.out.println("SENDING EXCEPTION");

					/* If result was a exception, set result to the exception */
					result_skel = e;

					// System.out.println("this is skel result - " + result_skel.toString());

				}

				out.writeObject(result_skel);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}

			/* After service, close all streams and sockets */

			finally {

				if (in != null && out != null && serviceSocket != null) {
					try {

						serviceSocket.close();
						out.close();
						in.close();

					} catch (IOException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
				}

			}

		}

	}

	/**
	 * Called when the listening thread exits.
	 * 
	 * <p>
	 * The listening thread may exit due to a top-level exception, or due to a call
	 * to <code>stop</code>.
	 * 
	 * <p>
	 * When this method is called, the calling thread owns the lock on the
	 * <code>Skeleton</code> object. Care must be taken to avoid deadlocks when
	 * calling <code>start</code> or <code>stop</code> from different threads during
	 * this call.
	 * 
	 * <p>
	 * The default implementation does nothing.
	 * 
	 * @param cause The exception that stopped the skeleton, or <code>null</code> if
	 *              the skeleton stopped normally.
	 */
	protected void stopped(Throwable cause) {

	}

	/**
	 * Called when an exception occurs at the top level in the listening thread.
	 * 
	 * <p>
	 * The intent of this method is to allow the user to report exceptions in the
	 * listening thread to another thread, by a mechanism of the user's choosing.
	 * The user may also ignore the exceptions. The default implementation simply
	 * stops the server. The user should not use this method to stop the skeleton.
	 * The exception will again be provided as the argument to <code>stopped</code>,
	 * which will be called later.
	 * 
	 * @param exception The exception that occurred.
	 * @return <code>true</code> if the server is to resume accepting connections,
	 *         <code>false</code> if the server is to shut down.
	 */
	protected boolean listen_error(Exception exception) {
		return false;
	}

	/**
	 * Called when an exception occurs at the top level in a service thread.
	 * 
	 * <p>
	 * The default implementation does nothing.
	 * 
	 * @param exception The exception that occurred.
	 */
	protected void service_error(RMIException exception) {

	}

	/**
	 * Stops the skeleton server, if it is already running.
	 * 
	 * <p>
	 * The listening thread terminates. Threads created to service connections may
	 * continue running until their invocations of the <code>service</code> method
	 * return. The server stops at some later time; the method <code>stopped</code>
	 * is called at that point. The server may then be restarted.
	 */
	public synchronized void stop() {
		/* Set skeleton to not running anymore */
		isConnected = false;

		/* Close listen socket */

		if (listenSocket != null) {

			try {
				listenSocket.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}

		stopped(null);

	}

}