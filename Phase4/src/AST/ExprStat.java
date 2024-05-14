package AST;
import Utilities.Visitor;

/**
 * Node representing an expression statement; that is, an expressions
 * used as a statement where the value of the expression is ignored.<br>
 * Examples:<br>
 * <code>foo(x);</code> (foo is non-void)<br>
 * <code>i++;</code>
*/
public class ExprStat extends Statement {

    /** 
     * Constructs an expression statement based on an expression.
     * @param expression An expression.
     */
    public ExprStat(Expression expression) {
	super(expression);
	nchildren = 1;
	children = new AST[] { expression };
    }

    /**
     * Accessor for getting the expression of the expression statement.
     * @return An expression.
     */
    public Expression expression() {
	return (Expression)children[0];
    }

    /**
     * Calls {@link Visitor#visitExprStat} on the visitor v.
     * @param v A reference to a Visitor object.
     * @return null
     */
    public Object visit(Visitor v) {
	return v.visitExprStat(this);
    }
}
