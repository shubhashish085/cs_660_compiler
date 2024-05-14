package AST;
import Utilities.Visitor;

/** 
 * Node representing an if statement.<br>
 * Examples:<br><code>
 *if (a&gt;0)<br>
 *  &nbsp;&nbsp;b = 9;<br>  
 *<br>  
 *if (b == a)<br>  
 *  &nbsp;&nbsp;q = 9;<br>  
 *else<br>  
 *  &nbsp;&nbsp;break;</code>
 */
public class IfStat extends Statement {

    /**
     * Constructs an if statement with no else part.
     * @param expr An expression.
     * @param thenpart A statement.
     */
    public IfStat(Expression expr, Statement thenpart) {
	this(expr, thenpart, null);
    }

    /**
     * Constructs an if statement.
     * @param expr An expression.
     * @param thenpart A statement.
     * @param elsepart A statement.
     */
    public IfStat(Expression expr, Statement thenpart, Statement elsepart) {
	super(expr);
	nchildren = 3;
	children = new AST[] { expr, thenpart, elsepart };
    }

    /**
     * Accessor for getting the expression of the if statement.
     * @return An expression.
     */
    public Expression expr() {
	return (Expression)children[0];
    }

    /**
     * Accessor for getting the then-part statement of the if statement.
     * @return A statement.
     */
    public Statement thenpart() {
	return (Statement)children[1];
    }

    /**
     * Accessor for getting the else-part statement of the if statement.
     * @return A statement.
     */
    public Statement  elsepart() { return (Statement)children[2];  }

    /**
     * Calls {@link Visitor#visitIfStat} on the visitor v.
     * @param v A reference to a Visitor object.
     * @return null
     */
    public Object visit(Visitor v) {
	return v.visitIfStat(this);
    }
}
