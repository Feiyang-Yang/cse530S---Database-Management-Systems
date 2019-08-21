/*
 * Team members:
 * 1. Feiyang Yang, 458715
 * 2. Hao Sun, 458716
 * */

package test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.junit.Before;
import org.junit.Test;

import hw1.Catalog;
import hw1.Database;
import hw1.HeapFile;
import hw1.HeapPage;
import hw1.IntField;
import hw1.StringField;
import hw1.Tuple;
import hw1.TupleDesc;
import hw1.Type;

public class YourUnitTests {
	
	private HeapFile hf;
	private TupleDesc td;
	private Catalog c;
	private HeapPage hp;

	@Before
	public void setup() {
		
		try {
			Files.copy(new File("testfiles/test.dat.bak").toPath(), new File("testfiles/test.dat").toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.out.println("unable to copy files");
			e.printStackTrace();
		}
		
		c = Database.getCatalog();
		c.loadSchema("testfiles/test.txt");
		
		int tableId = c.getTableId("test");
		td = c.getTupleDesc(tableId);
		hf = c.getDbFile(tableId);
		hp = hf.readPage(0);
	}
	
	@Test
	public void testGetNumSlots() {
		int tupleSize = td.getSize();
		int headerSize = (hf.PAGE_SIZE) / (1 + tupleSize);
		assertTrue(hp.getNumSlots() == headerSize);
	}
	
	
	/*
	 * Test add one more Tuple into a HeapFile with a full HeapPage
	 * */
	@Test
	public void testAddTupleToFullPage() {
		
		int numSlots = hp.getNumSlots();
		
		//Create numSlots new Tuples
		Tuple lastOne = new Tuple(td);
		Tuple newOne = new Tuple(td);
		for(int i = 0; i < numSlots + 1; i++) {
			newOne = new Tuple(td);
			do {
				byte[] f1 = new byte[] { (byte) (Math.random() * 256), (byte) (Math.random() * 256),
						(byte) (Math.random() * 256), (byte) (Math.random() * 256) };
				byte[] f2 = new byte[129];
				f2[0] = (byte) (Math.random() * 129);
				for (int j = 1; j < f2[0] + 1; j++) {
					f2[i] = (byte) (Math.random() * 256);
				}

				newOne.setField(0, new IntField(f1));
				newOne.setField(1, new StringField(f2));
			}while(newOne.equals(lastOne));
			
			lastOne = newOne;
			
			try {
				hf.addTuple(newOne);
			} catch (Exception e) {
				fail("error when adding valid tuple");
				e.printStackTrace();
			}
		}
		
		assertTrue(hf.getNumPages() == 2);
	}
	
	/*
	 * Test slotOccupied function after delete a tuple
	 * */
	@Test
	public void testSlotOccupiedAfterDelete() {
		
		Tuple newOne = new Tuple(td);
		byte[] f1 = new byte[] { (byte) (Math.random() * 256), (byte) (Math.random() * 256),
				(byte) (Math.random() * 256), (byte) (Math.random() * 256) };
		byte[] f2 = new byte[129];
		f2[0] = (byte) (Math.random() * 129);
		for (int i = 1; i < f2[0] + 1; i++) {
			f2[i] = (byte) (Math.random() * 256);
		}

		newOne.setField(0, new IntField(f1));
		newOne.setField(1, new StringField(f2));
		hf.addTuple(newOne);
		int pId = newOne.getPid(); //Page Id
		int slotId = newOne.getPid(); //Slot Id
		
		HeapPage currentHeapPage = hf.readPage(pId);
		assertTrue(currentHeapPage.slotOccupied(slotId));
		hf.deleteTuple(newOne);
		assertFalse(currentHeapPage.slotOccupied(slotId));
	}
	
	/*
	 * Test addTuple function by adding a Tuple with wrong structure
	 * */
	@Test(expected = Exception.class)
	public void testAddTupleWithWrongStructure() {
		Type[] t = new Type[] {Type.INT, Type.STRING, Type.INT};
		String[] c = new String[] {"id", "name", "age"};
		
		TupleDesc td = new TupleDesc(t, c);
		
		Tuple newOne = new Tuple(td);
		byte[] f1 = new byte[] { (byte) (Math.random() * 256), (byte) (Math.random() * 256),
				(byte) (Math.random() * 256), (byte) (Math.random() * 256) };
		byte[] f2 = new byte[129];
		f2[0] = (byte) (Math.random() * 129);
		for (int i = 1; i < f2[0] + 1; i++) {
			f2[i] = (byte) (Math.random() * 256);
		}
		
		byte[] f3 = new byte[] { (byte) (Math.random() * 256), (byte) (Math.random() * 256),
				(byte) (Math.random() * 256), (byte) (Math.random() * 256) };

		newOne.setField(0, new IntField(f1));
		newOne.setField(1, new StringField(f2));
		newOne.setField(2, new IntField(f3));
		hf.addTuple(newOne);
	}
}
