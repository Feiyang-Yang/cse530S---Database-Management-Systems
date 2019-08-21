/*
 * Team members:
 * 1. Feiyang Yang, 458715
 * 2. Hao Sun, 458716
 * */

package hw1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class HeapPage {

	private int id; 		//id of the page
	private byte[] header; 	//manage all the slots of this page
	private Tuple[] tuples; //tuple has one less bit than slot,which is only in conception
	private TupleDesc td; 	
	private int numSlots; 	//how many slots for most in the heapPage
	private int tableId; 	//the id of the table the page belongs to
	
	//HW4
	private boolean isDirty;
	private int tid; 		// transactionId that make this heappage dirty

	public HeapPage(int id, byte[] data, int tableId) throws IOException {
		this.id = id;
		this.tableId = tableId;

		this.td = Database.getCatalog().getTupleDesc(this.tableId);	//Table can be found in Catalog using id, then find the desc
		this.numSlots = getNumSlots();	//Calculate based on desc
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data)); //Input stream

		// allocate and read the header slots of this page
		header = new byte[getHeaderSize()];
		for (int i=0; i<header.length; i++)
			header[i] = dis.readByte();   // update header

		try{
			// allocate and read the actual records of this page
			tuples = new Tuple[numSlots];
			for (int i=0; i<tuples.length; i++)
				tuples[i] = readNextTuple(dis,i);
		}catch(NoSuchElementException e){
			e.printStackTrace();
		}
		dis.close();

		//HW4
		this.isDirty = false; 
		this.tid = -1;			// default -1
	}

	public int getId() {
		//HW1
		return this.id;
	}
	
	public int getTableId() {
		//HW4
		return this.tableId;
	}

	/**
	 * Computes and returns the total number of slots that are on this page (occupied or not).
	 * Must take the header into account!
	 * @return number of slots on this page
	 */
	public int getNumSlots() {
		//HW1
		int pageBitNum = 8 * HeapFile.PAGE_SIZE; 	//Transfer to total bits of a HeapPage
		int tupleBitNum = 8 * td.getSize();			//Total bits of a Tuple
		int slotBitNum = 1 + tupleBitNum;			//Tuple bits plus one bit in Header
		return pageBitNum / slotBitNum;
	}

	/**
	 * Computes the size of the header. Headers must be a whole number of bytes (no partial bytes)
	 * @return size of header in bytes
	 */
	private int getHeaderSize() {        
		//HW1
		//In bytes, getNumSlots() is bits, need divided by 8
		int headNumber = (int) Math.ceil(getNumSlots()/8.0); // int headNumber = (int)Math.ceil(getNumSlots()/8); Not
		return headNumber;
	}

	/**
	 * Checks to see if a slot is occupied or not by checking the header
	 * @param s the slot to test
	 * @return true if occupied
	 */
	public boolean slotOccupied(int s) { //Note: s starts from 0
		//HW1
		if (s < 0 || s >= numSlots) { 
			return false;
		} else {
			int whichByte = s / 8; 	// find which byte this s is in
			int whichBit = s % 8; 	// find the index (from right to left, start from 0)
			
			int shift = 1 << whichBit;
			//now focus on this bit of this byte. if it is occupied, that bit will be 1, after & operation, it is !0
			int bitOperation =  shift & header[whichByte];
			return (bitOperation != 0);
			
			/*
			 * if(whichByte > (header.length-1)) { //Redundant condition
				return false;
			} else {
				int shift = 1 << (whichBit+1);
				//now focus on this bit of this byte
				//if not occupied in this byte originally, that will be 1&0, == 0,we need return false
				int bitOperation =  shift & header[whichByte] ;
				boolean result = !(bitOperation == 0);
				return result;
			}	
			 * */
		}
	}

	/**
	 * Sets the occupied status of a slot by modifying the header
	 * @param s the slot to modify
	 * @param value its occupied status
	 */
	public void setSlotOccupied(int s, boolean value) {
		//your code here
		if (s < 0 || s >= numSlots) {
			throw new NoSuchElementException("error wrong slot input") ;
		} else {
			int whichByte = s / 8; //s starts from 0, so no +1
			int whichBit = s % 8;
			int shift = 1 << whichBit;
			//if false-false,true-true, then no need to change
			//if false-true, true-false, then need to change
			if ((slotOccupied(s) != value)){  //0-1,1-0
				header[whichByte] = (byte)(shift ^ header[whichByte]);
			}
		}
	}
	
	/**
	 * Adds the given tuple in the next available slot. Throws an exception if no empty slots are available.
	 * Also throws an exception if the given tuple does not have the same structure as the tuples within the page.
	 * @param t the tuple to be added.
	 * @throws Exception
	 */
	public void addTuple(Tuple t) throws Exception {
		//HW1
		if(t == null) {
			throw new Exception("Null Tuple");
		} else {
			if(!t.tupleDesc.equals(this.td)) {
				throw new Exception("Wrong Type");
			}else{
				int i = 0;
				int slotLength = this.getNumSlots();
				
				//find the i or out of bound
				while(i < slotLength && this.slotOccupied(i) == true) {
					i++;
				}
				
				if(i >= slotLength) {
					throw new Exception("All occupied");
				} else {
					t.setId(i);  // pageID + slotID for pinpoint location 
					t.setPid(this.id);
					this.setSlotOccupied(i,true);
					this.tuples[i] = t; //this line !
				}
			}
		} 
	}

	/**
	 * Removes the given Tuple from the page. If the page id from the tuple does not match this page, throw
	 * an exception. If the tuple slot is already empty, throw an exception
	 * @param t the tuple to be deleted
	 * @throws Exception
	 */
	public void deleteTuple(Tuple t) {
		//HW1
		if(t == null) {
			try {
				throw new Exception("Null Tuple");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			int currentSlotId = t.getId();
			int TuplePageId = t.getPid();
			if(TuplePageId != id) {
				try {
					throw new Exception("Error: Not in the same page");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				if(!slotOccupied(currentSlotId)) {
					try {
						throw new Exception("Nothing in this Tuple to delete");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				setSlotOccupied(currentSlotId,false);
				tuples[currentSlotId] = null;
			}
		}
	}
	
	/**
     * Suck up tuples from the source file.
     */
	private Tuple readNextTuple(DataInputStream dis, int slotId) {
		// if associated bit is not set, read forward to the next tuple, and
		// return null.
		if (!slotOccupied(slotId)) {	//check header first, if no element just move pointer to next
			for (int i=0; i<td.getSize(); i++) { 	//in bytes
				try {
					dis.readByte();					//move pointer to next 
				} catch (IOException e) {
					throw new NoSuchElementException("error reading empty tuple");
				}
			}
			return null;
		}

		// read fields in the tuple
		Tuple t = new Tuple(td);
		t.setPid(this.id);
		t.setId(slotId);

		for (int j=0; j<td.numFields(); j++) { //Check desc, check fields and type
			if(td.getType(j) == Type.INT) {
				byte[] field = new byte[4];
				try {
					dis.read(field);
					t.setField(j, new IntField(field));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				byte[] field = new byte[129];
				try {
					dis.read(field);
					t.setField(j, new StringField(field));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return t;
	}

	/**
     * Generates a byte array representing the contents of this page.
     * Used to serialize this page to disk.
	 *
     * The invariant here is that it should be possible to pass the byte
     * array generated by getPageData to the HeapPage constructor and
     * have it produce an identical HeapPage object.
     *
     * @return A byte array correspond to the bytes of this page.
     */
	public byte[] getPageData() {
		int len = HeapFile.PAGE_SIZE;
		ByteArrayOutputStream baos = new ByteArrayOutputStream(len);
		DataOutputStream dos = new DataOutputStream(baos);

		// create the header of the page
		for (int i=0; i<header.length; i++) {
			try {
				dos.writeByte(header[i]);
			} catch (IOException e) {
				// this really shouldn't happen
				e.printStackTrace();
			}
		}

		// create the tuples
		for (int i=0; i<tuples.length; i++) {

			// empty slot
			if (!slotOccupied(i)) {
				for (int j=0; j<td.getSize(); j++) {
					try {
						dos.writeByte(0);
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
				continue;
			}

			// non-empty slot
			for (int j=0; j<td.numFields(); j++) {
				Field f = tuples[i].getField(j);
				try {
					dos.write(f.toByteArray());

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// padding
		int zerolen = HeapFile.PAGE_SIZE - (header.length + td.getSize() * tuples.length); //- numSlots * td.getSize();
		byte[] zeroes = new byte[zerolen];
		try {
			dos.write(zeroes, 0, zerolen);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return baos.toByteArray();
	}

	/**
	 * Returns an iterator that can be used to access all tuples on this page. 
	 * @return
	 */
	public Iterator<Tuple> iterator() {
		//HW1
		//utilize the java inner-built iterator in ArrayList
		ArrayList<Tuple> arrayLi = new ArrayList<>();
		int i = 0;
		while(i < tuples.length) {
			if(slotOccupied(i)) {
				arrayLi.add(tuples[i]);
			}
			i++;
		}
		return arrayLi.iterator();	    
	}
	
	// ***** HW4 ***** //
	// Check is this heappage is dirty or not
	public boolean isDirty() {
		return this.isDirty;
	}
	
	// set a page to be dirty or not in a transaction
	public void setDirty(int tid, boolean isDirty) {
		this.tid = tid;
		this.isDirty = isDirty;
	}
	
	// return the tranctionID of a page, if not in a transaction return -1
	public int getTranctionId() {
		return this.tid;
	}
	
	// check if this page has empty slot for insertion
	public boolean hasFreeSlot() {
		int i = 0;
		while (i < this.getNumSlots()) {
			if (!slotOccupied(i)) {
				return true;
			}
			i++;
		}
		return false;
	}
}
