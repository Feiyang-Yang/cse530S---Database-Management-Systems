 package hw3;

import java.util.ArrayList;

import hw1.Field;
import hw1.RelationalOperator;

public class LeafNode implements Node {
	int degree;
	ArrayList<Entry> entries;
	
	//InnerNode Parent
	
	public LeafNode(int degree) {
		//HW3
		this.degree = degree;
		entries = new ArrayList<>();
	}
	
	public ArrayList<Entry> getEntries() {
		//HW3
		return entries;
	}

	public int getDegree() {
		//HW3
		return degree;
	}
	
	public boolean isLeafNode() {
		return true;
	}
	
	//***** More Helper Functions *****//
	
	// Return the Capacity Status, return type is Capacity ENUM
	public Capacity checkCapacity() {
		int halfSize = (int)Math.ceil(degree/2.0);
		int currentSize = entries.size();
		
		// Four different Capacity Status
		if(currentSize < halfSize) {
			return Capacity.UNDER_HALF;
		}else if(currentSize == halfSize) {
			return Capacity.HALF;
		}else if(currentSize <= degree) {
			return Capacity.ABOVE_HALF;
		}else {
			return Capacity.OVERSIZE;
		}
	}
	
	// Add new entry to entries
	public void addEntry(Entry entry_add) {
		//>>> 1. Iterate the entries, and find the right place to insert, keep sorted <<<//
		for(int i = 0; i < entries.size(); i++) {
			if(entries.get(i).getField().compare(RelationalOperator.EQ, entry_add.getField())) 
				return; //do nothing
			if(entries.get(i).getField().compare(RelationalOperator.GT, entry_add.getField())) {
				entries.add(i, entry_add);
				return;
			}
		}
		
		//>>> 2. This new entry is greater than each entry in entries, add to the last<<<//
		entries.add(entry_add);
	}
	
	// Delete one entry from entries
	public void delEntry(Entry entry_del) {
		// Iterate the entries to find the right one
		for(int i = 0; i < entries.size(); i++) {
			if(entries.get(i).getField().compare(RelationalOperator.EQ, entry_del.getField())) {
				entries.remove(i);
				return;
			}
		}
	}
	
	// Update entries, used in HandleSplit, set a new entries to a LeafNode
	public void updateEntries(ArrayList<Entry> entries) {
		this.entries = entries;
	}
	
	// Check is the field is in this leafNode
	public boolean containField(Field field) {
		// Iterate the entries to check the field
		for(int i = 0; i < entries.size(); i++) {
			if(entries.get(i).getField().compare(RelationalOperator.EQ, field)) {
				return true;
			}
		}
		return false;
	}
	
	// Get the corresponding search key should be in inner node direct to this leafNode
	// This will help inner node to update search keys in InnerNode Class
	public Field getSearchKey() {
		//System.out.println(entries.size());
		return entries.get(entries.size() - 1).getField();
	}

}