package hw4;

import static org.junit.Assert.assertTrue;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import hw1.Database;
import hw1.HeapFile;
import hw1.HeapPage;
import hw1.Tuple;
import net.sf.jsqlparser.schema.Table;

/**
 * BufferPool manages the reading and writing of pages into memory from disk.
 * Access methods call into it to retrieve pages, and it fetches pages from the
 * appropriate location.
 * <p>
 * The BufferPool is also responsible for locking; when a transaction fetches a
 * page, BufferPool which check that the transaction has the appropriate locks
 * to read/write the page.
 */
public class BufferPool {

	private int numPages; // the total number of pages in this buffer pool
	private HashMap<String, HeapPage> cache; // Key will be "tableId:pageId"
	private HashMap<Integer, List<String>> tid_to_pages; // transactionID linked to the list of heappages (using
															// "tableId.pageId")

	// ***** Locks ***** //
	private HashMap<String, List<Integer>> read_locks; // String is the key "tableId:pageId", List<Integer> is the
														// transaction id that get read lock for that heappage
	private HashMap<String, Integer> write_locks; // String is the key "tableId:pageId", Integer is the transaction id
													// that has the write lock for that heappage

	/** Bytes per page, including header. */
	public static final int PAGE_SIZE = 4096;

	/**
	 * Default number of pages passed to the constructor. This is used by other
	 * classes. BufferPool should use the numPages argument to the constructor
	 * instead.
	 */
	public static final int DEFAULT_PAGES = 50;

	/**
	 * Creates a BufferPool that caches up to numPages pages.
	 *
	 * @param numPages maximum number of pages in this buffer pool.
	 */
	public BufferPool(int numPages) {
		// HW4
		this.numPages = numPages;
		this.cache = new HashMap<String, HeapPage>();
		this.tid_to_pages = new HashMap<Integer, List<String>>();

		this.read_locks = new HashMap<String, List<Integer>>();
		this.write_locks = new HashMap<String, Integer>();
	}

	/**
	 * Retrieve the specified page with the associated permissions. Will acquire a
	 * lock and may block if that lock is held by another transaction.
	 * <p>
	 * The retrieved page should be looked up in the buffer pool. If it is present,
	 * it should be returned. If it is not present, it should be added to the buffer
	 * pool and returned. If there is insufficient space in the buffer pool, an page
	 * should be evicted and the new page should be added in its place.
	 *
	 * @param tid     the ID of the transaction requesting the page
	 * @param tableId the ID of the table with the requested page
	 * @param pid     the ID of the requested page
	 * @param perm    the requested permissions on the page
	 */
	public HeapPage getPage(int tid, int tableId, int pid, Permissions perm) throws Exception {
		// HW4
		// Check if this transaction can get a lock on that page, and acquire a lock or
		// wait to abort
		boolean getLock = canGetLock(tid, tableId, pid, perm);
		String key = tableId + ":" + pid;
		if (getLock) {
			if (this.cache.containsKey(key)) { // In cache, just return
				this.putIntoTransactions(tid, tableId, pid);
				return this.cache.get(key);
			} else { // not in cache, read a new one from database and add into cache
				if (this.cache.size() == this.numPages) {
					evictPage(); // evictPage can throw Exception
				}
				// Get the heappage from disk and put into cache
				HeapPage heappage = Database.getCatalog().getDbFile(tableId).readPage(pid);
				this.putIntoTransactions(tid, tableId, pid);
				this.cache.put(key, heappage);
				return heappage;
			}
		} else {
			// if it doesn't get lock, wait until timeout to abort, handle deadlock
			long start_time = System.currentTimeMillis(); // get the current time, used for timeout
			while (getLock == false && (System.currentTimeMillis() - start_time) < 500) // If doesn't get lock and no
																						// timeout keep trying to
																						// getLock
			{
				getLock = canGetLock(tid, tableId, pid, perm);
			}

			// Check if timeout or get the lock
			if (getLock) {
				if (this.cache.containsKey(key)) { // In cache, just return
					this.putIntoTransactions(tid, tableId, pid);
					return this.cache.get(key);
				} else { // not in cache, read a new one from database and add into cache
					if (this.cache.size() == this.numPages) {
						evictPage(); // evictPage can throw Exception
					}

					// Get the heappage from disk and put into cache
					HeapPage heappage = Database.getCatalog().getDbFile(tableId).readPage(pid);
					this.putIntoTransactions(tid, tableId, pid);
					this.cache.put(key, heappage);
					return heappage;
				}
			} else { // cannot get lock before timeout, abort
				transactionComplete(tid, false); // abort
				return null;
			}
		}
	}

	/**
	 * Releases the lock on a page. Calling this is very risky, and may result in
	 * wrong behavior. Think hard about who needs to call this and why, and why they
	 * can run the risk of calling it.
	 *
	 * @param tid     the ID of the transaction requesting the unlock
	 * @param tableID the ID of the table containing the page to unlock
	 * @param pid     the ID of the page to unlock
	 */
	public void releasePage(int tid, int tableId, int pid) {
		// HW4
		String key = tableId + ":" + pid;

		// Remove write lock, need check if the tid is the one has the lock
		if (this.write_locks.containsKey(key) && this.write_locks.get(key) == tid) {
			this.write_locks.remove(key);
		}

		// Remove read_lock
		List<Integer> tid_list;
		if (this.read_locks.containsKey(key)) {
			tid_list = this.read_locks.get(key);
			tid_list.remove(tid);

			if (tid_list.size() == 0) {
				this.read_locks.remove(key);
			}
		}
	}

	/** Return true if the specified transaction has a lock on the specified page */
	public boolean holdsLock(int tid, int tableId, int pid) {
		// HW4
		String key = tableId + ":" + pid;

		// Check if tid holds read lock
		List<Integer> tid_list;
		if (this.read_locks.containsKey(key)) {
			tid_list = this.read_locks.get(key);
			if (tid_list.contains(tid)) {
				return true;
			}
		}

		// Check if tid holds write lock
		if (this.write_locks.containsKey(key)) {
			return this.write_locks.get(key) == tid;
		}
		return false;
	}

	/**
	 * Commit or abort a given transaction; release all locks associated to the
	 * transaction. If the transaction wishes to commit, write
	 *
	 * @param tid    the ID of the transaction requesting the unlock
	 * @param commit a flag indicating whether we should commit or abort
	 */
	public void transactionComplete(int tid, boolean commit) throws IOException {
		// HW4
		if (!this.tid_to_pages.containsKey(tid)) {
			throw new IOException("No such transaction");
		}

		List<String> page_list = this.tid_to_pages.get(tid);
		for (String page_key : page_list) {
			int tableId, pid;
			String[] page_info = page_key.split(":");
			tableId = Integer.parseInt(page_info[0]);
			pid = Integer.parseInt(page_info[1]);

			// Relase lock
			releasePage(tid, tableId, pid);

			HeapPage heappage = this.cache.get(page_key);
			if (commit) { // commit, flush all the dirty page in cache and release all the locks of the
							// transaction
				// Flush dirty page
				if (heappage.isDirty() && heappage.getTranctionId() == tid) {
					flushPage(tableId, pid);
				}
			} else { // abort, remove the dirty page in cache and release all the locks of this
						// transaction
				// Remove dirty page from cache
				if (heappage.isDirty() && heappage.getTranctionId() == tid) {
					this.cache.remove(page_key);
				}
			}
		}
	}

	/**
	 * Add a tuple to the specified table behalf of transaction tid. Will acquire a
	 * write lock on the page the tuple is added to. May block if the lock cannot be
	 * acquired.
	 * 
	 * Marks any pages that were dirtied by the operation as dirty
	 *
	 * @param tid     the transaction adding the tuple
	 * @param tableId the table to add the tuple to
	 * @param t       the tuple to add
	 */
	public void insertTuple(int tid, int tableId, Tuple t) throws Exception {
		// HW4
		int pid = t.getPid();
		String page_key = tableId + ":" + pid;

		// Check if this transaction holds write lock
		if (this.holdsWriteLock(tid, tableId, pid)) {
			HeapPage heappage = this.cache.get(page_key);
			heappage.setDirty(tid, true);
			heappage.addTuple(t);
			this.cache.put(page_key, heappage);
		} else { // try to get the write lock
			this.getPage(tid, tableId, pid, Permissions.READ_WRITE);
		}
	}

	/**
	 * Remove the specified tuple from the buffer pool. Will acquire a write lock on
	 * the page the tuple is removed from. May block if the lock cannot be acquired.
	 *
	 * Marks any pages that were dirtied by the operation as dirty.
	 *
	 * @param tid     the transaction adding the tuple.
	 * @param tableId the ID of the table that contains the tuple to be deleted
	 * @param t       the tuple to add
	 */
	public void deleteTuple(int tid, int tableId, Tuple t) throws Exception {
		// HW4
		int pid = t.getPid();
		String page_key = tableId + ":" + pid;

		// Check if this transaction holds write lock
		if (this.holdsWriteLock(tid, tableId, pid)) {
			HeapPage heappage = this.cache.get(page_key);
			heappage.setDirty(tid, true);
			heappage.deleteTuple(t);
			this.cache.put(page_key, heappage);
		} else { // try to get the write lock
			this.getPage(tid, tableId, pid, Permissions.READ_WRITE);
		}
	}

	/**
	 * Discards a page from the buffer pool. Flushes the page to disk to ensure
	 * dirty pages are updated on disk.
	 */
	private synchronized void flushPage(int tableId, int pid) throws IOException {
		// HW4
		String key = tableId + ":" + pid;
		// do this only when the buffer pool contains the page
		if (cache.containsKey(key)) {
			HeapPage heappage = cache.get(key);
			if (heappage.isDirty()) {
				Database.getCatalog().getDbFile(tableId).writePage(heappage);
				heappage.setDirty(-1, false); // set as clean and clear the transaction id
			}
		} else {
			throw new IOException("No heap page in the cache!");
		}
	}

	/*
	 * * Since space in the BufferPool cache is limited, you may have to sometimes
	 * evict pages when reading in a new page. We want to ensure that we do not
	 * evict any dirty pages, as these pages are currently being worked on by a
	 * transaction. Complete your evict() method such that it evicts the first
	 * non-dirty page that it can find. If no such page exists, throw an exception.
	 * 
	 */
	private synchronized void evictPage() throws Exception {
		// HW4
		String evict_page = null;
		for (Map.Entry<String, HeapPage> cacheEntry : cache.entrySet()) {

			HeapPage heappage = cacheEntry.getValue();
			if (!heappage.isDirty()) {
				evict_page = cacheEntry.getKey();
				cache.remove(evict_page);

				// Remove this page key in tid_to_pages
				for (List<String> page_list : this.tid_to_pages.values()) {
					page_list.remove(evict_page);
				}

				// Remove lock
				this.read_locks.remove(evict_page);
				this.write_locks.remove(evict_page);

				return;
			}
		}

		if (evict_page == null) {
			throw new Exception("All pages are dirty, can not evict page!");
		}
	}

	// Put a specific page into one transaction.
	private void putIntoTransactions(int tid, int tableId, int pid) {
		String page = tableId + ":" + pid;
		List<String> page_list;

		if (this.tid_to_pages.containsKey(tid)) {
			page_list = this.tid_to_pages.get(tid);
			if (!page_list.contains(page)) {
				page_list.add(page);
			}
		} else {
			page_list = new ArrayList<String>();
			page_list.add(page);
			this.tid_to_pages.put(tid, page_list);
		}
	}

	// ***** Locks functions ***** //

	// Check if this transaction can get a lock for "tableId.pid" heappage
	private boolean canGetLock(int tid, int tableId, int pid, Permissions perm) {
		String key = tableId + ":" + pid;
		if (this.write_locks.containsKey(key)) { // There is write lock on that page, can not get lock
			if(this.write_locks.get(key) == tid) {
				return true;
			}
			return false;
		}

		List<Integer> tid_list;
		// No write lock, check the permission this transaction want to get
		if (perm == Permissions.READ_ONLY) { // Read lock request
			if (this.read_locks.containsKey(key)) {
				tid_list = this.read_locks.get(key);

				if (!tid_list.contains(tid)) {
					tid_list.add(tid);
				}
			} else {
				tid_list = new ArrayList<>();
				tid_list.add(tid);
				this.read_locks.put(key, tid_list);
			}
			return true;
		} else { // Write lock request
			if (this.read_locks.containsKey(key)) {
				tid_list = this.read_locks.get(key);
				if (tid_list.size() == 1 && tid_list.get(0) == tid) { // update read lock to write lock
					this.write_locks.put(key, tid);
					this.read_locks.remove(key);
					return true;
				} else {
					return false;
				}
			} else {
				this.write_locks.put(key, tid);
				return true;
			}
		}
	}

	private boolean holdsWriteLock(int tid, int tableId, int pid) {
		String key = tableId + ":" + pid;
		if (this.write_locks.containsKey(key)) {
			return this.write_locks.get(key) == tid;
		}
		return false;
	}

	/** for test **/
	public int getCacheSize() {
		return this.cache.size();
	}
}
