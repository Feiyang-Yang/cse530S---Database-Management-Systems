package test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import hw1.Field;
import hw1.IntField;
import hw1.RelationalOperator;
import hw3.BPlusTree;
import hw3.Entry;
import hw3.InnerNode;
import hw3.LeafNode;
import hw3.Node;

public class YourHW3Tests {

	
	// Different Degrees : 1. change inner degree; 2. change leaf degree; 3. change
	// both

	// Insertion: 1. Splitting Nodes; 2. Splitting Inner Node; 3. Create new root

	// Deletion: 1. Merge Nodes; 2. Borrowing value form sibling; 3. Push through;
	// 4. Deleting a level

	@Test
	public void testHigherDegrees() {
		BPlusTree bt = new BPlusTree(4, 2);
		bt.insert(new Entry(new IntField(12), 0));
		bt.insert(new Entry(new IntField(9), 0));
		bt.insert(new Entry(new IntField(10), 0));
		bt.insert(new Entry(new IntField(6), 0));
		bt.insert(new Entry(new IntField(4), 0));
		bt.insert(new Entry(new IntField(2), 0));
		bt.insert(new Entry(new IntField(13), 0));
		bt.insert(new Entry(new IntField(15), 0));
		bt.insert(new Entry(new IntField(5), 0));
		bt.insert(new Entry(new IntField(3), 0));

		// verify root properties
		Node root = bt.getRoot();

		assertTrue(root.isLeafNode() == false);

		InnerNode in = (InnerNode) root;

		ArrayList<Field> k = in.getKeys();
		ArrayList<Node> c = in.getChildren();

		assertTrue(k.get(0).compare(RelationalOperator.EQ, new IntField(6)));

		// grab left and right children from root
		InnerNode l = (InnerNode) c.get(0);
		InnerNode r = (InnerNode) c.get(1);

		assertTrue(l.isLeafNode() == false);
		assertTrue(r.isLeafNode() == false);

		// check values in left child of root
		ArrayList<Field> kl = l.getKeys();
		ArrayList<Node> cl = l.getChildren();

		assertTrue(kl.get(0).compare(RelationalOperator.EQ, new IntField(3)));
		assertTrue(kl.get(1).compare(RelationalOperator.EQ, new IntField(4)));

		// check values in right child of root
		ArrayList<Field> kr = r.getKeys();
		ArrayList<Node> cr = r.getChildren();

		assertTrue(kr.get(0).compare(RelationalOperator.EQ, new IntField(9)));
		assertTrue(kr.get(1).compare(RelationalOperator.EQ, new IntField(10)));
		assertTrue(kr.get(2).compare(RelationalOperator.EQ, new IntField(13)));

		// get left node's children, verify
		Node lc0 = cl.get(0);
		Node lc1 = cl.get(1);
		Node lc2 = cl.get(2);

		assertTrue(lc0.isLeafNode());
		assertTrue(lc1.isLeafNode());
		assertTrue(lc2.isLeafNode());

		LeafNode lc0l = (LeafNode) lc0;
		LeafNode lc1l = (LeafNode) lc1;
		LeafNode lc2l = (LeafNode) lc2;

		ArrayList<Entry> elc0 = lc0l.getEntries();

		assertTrue(elc0.get(0).getField().equals(new IntField(2)));
		assertTrue(elc0.get(1).getField().equals(new IntField(3)));

		ArrayList<Entry> elc1 = lc1l.getEntries();

		assertTrue(elc1.get(0).getField().equals(new IntField(4)));
		
		ArrayList<Entry> elc2 = lc2l.getEntries();

		assertTrue(elc2.get(0).getField().equals(new IntField(5)));
		assertTrue(elc2.get(1).getField().equals(new IntField(6)));
		
		// get right node's children, verify
		
		Node rc0 = cr.get(0);
		Node rc1 = cr.get(1);
		Node rc2 = cr.get(2);
		Node rc3 = cr.get(3);

		assertTrue(rc0.isLeafNode());
		assertTrue(rc1.isLeafNode());
		assertTrue(rc2.isLeafNode());
		assertTrue(rc3.isLeafNode());

		LeafNode rc0l = (LeafNode) rc0;
		LeafNode rc1l = (LeafNode) rc1;
		LeafNode rc2l = (LeafNode) rc2;
		LeafNode rc3l = (LeafNode) rc3;

		ArrayList<Entry> erc0 = rc0l.getEntries();

		assertTrue(erc0.get(0).getField().equals(new IntField(9)));

		ArrayList<Entry> erc1 = rc1l.getEntries();

		assertTrue(erc1.get(0).getField().equals(new IntField(10)));
		
		ArrayList<Entry> erc2 = rc2l.getEntries();

		assertTrue(erc2.get(0).getField().equals(new IntField(12)));
		assertTrue(erc2.get(1).getField().equals(new IntField(13)));
		
		ArrayList<Entry> erc3 = rc3l.getEntries();

		assertTrue(erc3.get(0).getField().equals(new IntField(15)));

	}

	// Test delete(parent borrows search key from other parent and update root)
	@Test
	public void testDelete() {
		// Create a tree, then delete some values

		BPlusTree bt = new BPlusTree(3, 2);
		bt.insert(new Entry(new IntField(10), 0));
		bt.insert(new Entry(new IntField(6), 0));
		bt.insert(new Entry(new IntField(14), 0));
		bt.insert(new Entry(new IntField(8), 0));
		bt.insert(new Entry(new IntField(2), 0));
		bt.insert(new Entry(new IntField(7), 0));
		bt.insert(new Entry(new IntField(1), 0));
		bt.insert(new Entry(new IntField(4), 0));
		bt.insert(new Entry(new IntField(11), 0));
		bt.insert(new Entry(new IntField(9), 0));

		// delete 11, 14, 10 check the snapshot of current tree
		bt.delete(new Entry(new IntField(11), 0));
		bt.delete(new Entry(new IntField(14), 0));
		bt.delete(new Entry(new IntField(10), 0));

		// Get Root and Verify
		Node root = bt.getRoot();

		assertTrue(root.isLeafNode() == false);

		InnerNode in = (InnerNode) root;

		ArrayList<Field> k = in.getKeys();
		ArrayList<Node> c = in.getChildren();

		// Originally the root is 8
		assertTrue(k.get(0).compare(RelationalOperator.EQ, new IntField(6)));

		// grab left and right children from root
		InnerNode l = (InnerNode) c.get(0);
		InnerNode r = (InnerNode) c.get(1);

		assertTrue(l.isLeafNode() == false);
		assertTrue(r.isLeafNode() == false);

		// check values in root left node
		ArrayList<Field> kl = l.getKeys();
		ArrayList<Node> cl = l.getChildren();

		assertTrue(kl.get(0).compare(RelationalOperator.EQ, new IntField(2)));

		// get left node's children, verify
		Node ll = cl.get(0);
		Node lr = cl.get(1);

		assertTrue(ll.isLeafNode());
		assertTrue(lr.isLeafNode());

		LeafNode lll = (LeafNode) ll;
		LeafNode lrl = (LeafNode) lr;

		ArrayList<Entry> ell = lll.getEntries();

		assertTrue(ell.get(0).getField().equals(new IntField(1)));
		assertTrue(ell.get(1).getField().equals(new IntField(2)));

		ArrayList<Entry> elr = lrl.getEntries();

		assertTrue(elr.get(0).getField().equals(new IntField(4)));
		assertTrue(elr.get(0).getField().equals(new IntField(6)));

		// verify right node
		ArrayList<Field> kr = r.getKeys();
		ArrayList<Node> cr = r.getChildren();

		assertTrue(kr.get(0).compare(RelationalOperator.EQ, new IntField(8)));

		// get right node's children, verify
		Node rl = cr.get(0);
		Node rr = cr.get(1);

		assertTrue(rl.isLeafNode());
		assertTrue(rr.isLeafNode());

		LeafNode rll = (LeafNode) rl;
		LeafNode rrl = (LeafNode) rr;

		ArrayList<Entry> erl = rll.getEntries();

		assertTrue(erl.get(0).getField().equals(new IntField(7)));
		assertTrue(erl.get(1).getField().equals(new IntField(8)));

		ArrayList<Entry> err = rrl.getEntries();

		assertTrue(err.get(0).getField().equals(new IntField(9)));
	}
}
