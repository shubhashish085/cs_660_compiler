package AST;
import Utilities.Visitor;

/**
 * Node representing a do statement<br>
 * Example:<br>
 *   <code>do {<br>
 *     &nbsp;&nbsp;...<br>
 *   } while (...)</code>
 */
public class DoStat extends Statement {
    /**
     * Constructs a do statement given a statement and an expression.
     * @param stat A statement.
     * @param expr An expression.
     */
    public DoStat(Statement stat, Expression expr) {
	super(expr);
	nchildren = 2;
	children = new AST[] { stat, expr };
    }

    /**
     * Accessor for getting the statement of the do statement.
     * @return A statement.
     */
    public Statement stat() {
	return (Statement)children[0];
    }
    /**
     * Accessor for getting the expression of the do statement.
     * @return An expression.
     */
    public Expression expr() {
	return (Expression)children[1];
    }
    
    /**
     * Calls {@link Visitor#visitDoStat} on the visitor v.
     * @param v A reference to a Visitor object.
     * @return null
     */
    public Object visit(Visitor v) {
	return v.visitDoStat(this);
    }
}
