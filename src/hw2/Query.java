package hw2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import hw1.Catalog;
import hw1.Database;
import hw1.HeapFile;
import hw1.RelationalOperator;
import hw1.Field;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.*;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

public class Query {

	private String q;

	public Query(String q) {
		this.q = q;
	}

	// All the API can be found at the end of this function
	public Relation execute() {
		Statement statement = null;
		try {
			statement = CCJSqlParserUtil.parse(q);
		} catch (JSQLParserException e) {
			System.out.println("Unable to parse query");
			e.printStackTrace();
		}
		Select selectStatement = (Select) statement;
		PlainSelect sb = (PlainSelect) selectStatement.getSelectBody(); // select body

		// HW2
		// >>> 0. Setup, find the table <<<//
		Catalog catalog = Database.getCatalog();
		String originalTableName = ((Table) sb.getFromItem()).getName().toLowerCase();
		Relation relation_result = getRelationFromItem(sb.getFromItem(), catalog);
		/*
		 * The following operation Replaced by a function getRelationFromItem():
		 * 
		 * String tableName = ((Table)sb.getFromItem()).getName(); HeapFile heapfile =
		 * catalog.getDbFile(catalog.getTableId(tableName)); Relation relation_result =
		 * new Relation(heapfile.getAllTuples(), heapfile.getTupleDesc());
		 */

		// >>> 1. JOIN operation <<<//
		relation_result = joinOperation(catalog, sb, relation_result, originalTableName);

		// >>> 2. WHERE Operation <<<//
		relation_result = whereOperation(sb, relation_result);

		// >>> 3. SELECT, GROUP BY and AGGREGATE Operation <<<//
		relation_result = selectOperation(sb, relation_result);
		
		return relation_result;
	}

	/*
	 * Funtion to get relation from a table (fromItem)
	 * 
	 * item is the FromItem ---> table name catalog is used to find the table in
	 * heapfile
	 * 
	 * return a corresponding relation
	 */
	private Relation getRelationFromItem(FromItem item, Catalog catalog) {
		// >>> 0. get the table name from FromItem
		String tablename = ((Table) item).getName();

		// >>> 1. get the heapfile and create a relation
		HeapFile heapfile = catalog.getDbFile(catalog.getTableId(tablename));
		return new Relation(heapfile.getAllTuples(), heapfile.getTupleDesc());
	}

	/*
	 * Function to conduct JOIN operation
	 * 
	 * catalog is used to find table in heapfile sb is used to get join expression
	 * relation_result is the relation to be operated originalTableName is the
	 * tablename of that relation_result
	 * 
	 * return the Relation after join
	 */
	private Relation joinOperation(Catalog catalog, PlainSelect sb, Relation relation_result,
			String originalTableName) {
		List<Join> joinList = sb.getJoins(); // get the join list
		if (joinList != null) { // if joinList is not empty, operate join
			HashSet<String> tableNames = new HashSet<>(); // Store table names that already joined
			tableNames.add(originalTableName);
			
			for (Join join : joinList) {
				// >>> 1 Get relation in the right of JOIN <<<//
				Relation relation_toJoin = getRelationFromItem(join.getRightItem(), catalog);
				// >>> 2 Get the On Expression and get the fieldName that should be equal <<<//
				Expression onExpression = join.getOnExpression();
				String field1 = ((Column) ((BinaryExpression) onExpression).getLeftExpression()).getColumnName();
				String field2 = ((Column) ((BinaryExpression) onExpression).getRightExpression()).getColumnName();

				String table1_name = ((Column) (((BinaryExpression) onExpression).getLeftExpression())).getTable()
						.getName().toLowerCase();
				String table2_name = ((Column) (((BinaryExpression) onExpression).getRightExpression())).getTable()
						.getName().toLowerCase();

				// >>> 3 Call the join function in Relation class, need transfer fieldname to
				// fieldid <<<//
				// The order of field1 and field2 may messed up, so need to check whether this
				// field in this table, if not, swap
				int field_id1, field_id2;
				if (tableNames.contains(table1_name)) {
					field_id1 = relation_result.getDesc().nameToId(field1);
					field_id2 = relation_toJoin.getDesc().nameToId(field2);
					tableNames.add(table2_name);
				} else {
					field_id1 = relation_result.getDesc().nameToId(field2);
					field_id2 = relation_toJoin.getDesc().nameToId(field1);
					tableNames.add(table1_name);
				}
				
				relation_result = relation_result.join(relation_toJoin, field_id1, field_id2);
			}
		}

		return relation_result;
	}

	/*
	 * Function to conduct WHERE operation
	 * 
	 * catalog is used to find table in heapfile sb is used to get join expression
	 * relation_result is the relation to be operated originalTableName is the
	 * tablename of that relation_result
	 * 
	 * return the Relation after join
	 */
	private Relation whereOperation(PlainSelect sb, Relation relation_result) {
		Expression whereExp = sb.getWhere(); // Get Where Expression

		if (whereExp != null) { // Check if WhereExpression is null or not

			// >>> 0 Setup vistor <<<//
			WhereExpressionVisitor whereExpVisitor = new WhereExpressionVisitor();
			whereExp.accept(whereExpVisitor);

			// >>> 1 Get field id, operator and operand for select function in Relation
			// class <<<//
			String fieldName = whereExpVisitor.getLeft();
			int field = relation_result.getDesc().nameToId(fieldName);
			RelationalOperator op = whereExpVisitor.getOp();
			Field operand = whereExpVisitor.getRight();

			// >>> 2 Call select function in Relation class <<<//
			relation_result = relation_result.select(field, op, operand);
		}

		return relation_result;
	}
	
	private Relation selectOperation(PlainSelect sb, Relation relation_result) {
		//>>> 0. Get select items, initialize variables <<<//
		List<SelectItem> itemList = sb.getSelectItems();
		List<Expression> groupbyList = sb.getGroupByColumnReferences();
		ArrayList<Integer> fields = new ArrayList<>(); 	// store field ids
		ArrayList<Integer> renameFields = new ArrayList<>(); // store field ids that should be renamed
		ArrayList<String> renames = new ArrayList<>(); 	// store new names
		boolean isAggregate = false;
		AggregateOperator op = null;
		boolean isAllColumns = false;
		boolean isGroupBy = (groupbyList != null);
		
		//>>> 1. Iterate select items <<<//
		for(SelectItem selectItem: itemList) { //selectItem is a column
			ColumnVisitor columnVisitor = new ColumnVisitor();
			selectItem.accept(columnVisitor);
			
			//>>> 2. Get the selected column <<<//
			String columnName = columnVisitor.getColumn();
			
			//>>> 3. Check if it is '*' or a name
			if(columnName == "*") { //add all field ids and store into fields list
				isAllColumns = true;
				for(int i = 0; i < relation_result.getDesc().numFields(); i++) {
					fields.add(i);
				}
			}else {
				int fieldId = relation_result.getDesc().nameToId(columnName);
				fields.add(fieldId);
				
				// Record rename field and its new name
				Alias alias = ((SelectExpressionItem)selectItem).getAlias();
				if(alias != null && alias.isUseAs()) {
					renameFields.add(fieldId);
					renames.add(alias.getName());
				}
			}
			// is aggregate?
			if(columnVisitor.isAggregate()) {
				isAggregate = true;
				op = columnVisitor.getOp();
			}
		}
		
		//>>> 4. Call project function in Relation Class based on the field ids
		relation_result = relation_result.project(fields);
		
		//>>> 5. Rename
		if(!renameFields.isEmpty()) {
			relation_result = relation_result.rename(renameFields, renames);
		}
		
		//>>> 6. if aggregate, 
		if(isAggregate) {
			if(isAllColumns && op == AggregateOperator.COUNT) {
				isGroupBy = false;
			}
			relation_result = relation_result.aggregate(op, isGroupBy);
		}
		return relation_result;
	}
}

/*
 * net.sf.jsqlparser.statement.select
 * 
 * FromItem: An item in a "SELECT [...] FROM item1" statement. --Table
 * SelectItem: Anything between "SELECT" and "FROM" (that is, any column or
 * expression etc to be retrieved with the query) AllColumns: All the columns
 * (as in "SELECT * FROM ...") PlainSelect: The core of a "SELECT" statement (no
 * UNION, no ORDER BY) SelectExpressionItem: An expression as in
 * "SELECT expr1 AS EXPR"
 * 
 * net.sf.jsqlparser.statement.select.PlainSelect (The core of a "SELECT"
 * statement (no UNION, no ORDER BY))
 * 
 * getFromItem(): return FromItem. --Table getJoins(): return the list of Joins
 * 
 * net.sf.jsqlparser.statement.select.Join getOnExpression() : get On
 * Expression, type Expression, need to transfer to BinaryExpression
 * 
 * net.sf.jsqlparser.expression.BinaryExpression getLeftExpression() : get left
 * Expression getRightExpression() : get right Expression
 * 
 * 
 */
