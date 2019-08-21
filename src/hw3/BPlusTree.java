package hw3;


import java.util.ArrayDeque;
import java.util.ArrayList;

import hw1.Field;

public class BPlusTree {
    
	Node root;
	int pInner;
	int pLeaf;
	// Determine if the innernode needs refresh its keys
	static boolean mergeOrBorrow = false;
	
	//pInner, pLeaf are degrees
    public BPlusTree(int pInner, int pLeaf) {
    	//HW3
    	this.pInner = pInner;
    	this.pLeaf = pLeaf;
    	root = new LeafNode(pLeaf);
    }
    
    //***** Search *****//
    
    // Find the LeafNode that contains Field f, else, return null
    public LeafNode search(Field f) {
    	//HW3
    	Node curNode = root;
    	
    	// Go down to the LeafNode
    	while(!curNode.isLeafNode()) {
    		curNode = ((InnerNode)curNode).findChildByKey(f);
    	}
    	
    	// Find the leafNode and check whether it contains the Field f
    	LeafNode leafNode = (LeafNode)curNode;
    	if(leafNode.containField(f)) {
    		return leafNode;
    	}
    	
    	return null;
    }
    
    //***** Insert *****//
    
    // Insert a new Entry e into the B+Tree, this is the main function, we also need a recursive function for helping
    public void insert(Entry e) {
    	//HW3
    	insert_rec(root, e);
    	
    	// Don't forget to handle the split in root
    	if(root.checkCapacity() == Capacity.OVERSIZE) 
    		handleSplit(root, null); // current node is root and parent is null
    }
    
    // Insert a new Entry e into node 
    private void insert_rec(Node node, Entry e) {
    	//Base Case: node is LeafNode 
    	if(node.isLeafNode()) {
    		((LeafNode)node).addEntry(e);
    	}else {
    		// Find the node insert to
    		InnerNode node_inner = (InnerNode)node;
    		Node childNode = node_inner.findChildByKey(e.getField());
    		
    		// Recursively insert 
    		insert_rec(childNode, e);
    		
    		// Handle split; node_inner is the parent of childNode
    		if(childNode.checkCapacity() == Capacity.OVERSIZE) 
    			handleSplit(childNode, node_inner);
    		
    		// Don't forget to refresh keys in parent
    		node_inner.refreshKeys();
    	}
    }
    
    //Handle the split condition of the B+Tree
    private void handleSplit(Node current, InnerNode parent) {
    	// Oversize, we need do split
    	// Create a new Node pointer
    	Node new_node = null;
    		
    	// 1. Check if the current Node is LeafNode or InnnerNode and split
    	if(!current.isLeafNode()) { // Current node is InnerNode
    		// Create a new inner node
    		InnerNode new_inner = new InnerNode(this.pInner);
    		
    		// Split current node, add a new inner node
    		innerSplit((InnerNode)current, new_inner);
    		
    		new_node = new_inner;
    	}else {
    		// Create a new leaf node
    		LeafNode new_leaf = new LeafNode(this.pLeaf);
    		
    		// Split current node, add a new inner node
    		leafSplit((LeafNode)current, new_leaf);
    			
    		new_node = new_leaf;
    	}
    		
    	// 2. Handle the parent node pointers, including root condition or not
    	if(parent == null) { 
    		// create new root
    		InnerNode new_root = new InnerNode(this.pInner);
    		new_root.addChild(current);
    		new_root.addChild(new_node);
    		root = new_root;
    	}else {
    		// add new node to the children of parent
    		parent.addChild(new_node);
    	}
    }
    
    // Handle Leaf Node Split
    private void leafSplit(LeafNode current, LeafNode new_leaf) {	
		// Get all the children of current node, prepare for spliting
		ArrayList<Entry> entries = current.getEntries();
			
		int mid = (int)Math.ceil(entries.size()/2.0);
		// Create two new children list
		ArrayList<Entry> leftEntries = new ArrayList<>(entries.subList(0, mid));
		ArrayList<Entry> rightEntries = new ArrayList<>(entries.subList(mid, entries.size()));
		// Set two new children Lists as the children of those two node
		current.updateEntries(leftEntries);
		new_leaf.updateEntries(rightEntries);
    }
    
    // Handle Handle Inner Split
    private void innerSplit(InnerNode current, InnerNode new_inner) {
		// Get all the children of current node, prepare for spliting
		ArrayList<Node> children = current.getChildren();
		int mid = (int)Math.ceil(children.size()/2.0);

		// Create two new children list
		ArrayList<Node> leftChildren = new ArrayList<>(children.subList(0, mid));
		ArrayList<Node> rightChildren = new ArrayList<>(children.subList(mid, children.size()));
			
		// Set two new children Lists as the children of those two node
		current.updateChildren(leftChildren);
		new_inner.updateChildren(rightChildren);
    }
    
    public Node getRoot() {
    	//HW3
    	return this.root;
    }
    
    //***** Delete *****//
    
    public void delete(Entry e) {
    	//HW3
    	// search for the LeafNode we will operate on
    	LeafNode searchNode = this.search(e.getField());
    	if (searchNode == null) {
    		// if the LeafNode returned is null, the entry is not in the tree, do nothing 
    		return;
    	} else { 
    		// if not null, the entry is in the tree, delete it
    		ArrayDeque<Node> dequeNodes = new ArrayDeque<Node>();
    		dequeNodes.push(root);
    		
    		// Delete recursively
    		delete_rec(dequeNodes, e);
    		
    		// Refresh keys of InnerNodes based on its children, start from root layer, to every InnderNode
    		//refreshkeys_rec(root);
    	}
    }
    
    // Update keys in this InnerNode based on its children
    // start from root layer, to every InnderNode
	private void refreshkeys_rec(Node curNode) {
	 	//HW3
		// only need to consider InnerNode for recursion
		if(!curNode.isLeafNode()) {
			ArrayList<Node> children =  ((InnerNode) curNode).getChildren();
			
			// Recursively refresh all the keys
			for(int i = 0; i < children.size(); i++) {
				refreshkeys_rec(children.get(i));
			}
			((InnerNode) curNode).refreshKeys();
		}
	}
    
    // for deleting node, go layer by layer through recursion
 	private void delete_rec(ArrayDeque<Node> dequeNodes, Entry e) { // use Deque for handling coming last, deleting first
 		Node curNode = dequeNodes.peek();
 		// base case: the entry in leafNode, just delete it
 		if (curNode.isLeafNode()) {
 			((LeafNode) curNode).delEntry(e);
 		}
 		// when the entry in innerNode, record which childNode is chosen in this layer and go to the next layer
 		else {
 			Node child = ((InnerNode) curNode).findChildByKey(e.getField());
 			dequeNodes.push(child);
 			delete_rec(dequeNodes, e);
 		} 
 		
 		//dequeNodes contains all the inner nodes level by level, handle the deletion condition of the B+Tree
 		handleMergeOrBorrow(dequeNodes);
 		
 		// Don't forget to poll the last one that has been handled merge
 		dequeNodes.pop();
 		
 		// refresh its parent if merge or borrow happens
 		if(mergeOrBorrow) {
 			if(dequeNodes.size() > 0) {
 	 			Node parent = dequeNodes.peek();
 	 			((InnerNode)parent).refreshKeys();
 	 		}
 		}
 	}
    
	//Handle the Merge or Borrow condition of the B+Tree
	private void handleMergeOrBorrow(ArrayDeque<Node> dequeNodes) {
		Node curNode = dequeNodes.peek();
		
		// Case 1. Only handle root node
		if(root == curNode) {
			// Case 1.1 if it is leaf node, do nothing
			if(curNode.isLeafNode()) {
				return;
			// Case 1.2 if it is not leaf
			}else {
				if(((InnerNode) root).getChildren().size() < 2) {
					root = ( (InnerNode) root).getChildren().get(0);
				}
				return;
			}
		}
		
		if(curNode.checkCapacity() == Capacity.UNDER_HALF) {
			mergeOrBorrow = true;
			dequeNodes.pop();
			// get the parent node of the current node, remember to restore the tree
			InnerNode parNode = (InnerNode) dequeNodes.peek();
			// Keep the original ArrayDeque
			dequeNodes.push(curNode);
			
			//Find left and right sibling first
			Node sibNode = this.getLeftSibling(dequeNodes);
			if(sibNode == null) {
				sibNode = this.getRightSibling(dequeNodes);
			}
			
			// Case 2: InnerNode
			if(!sibNode.isLeafNode()) { // InnerNode
				// Case 2.1: if sibling is just as half full, merge nodes
				// (it will not appear where sibling's less than half full because we keep the tree structure)
				if (sibNode.checkCapacity() == Capacity.HALF) {
					ArrayList<Node> mergeNodes = ((InnerNode) curNode).getChildren();
					for (int i = 0; i < mergeNodes.size(); i++) {
						Node mergeNode = mergeNodes.get(i);
						((InnerNode) sibNode).addChild(mergeNode);
					}
					parNode.removeChild(curNode); // remove curNode from its Parent
				}
				// Case 2.2: if sibling node is more than half full, borrow node from sibling
				else {
					ArrayList<Node> sib_children = ((InnerNode) sibNode).getChildren();
					Node burrowNode = sib_children.get(sib_children.size() - 1);
					((InnerNode) sibNode).removeChild(burrowNode);
					((InnerNode) curNode).addChild(burrowNode);
				}
			}
			// Case 3: LeafNode
			else {
				// Case 3.1: if sibling is just as half full, merge nodes
				if (sibNode.checkCapacity() == Capacity.HALF) {
					ArrayList<Entry> mergeEntries = ((LeafNode) curNode).getEntries();
					for (int i = 0; i < mergeEntries.size(); i++) {
						Entry mergeEntry = mergeEntries.get(i);
						((LeafNode) sibNode).addEntry(mergeEntry);
					}
					parNode.removeChild(curNode);
				} 
				// Case 3.2: if sibling node is more than half full, borrow node from sibling
				else {
					ArrayList<Entry> sib_Entries = ((LeafNode) sibNode).getEntries();
					Entry burrowEntry = sib_Entries.get(sib_Entries.size() - 1);
					((LeafNode) sibNode).delEntry(burrowEntry);
					((LeafNode) curNode).addEntry(burrowEntry);
				}
			}
		}else {
			mergeOrBorrow = false;
		}
	}

	private Node getLeftSibling(ArrayDeque<Node> dequeNodes) {
		ArrayDeque<Node> deque = dequeNodes.clone();
		Node child = null;
		InnerNode parent = null;
		Node sib_temp = null;
		Node sib = null;
		
		int steps = 0; // represent the steps go up from the curNode
		while(deque.size() >= 2) {
			child = deque.pop();
			parent = (InnerNode)deque.peek();
			sib = parent.getLeftSibling(child);
			if(sib == null) {
				steps++; // Keep going up to find the sibling of current node
			}else {
				sib_temp = sib; 
				/* Find a left sibling, but may in the different level with current node,
				 * Need go down rightmost to the same level of current node, that will be the left sibling
				 * */
				break;
			}
		}
		
		if(sib_temp == null) { // Cannot find the left sibling
			return null;
		}else {
			sib = sib_temp;
			for(int i = 1; i <= steps; i++) {
				sib = ((InnerNode)sib).getLastChild();
			}
		}
		return sib;
	}
	
	private Node getRightSibling(ArrayDeque<Node> dequeNodes) {
		ArrayDeque<Node> deque = dequeNodes.clone();
		Node child = null;
		InnerNode parent = null;
		Node sib_temp = null;
		Node sib = null;
		
		int steps = 0; // represent the steps go up from the curNode
		while(deque.size() >= 2) {
			child = deque.pop();
			parent = (InnerNode)deque.peek();
			sib = parent.getRightSibling(child);
			if(sib == null) {
				steps++; // Keep going up to find the sibling of current node
			}else {
				sib_temp = sib; 
				/* Find a right sibling, but may in the different level with current node,
				 * Need go down leftmost to the same level of current node, that will be the right sibling
				 * */
				break;
			}
		}
		
		if(sib_temp == null) { // Cannot find the left sibling
			return null;
		}else {
			sib = sib_temp;
			for(int i = 1; i <= steps; i++) {
				sib = ((InnerNode)sib).getFirstChild();
			}
		}
		return sib;
	}
}
