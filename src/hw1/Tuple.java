/*
 * Team members:
 * 1. Feiyang Yang, 458715
 * 2. Hao Sun, 458716
 * */

package hw1;

import java.sql.Types;
import java.util.HashMap;

/**
 * This class represents a tuple that will contain a single row's worth of information
 * from a table. It also includes information about where it is stored
 * @author Sam Madden modified by Doug Shook
 *
 */
public class Tuple {
	
	/**
	 * Creates a new tuple with the given description
	 * @param t the schema for this tuple
	 */
	
	TupleDesc tupleDesc;
	int pId; 											//Page Id
	int slotId; 										//Slot Id
	HashMap<String, Field> fields = new HashMap<>(); 	//Data fields <Name, data>, O(1) time complexity
	
	public Tuple(TupleDesc t) {
		//HW1
		this.tupleDesc = t;
	}
	
	public TupleDesc getDesc() {
		//HW1
		return this.tupleDesc;
	}
	
	/**
	 * retrieves the page id where this tuple is stored
	 * @return the page id of this tuple
	 */
	public int getPid() {
		//HW1
		return this.pId;
	}

	public void setPid(int pid) {
		//HW1
		this.pId = pid;
	}

	/**
	 * retrieves the tuple (slot) id of this tuple
	 * @return the slot where this tuple is stored
	 */
	public int getId() {
		//HW1
		return this.slotId;
	}

	public void setId(int id) {
		//HW1
		this.slotId = id;
	}
	
	public void setDesc(TupleDesc td) {
		//HW1
		this.tupleDesc = td;
	}
	
	/**
	 * Stores the given data at the i-th field
	 * @param i the field number to store the data
	 * @param v the data
	 */
	public void setField(int i, Field v) {
		//HW1
		String fieldName = this.tupleDesc.getFieldName(i);
		this.fields.put(fieldName, v);
	}
	
	public Field getField(int i) {
		//HW1
		String fieldName = this.tupleDesc.getFieldName(i);
		return this.fields.get(fieldName);
	}
	
	/**
	 * Creates a string representation of this tuple that displays its contents.
	 * You should convert the binary data into a readable format (i.e. display the ints in base-10 and convert
	 * the String columns to readable text).
	 */
	public String toString() {
		//HW1
		StringBuilder sb = new StringBuilder();

		for(int i = 0; i < this.fields.size(); i++) {
			Field curField = this.fields.get(this.tupleDesc.getFieldName(i));
			if(this.tupleDesc.getType(i) == Type.STRING) {
				if (curField instanceof StringField) {
					StringField stringF = (StringField)curField;
					sb.append(stringF.toString());
				} else {
					try {
						throw new Exception("Actual type is not STRING");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}else if(this.tupleDesc.getType(i) == Type.INT) {
				if(curField instanceof IntField) {
					IntField intF = (IntField)curField;
					sb.append(intF.toString());
				} else {
					try {
						throw new Exception("Actual type is not INT");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} 

			} else {
				try {
					throw new Exception("Has to be INT or STRING");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return sb.toString();
	}
	
}
	