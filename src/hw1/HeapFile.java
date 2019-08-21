/*
 * Team members:
 * 1. Feiyang Yang, 458715
 * 2. Hao Sun, 458716
 * */

package hw1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A heap file stores a collection of tuples. It is also responsible for
 * managing pages. It needs to be able to manage page creation as well as
 * correctly manipulating pages when tuples are added or deleted.
 * 
 * @author Sam Madden modified by Doug Shook
 *
 */

//Low-level: Physical, a real file in disk; High-level: Table object and HeapPage
public class HeapFile {

	public static final int PAGE_SIZE = 4096;

	private TupleDesc tupleDesc;
	private int id;
	private File file;

	/**
	 * Creates a new heap file in the given location that can accept tuples of the
	 * given type
	 * 
	 * @param f     location of the heap file
	 * @param types type of tuples contained in the file
	 */
	public HeapFile(File f, TupleDesc type) {
		// HW1
		this.file = f;
		this.tupleDesc = type;
		this.id = f.hashCode();
	}

	public File getFile() {
		// HW1
		return this.file;
	}

	public TupleDesc getTupleDesc() {
		// HW1
		return this.tupleDesc;
	}

	/**
	 * Creates a HeapPage object representing the page at the given page number.
	 * Because it will be necessary to arbitrarily move around the file, a
	 * RandomAccessFile object should be used here.
	 * 
	 * @param id the page number to be retrieved
	 * @return a HeapPage at the given page number
	 */
	public HeapPage readPage(int id) {
		// HW1
		byte[] content = new byte[HeapFile.PAGE_SIZE];

		long startPosition = PAGE_SIZE * id; // all from 0, so no need to -1
		int tableId = this.getId();

		try {
			RandomAccessFile fileReader = new RandomAccessFile(this.file, "r");
			fileReader.seek(startPosition);
			fileReader.read(content); // read the length of content, PAGE_SIZE here
			fileReader.close();
			HeapPage newPage = new HeapPage(id, content, tableId);
			return newPage;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns a unique id number for this heap file. Consider using the hash of the
	 * File itself.
	 * 
	 * @return
	 */
	public int getId() {
		// HW1
		return this.id;
	}

	/**
	 * Writes the given HeapPage to disk. Because of the need to seek through the
	 * file, a RandomAccessFile object should be used in this method.
	 * 
	 * @param p the page to write to disk
	 */
	public void writePage(HeapPage p) {
		// HeapFile
		// the content to write into disk, the length is PAGE_SIZE
		byte[] content = p.getPageData();
		int pageId = p.getId();
		long startPosition = PAGE_SIZE * pageId; // NOT long startPosition = PAGE_SIZE*(pageId+1);

		try {
			// launch RandomAccessFile object
			RandomAccessFile fileReader = new RandomAccessFile(this.file, "rw");
			fileReader.seek(startPosition);
			fileReader.write(content); // write the length of content, PAGE_SIZE here
			fileReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Adds a tuple. This method must first find a page with an open slot, creating
	 * a new page if all others are full. It then passes the tuple to this page to
	 * be stored. It then writes the page to disk (see writePage)
	 * 
	 * @param t The tuple to be stored
	 * @return The HeapPage that contains the tuple
	 */
	public HeapPage addTuple(Tuple t) {
		//HW1
		int pageIndex = 0;
		int pageNumTotal = this.getNumPages();
		HeapPage curPage = null;

		// loop through every page, and in every page check the header find the empty slot
		while (pageIndex < pageNumTotal) {
			curPage = this.readPage(pageIndex);
			if (curPage == null) {
				continue;
			}
			int slotNumTotal = curPage.getNumSlots();
			int curSlotIndex = 0;

			while (curSlotIndex < slotNumTotal) {
				if (curPage.slotOccupied(curSlotIndex) == false) {
					try {
						curPage.addTuple(t);
						break;
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				curSlotIndex++;
			}

			if (curSlotIndex < slotNumTotal) { //Add success
				break;
			}
			pageIndex++;
		}

		// if not find a slot free and no add,reference to a new page for returning
		if (pageIndex == pageNumTotal) {
			try {
				curPage = new HeapPage(pageNumTotal, new byte[PAGE_SIZE], this.getId());  //pageId, data, fileId or tableId
				curPage.addTuple(t);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// then writes the page to disk (see writePage)
		this.writePage(curPage);
		return curPage;
	}

	/**
	 * This method will examine the tuple to find out where it is stored, then
	 * delete it from the proper HeapPage. It then writes the modified page to disk.
	 * 
	 * @param t the Tuple to be deleted
	 */
	public void deleteTuple(Tuple t) {
		// HW1
		// find the according page then delete
		HeapPage thePage = this.readPage(t.getPid());
		thePage.deleteTuple(t);
		// then writes the modified page to disk.
		this.writePage(thePage);
	}

	/**
	 * Returns an ArrayList containing all of the tuples in this HeapFile. It must
	 * access each HeapPage to do this (see iterator() in HeapPage)
	 * 
	 * @return
	 */
	public ArrayList<Tuple> getAllTuples() {
		// HW1
		ArrayList<Tuple> tuplesArrayLi = new ArrayList<>();
		int pageNumTotal = this.getNumPages();
		int curPageIndex = 0;
		while (curPageIndex < pageNumTotal) {
			Iterator<Tuple> iterator = this.readPage(curPageIndex).iterator();
			while (iterator.hasNext()) {
				Tuple t = iterator.next();
				tuplesArrayLi.add(t);
			}
			curPageIndex++;
		}
		return tuplesArrayLi;
	}

	/**
	 * Computes and returns the total number of pages contained in this HeapFile
	 * 
	 * @return the number of pages
	 */
	public int getNumPages() {
		// HW1
		// From File class
		long fileByteLength = this.file.length();
		int numPages = (int) Math.ceil(fileByteLength / PAGE_SIZE);
		return numPages;

	}
}
