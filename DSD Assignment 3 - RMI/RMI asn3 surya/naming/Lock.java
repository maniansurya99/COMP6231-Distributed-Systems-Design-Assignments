package naming;

import java.util.ArrayList;

/* public class Lock - This class defines a lock object
 * - A lock object consists of an id (id of the thread that requested the lock)
 *   and the type of the lock that is requested
 *   
 * - In the case we need to give read locks to multiple readers, the num_readers 
 *   attribute stores the number of threads that have requested the read lock
 *   
 * - Whenever a read lock is requested, num_readers is initialized to 1, for a write lock the 
 *   num_readers is initialized to 0
 *   
 * read lock creation - new Lock(id, false, 1)
 * write lock create - new Lock(id, true, 0)
 */

public class Lock {

	long id;

	boolean lock;
	
	int readers;
	
	public Lock(long id, boolean lock, int readers) {
		this.id = id;
		this.lock = lock;
		this.readers = readers;
	}

}
