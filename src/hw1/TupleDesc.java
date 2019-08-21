/*
 * Team members:
 * 1. Feiyang Yang, 458715
 * 2. Hao Sun, 458716
 * */

package hw1;
import java.util.*;

/**
 * TupleDesc describes the schema of a tuple.
 */
public class TupleDesc {

	private Type[] types;		//Types list of a Tuple
	private String[] fields;	//Filed name list of a Tuple
	
    /**
     * Create a new TupleDesc with typeAr.length fields with fields of the
     * specified types, with associated named fields.
     *
     * @param typeAr array specifying the number of and types of fields in
     *        this TupleDesc. It must contain at least one entry.
     * @param fieldAr array specifying the names of the fields. Note that names may be null.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr) {
    	//HW1
    	this.types = typeAr;
        this.fields = fieldAr;
    }

    /**
     * @return the number of fields in this TupleDesc
     */
    public int numFields() {
        //HW1
    	return this.fields.length;
    }

    /**
     * Gets the (possibly null) field name of the ith field of this TupleDesc.
     *
     * @param i index of the field name to return. It must be a valid index.
     * @return the name of the ith field
     * @throws NoSuchElementException if i is not a valid field reference.
     */
    public String getFieldName(int i) throws NoSuchElementException {
        //HW1
    	if(i >= fields.length || i < 0) {
			throw new NoSuchElementException();
		} else {
			return this.fields[i];
		}
    }
    
    //Customized functions for convenience
    public String[] getFieldNames() {
    	return fields;
    }
    
    public Type[] getTypes() {
    	return types;
    }

    /**
     * Find the index of the field with a given name.
     *
     * @param name name of the field.
     * @return the index of the field that is first to have the given name.
     * @throws NoSuchElementException if no field with a matching name is found.
     */
    public int nameToId(String name) throws NoSuchElementException {
        //HW1
		for(int i = 0 ; i < types.length; i++) {
			if(fields[i].equals(name)) return i;
		}
		throw new NoSuchElementException();
    }

    /**
     * Gets the type of the ith field of this TupleDesc.
     *
     * @param i The index of the field to get the type of. It must be a valid index.
     * @return the type of the ith field
     * @throws NoSuchElementException if i is not a valid field reference.
     */
    public Type getType(int i) throws NoSuchElementException {
        //HW1
     	if(i >= types.length || i < 0) {
			throw new NoSuchElementException();
		} else {
			return this.types[i];
		}
    }

    /**
     * @return The size (in bytes) of tuples corresponding to this TupleDesc.
     * Note that tuples from a given TupleDesc are of a fixed size.
     */
    public int getSize() {
    	//HW1
	    int size = 0;
	   	int count = 0;
	    while(count < types.length) { //Check each type of all data fields
	        if(this.types[count] == Type.STRING) {
	            size += 129; //128?
	        } else {
	            size += 4;
	        }
	        count++;
	    }
	    return size;
    }

    /**
     * Compares the specified object with this TupleDesc for equality.
     * Two TupleDescs are considered equal if they are the same size and if the
     * n-th type in this TupleDesc is equal to the n-th type in td.
     *
     * @param o the Object to be compared for equality with this TupleDesc.
     * @return true if the object is equal to this TupleDesc.
     */
    public boolean equals(Object o) {
    	//HW1
    	if (this == o) return true;
 		if (o == null) return false;
 		if (getClass() != o.getClass()) return false;

		TupleDesc tupo = (TupleDesc) o;
	    if(tupo.getSize() != this.getSize()) return false; 
	    
	    int i = 0;
	    while(i < this.types.length) {
	    	if(this.types[i] != tupo.types[i]) {
	           return false;
	        }
	    	i++;
	    }
	    return true;
    }
   
    public int hashCode() {
        // If you want to use TupleDesc as keys for HashMap, implement this so
        // that equal objects have equals hashCode() results
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Returns a String describing this descriptor. It should be of the form
     * "fieldType[0](fieldName[0]), ..., fieldType[M](fieldName[M])", although
     * the exact format does not matter.
     * @return String describing this descriptor.
     */
    public String toString() {
        //HW1
    	StringBuilder sb = new StringBuilder();
        int count = 0;
        while(count < types.length) {
            if(this.types[count] == Type.INT) {
            	sb.append("INT(" + fields[count] + ")");
            } else {
                sb.append("String(" + fields[count] + ")");
            }
            if(count != types.length-1) {
            	sb.append(", ");
            }
            count++;
        }
        return sb.toString();
    }
}
