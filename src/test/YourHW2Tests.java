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
import hw1.IntField;
import hw1.TupleDesc;
import hw2.Query;
import hw2.Relation;

public class YourHW2Tests {

	private HeapFile testhf;
	private TupleDesc testtd;
	private HeapFile ahf;
	private TupleDesc atd;
	private Catalog c;

	@Before
	public void setup() {
		
		try {
			Files.copy(new File("testfiles/test.dat.bak").toPath(), new File("testfiles/test.dat").toPath(), StandardCopyOption.REPLACE_EXISTING);
			Files.copy(new File("testfiles/A.dat.bak").toPath(), new File("testfiles/A.dat").toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.out.println("unable to copy files");
			e.printStackTrace();
		}
		
		//Create heapfile and table for test.txt, table name is test;
		c = Database.getCatalog();
		c.loadSchema("testfiles/test.txt");
		
		int tableId = c.getTableId("test");
		testtd = c.getTupleDesc(tableId); //Hashmap: <tableId, table>; table has Headfile and there is TupleDesc inside
		testhf = c.getDbFile(tableId);
		
		//Create heapfile and table for A.txt, table name is A;
		c = Database.getCatalog();
		c.loadSchema("testfiles/A.txt");
		
		tableId = c.getTableId("A");
		atd = c.getTupleDesc(tableId);
		ahf = c.getDbFile(tableId);
	}
	
	/*
	 * Test MAX function in aggregator
	 * */
	@Test
	public void testAggregate_MAX() {
		Query q = new Query("SELECT MAX(a2) FROM A");
		Relation r = q.execute();
		
		assertTrue(r.getTuples().size() == 1);
		IntField agg = (IntField)r.getTuples().get(0).getField(0);
		assertTrue(agg.getValue() == 8);
	}
	
	/*
	 * Test AS function
	 * */
	@Test
	public void testAS() {
		Query q = new Query("SELECT a2 AS newName FROM A");
		Relation r = q.execute();
		
		assertTrue(r.getDesc().getSize() == 1);
		assertTrue(r.getTuples().size() == 8);
		assertTrue(r.getDesc().getFieldName(0).equals("newName"));
	}
	

}
