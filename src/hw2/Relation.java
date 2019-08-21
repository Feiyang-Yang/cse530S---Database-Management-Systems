package hw2;

import java.util.ArrayList;

import hw1.Field;
import hw1.IntField;
import hw1.RelationalOperator;
import hw1.StringField;
import hw1.Tuple;
import hw1.TupleDesc;
import hw1.Type;

/**
 * This class provides methods to perform relational algebra operations. It will
 * be used to implement SQL queries.
 * 
 * @author Doug Shook
 *
 */
public class Relation {

	private ArrayList<Tuple> tuples;
	private TupleDesc td;

	public Relation(ArrayList<Tuple> l, TupleDesc td) {
		// HW2
		this.tuples = l;
		this.td = td;
	}

	/**
	 * This method performs a select operation on a relation
	 * 
	 * @param field   number (refer to TupleDesc) of the field to be compared, left
	 *                side of comparison
	 * @param op      the comparison operator
	 * @param operand a constant to be compared against the given column
	 * @return
	 */
	public Relation select(int field, RelationalOperator op, Field operand) {
		// HW2
		// corner case
		if (field < 0 || field >= td.numFields() || op == null || operand == null) {
			return null;
		}

		ArrayList<Tuple> result = new ArrayList<Tuple>();

		/*
		 * Optional operation:
		 * 
		 * for(Tuple curTuple: tuples){ Tuple curTuple = tuples.get(i);
		 * if(curTuple.getField(field).compare(op, operand)){ result.add(curTuple); } }
		 * 
		 */

		// loop through the tuples of this Relation to find the ones we want
		int i = 0;
		while (i < this.tuples.size()) {
			Tuple curTuple = tuples.get(i);
			Field curData = curTuple.getField(field);
			Field gotten = null;
			// wrap data into the type for compare
			if (this.td.getType(field).equals(Type.STRING)) {
				gotten = (StringField) curData;
			} else {
				gotten = (IntField) curData;
			}
			// Field - public boolean compare(RelationalOperator op, Field value);
			if (gotten.compare(op, operand)) {
				result.add(curTuple);
			}
			i++;
		}
		Relation newRela = new Relation(result, td);
		return newRela;
	}

	/**
	 * This method performs a rename operation on a relation
	 * 
	 * @param fields the field numbers (refer to TupleDesc) of the fields to be
	 *               renamed
	 * @param names  a list of new names. The order of these names is the same as
	 *               the order of field numbers in the field list
	 * @return
	 */
	public Relation rename(ArrayList<Integer> fields, ArrayList<String> names) {
		// HW2
		// corner case
		if (fields == null || names == null) {
			return null;
		}

		// get the original names before rename
		int origLength = this.td.numFields();
		String[] origNames = td.getFieldNames();

		// do the rename
		int j = 0;
		while (j < fields.size()) {
			Integer num = fields.get(j);
			origNames[num] = names.get(j);
			j++;
		}

		// updating TupleDesc
		Type[] typesTd = td.getTypes();
		TupleDesc newTd = new TupleDesc(typesTd, origNames);

		// update TupleDesc in tuples
		ArrayList<Tuple> tuples_newTd = tuples;
		for (Tuple curTuple : tuples_newTd) {
			curTuple.setDesc(newTd);
		}
		Relation newRela = new Relation(tuples_newTd, newTd);
		return newRela;
	}

	/**
	 * This method performs a project operation on a relation
	 * 
	 * @param fields a list of field numbers (refer to TupleDesc) that should be in
	 *               the result
	 * @return
	 */
	public Relation project(ArrayList<Integer> fields) {
		// HW2
		// corner case
		if (fields == null) {
			return null;
		}

		int newLength = fields.size();
		Type[] typeProj = new Type[newLength];
		String[] tupleproj = new String[newLength];
		// extract the types and fields and create a new TupleDesc
		int i = 0;
		while (i < newLength) {
			Integer number = fields.get(i);
			typeProj[i] = this.td.getType(number);
			tupleproj[i] = td.getFieldName(number);
			i++;
		}
		TupleDesc tdProj = new TupleDesc(typeProj, tupleproj);

		// Create a new tuple list based on the new TupleDesc
		ArrayList<Tuple> tuplesPoj = new ArrayList<Tuple>();
		int j = 0;
		while (j < this.tuples.size()) { // project fields in each tuple
			Tuple origiTup = tuples.get(j);
			Tuple eachTup = new Tuple(tdProj); // Create

			// get fields for eachTup
			int z = 0;
			while (z < newLength) {
				Integer num2 = fields.get(z);
				Field f = origiTup.getField(num2);
				eachTup.setField(z, f);
				z++;
			}
			// add the tuple into new tuple list
			tuplesPoj.add(eachTup);
			j++;
		}

		return new Relation(tuplesPoj, tdProj);
	}

	/**
	 * This method performs a join between this relation and a second relation. The
	 * resulting relation will contain all of the columns from both of the given
	 * relations, joined using the equality operator (=)
	 * 
	 * @param other  the relation to be joined
	 * @param field1 the field number (refer to TupleDesc) from this relation to be
	 *               used in the join condition
	 * @param field2 the field number (refer to TupleDesc) from other to be used in
	 *               the join condition
	 * @return
	 */
	private String[] getAllFields(TupleDesc td) {
		int numFiel = td.numFields();
		String[] all = new String[numFiel];
		int i = 0;
		while (i < numFiel) {
			// i++;
			all[i] = td.getFieldName(i);
			i++;
		}
		return all;
	}

	public Relation join(Relation other, int field1, int field2) {
		// your code here
		Type t1 = this.getDesc().getType(field1);
		Type t2 = other.getDesc().getType(field2);
		// corner case
		if (t1.equals(t2) == false || other == null || field1 < 0 || field2 < 0) {
			Relation exceptRel = new Relation(null, this.td);
			return exceptRel;
		}

		// prepare for the generation of the new Relation
		String[] fieldsFir = getAllFields(this.getDesc());
		String[] fieldsSec = getAllFields(other.getDesc());
		String[] jointFields = new String[fieldsFir.length + fieldsSec.length];
		System.arraycopy(fieldsFir, 0, jointFields, 0, fieldsFir.length);
		System.arraycopy(fieldsSec, 0, jointFields, fieldsFir.length, fieldsSec.length);

		Type[] typesFir = this.td.getTypes();
		Type[] typesSec = other.td.getTypes();
		Type[] jointTypes = new Type[typesFir.length + typesSec.length];
		System.arraycopy(typesFir, 0, jointTypes, 0, typesFir.length);
		System.arraycopy(typesSec, 0, jointTypes, typesFir.length, typesSec.length);

		TupleDesc jointTD = new TupleDesc(jointTypes, jointFields);
		ArrayList<Tuple> jointTuples = new ArrayList<>();

		// compare the specific field of all the rows in one table to another
		for (Tuple one : this.tuples) {
			Type rowType = one.getDesc().getType(field1);
			for (Tuple two : other.tuples) {
				Field oneField = null;
				Field twoField = null;
				byte[] oneByte = one.getField(field1).toByteArray();
				byte[] twoByte = two.getField(field2).toByteArray();

				if (rowType.equals(Type.STRING)) {
					oneField = new StringField(oneByte);
					twoField = new StringField(twoByte);
				} else {
					oneField = new IntField(oneByte);
					twoField = new IntField(twoByte);

				}

				// generate new Tuple only when the two tuples in this round have same value
				if (oneField.equals(twoField)) {
					Tuple jointNewTup = new Tuple(jointTD);
					for (int i = 0; i < fieldsFir.length; i++) {
						jointNewTup.setField(i, one.getField(i));
					}

					for (int j = 0; j < fieldsSec.length; j++) {
						jointNewTup.setField(j + fieldsFir.length, two.getField(j));
					}
					jointTuples.add(jointNewTup);
				}
			}
		}
		Relation jointRel = new Relation(jointTuples, jointTD);
		return jointRel;
	}

	/**
	 * Performs an aggregation operation on a relation. See the lab write up for
	 * details.
	 * 
	 * @param op      the aggregation operation to be performed
	 * @param groupBy whether or not a grouping should be performed
	 * @return
	 */
	public Relation aggregate(AggregateOperator op, boolean groupBy) {
		// HW2
		Aggregator aggregator = new Aggregator(op, groupBy, td);
		for (Tuple curTuple : tuples) {
			// Merge curTuple to relevant group:
			// 1. groupBy is true: two columns, multiple groups;
			// 2. groupBy is false: one columns, one group.
			aggregator.merge(curTuple);
		}
		return new Relation(aggregator.getResults(), td);
	}

	public TupleDesc getDesc() {
		// HW2
		return this.td;
	}

	public ArrayList<Tuple> getTuples() {
		// HW2
		return this.tuples;
	}

	/**
	 * Returns a string representation of this relation. The string representation
	 * should first contain the TupleDesc, followed by each of the tuples in this
	 * relation
	 */
	public String toString() {
		// HW2
		StringBuilder sb = new StringBuilder();
		sb.append(td.toString() + '\n');
		for (Tuple tuple : tuples)
			sb.append(tuple.toString() + '\n');
		return sb.toString();
	}
}
