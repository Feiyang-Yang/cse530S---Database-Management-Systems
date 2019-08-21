package hw3;

import hw1.Field;

public interface Node {

	public int getDegree();
	public boolean isLeafNode();
	
	//Common function to get the searchkey for current node
	public Field getSearchKey();
	public Capacity checkCapacity();
	
}
