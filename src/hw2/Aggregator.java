package hw2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import hw1.Field;
import hw1.IntField;
import hw1.StringField;
import hw1.Tuple;
import hw1.TupleDesc;

/**
 * A class to perform various aggregations, by accepting one tuple at a time
 * 
 * @author Doug Shook
 *
 */
public class Aggregator {
	AggregateOperator aggrOp;
	boolean groupBy;
	TupleDesc td;
	Field oneGroupName; // Used for the groupName when groupBy is false, only one group

	// <groupName, valueList> use field name as the group name, use arrayList to
	// store all the values in the group
	HashMap<Field, ArrayList<Field>> groups;

	public Aggregator(AggregateOperator o, boolean groupBy, TupleDesc td) {
		// HW2
		this.aggrOp = o;
		this.groupBy = groupBy;
		this.td = td;
		this.groups = new HashMap<>();
		oneGroupName = new IntField(1);
	}

	/**
	 * Merges the given tuple into the current aggregation
	 * 
	 * @param t the tuple to be aggregated
	 */
	public void merge(Tuple t) {
		// HW2
		Field groupName;
		Field value;
		
		if (this.groupBy) {
			// Two column, column 1 is groupName, column 2 is value
			groupName = t.getField(0);
			value = t.getField(1);
		} else {
			// One column, column 1 is the value, create
			groupName = oneGroupName;
			value = t.getField(0);
		}

		// Check if this group is exist, if not create a new group
		if (!groups.containsKey(groupName)) {
			groups.put(groupName, new ArrayList<>());
		}
		groups.get(groupName).add(value);
	}

	/**
	 * Returns the result of the aggregation
	 * 
	 * @return a list containing the tuples after aggregation
	 */
	public ArrayList<Tuple> getResults() {
		// HW2
		ArrayList<Tuple> resultTuples = new ArrayList<>();
		ArrayList<Field> values;

		// Iterate the group
		for (Field groupName : this.groups.keySet()) {
			// Within one group, calculate the aggregation:
			values = this.groups.get(groupName);
			
			// Check Field type
			if (values.get(0).getClass() == IntField.class) {
				// IntField, has MAX, MIN, SUM, COUNT, AVG operation
				// Create a temp value for aggregation
				Integer temp = (aggrOp == AggregateOperator.MAX || aggrOp == AggregateOperator.MIN) ? 
						((IntField) (values.get(0))).getValue() : 0;
				
				//Iterate the values and update temp based on operator
				for(Field curValue: values) {
					int value = ((IntField)curValue).getValue();
					switch(aggrOp) {
						case MAX:
							temp = value > temp? value : temp;
							break;
						case MIN:
							temp = value < temp? value : temp;
							break;
						case COUNT:
							temp++;
							break;
						case SUM:
						case AVG:
							temp += value;
							break;
					}
				}
				
				//if it is AVG, calculate
				if(aggrOp == AggregateOperator.AVG) {
					temp /= values.size();
				}
				
				// Aggregation is done for this group, create Tuple
				Tuple newTuple = new Tuple(td);
				if(groupBy) {
					newTuple.setField(0, groupName);
					newTuple.setField(1, new IntField(temp));
				}else {
					newTuple.setField(0, new IntField(temp));
				}
				resultTuples.add(newTuple);
			} else if (values.get(0).getClass() == StringField.class) {
				String temp = null;
				if(aggrOp == AggregateOperator.MAX || aggrOp == AggregateOperator.MIN) {
					temp = ((StringField) (values.get(0))).getValue();
				}
				int count = 0;
				
				//Iterate the values and update temp based on operator
				for(Field curValue: values) {
					String str = ((StringField)curValue).getValue();
					switch(aggrOp) {
						case MAX:
							temp = str.compareTo(temp) > 0? str : temp;
							break;
						case MIN:
							temp = str.compareTo(temp) < 0? str : temp;
							break;
						case COUNT:
							count++;
							break;
						case SUM:
						case AVG:
					}
				}
				
				Field newField;
				Tuple newTuple = new Tuple(td);
				if(aggrOp == AggregateOperator.COUNT) {
					newField = new IntField(count);
				}else {
					newField = new StringField(temp);
				}
				
				if(groupBy) {
					newTuple.setField(0, groupName);
					newTuple.setField(1, newField);
				}else {
					newTuple.setField(0, newField);
				}
				resultTuples.add(newTuple);
			} else {
				try {
					throw new Exception("Value column is not Int or String");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return resultTuples;
	}

}
