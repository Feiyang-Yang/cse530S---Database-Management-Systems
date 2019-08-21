package hw3;

import java.util.ArrayList;
import java.util.List;

import hw1.Field;
import hw1.RelationalOperator;

public class InnerNode implements Node {
	
	ArrayList<Field> keys; 			//Keys in this InnerNode, key can be String or Int
	ArrayList<Node> children;		//Children of this InnerNode
	private int degree;				//Degree is the number of keys in this innerNode
	
	//InnerNode Parent
	
	public InnerNode(int degree) {
		//HW3
		this.degree = degree - 1;
		keys = new ArrayList<>(); 		// the number of keys = pInner - 1
		children = new ArrayList<>(); 	// the number of children = pInner
	}
	
	public ArrayList<Field> getKeys() {
		//HW3
		return keys;
	}
	
	public ArrayList<Node> getChildren() {
		//HW3
		return children;
	}

	// Degree is the number of keys
	public int getDegree() {
		//HW3
		return degree;
	}
	
	public boolean isLeafNode() {
		return false;
	}
	
	//***** More Helper Functions *****//
	public Capacity checkCapacity() {
		int halfSize = (int)Math.ceil(degree/2.0);
		int currentSize = keys.size();
		
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
	
	// Add the node as one of the innnerNode's child
	public void addChild(Node node) {
		// Get the search key for this node
		Field key = node.getSearchKey();
		
		// Iterate the keys 
		for(int i = 0; i < children.size(); i++) {
			if(children.get(i).getSearchKey().compare(RelationalOperator.GTE, key)) {
				// Find the right place to put this new node
				children.add(i, node);
				
				// Update the keys of parent node
				refreshKeys();
				return;
			}
		}
		
		// Add new node to the last
		children.add(node);
		refreshKeys();
	}
	
	// Remove a child
	public void removeChild(Node node) {
		children.remove(children.indexOf(node));
		this.refreshKeys();
	}
	
	// Find child by Key
	public Node findChildByKey(Field key) {
		// iterate the its keys
		for(int i = 0; i < keys.size(); i++) {
			if(keys.get(i).compare(RelationalOperator.GTE, key)) {
				return children.get(i);
			}
		}
		
		//Else, return the last child
		return children.get(keys.size());
	}
	
	public Node getLastChild() {
		return children.get(children.size() - 1);
	}
	
	public Node getFirstChild() {
		return children.get(0);
	}
	
	// get sibling of one child
	public Node getRightSibling(Node child) {
		// get this child's index
		int index = children.indexOf(child);
		
		// check if it has right sibling
		if(index + 1 < children.size()) {
			return children.get(index + 1);
		} else {
			return null;
		}
	}

	public Node getLeftSibling(Node child) {
		// get this child's index
		int index = children.indexOf(child);
		
		// check if it has left sibling
		if (index == 0) {
			return null;
		} else {
			return children.get(index - 1);
		}
	}
	
	// Get searchKey
	public Field getSearchKey() {
		//Recursively get the rightmost leafNode's searchKey
		return children.get(children.size() - 1).getSearchKey();
	}
	
	// Update keys in this InnerNode based on its children
	public void refreshKeys() {
		// create a new ArrayList to store updated keys
		ArrayList<Field> updatedKeys = new ArrayList<>();
		
		// Iterate its children and get their search keys and put into updatedKeys
		for(int i = 0; i < children.size() - 1; i++) {
			//NOTE: children.size() - 1 is because that we don't need to store the search key for rightmost child
//			if(i < keys.size()) {
//				if(children.get(i).getSearchKey().compare(RelationalOperator.GT, this.keys.get(i))) {
//					updatedKeys.add(children.get(i).getSearchKey());
//				}else {
//					updatedKeys.add(keys.get(i));
//				}
//			}else {
//				updatedKeys.add(children.get(i).getSearchKey());
//			}
			updatedKeys.add(children.get(i).getSearchKey());
		}
		
		// Set updated keys as the new keys
		keys = updatedKeys;
	}
	
	// Update children based on children list
	public void updateChildren(ArrayList<Node> children) {
		this.children = children;
		
		//After update children, do not forget to update keys based on its children
		refreshKeys();
	}
	
}