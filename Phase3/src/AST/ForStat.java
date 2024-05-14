package AST;
import Utilities.Visitor;

/**
 * Node representing a for statement<br>
 * Examples:<br>
 * <code>for (;;)<br>  
 *  &nbsp;&nbsp;...<br>  
 *<br>  
 * for(int i=0; i&lt;100; i++)<br>  
 *  &nbsp;&nbsp;a = a + i;</code>
 */
public class ForStat extends Statement {
    
    /**
     * Constructs a for statement node.
     * @param t The 'for' token. Needed for line number information as init, expr, and incr can be absent.
     * @param init A sequence of {@link ExprStat}.
     * @param expr An expression
     * @param incr A sequence of {@link ExprStat}.
     * @param stat A statement.
     */
    public ForStat(Token t, Sequence /* of ExprStat */ init, 
		   Expression expr, 
		   Sequence /* of ExprStat */ incr , 
		   Statement stat) {
	super(t);
	nchildren = 4;
	children = new AST[] { init, expr, incr, stat };
    }

    /**
     * Accessor for getting the initializers of the for statement.
     * @return A {@link Sequence} of {@link ExprStat}s.
     */
    public Sequence init() {
	return (Sequence)children[0];
    }

    /**
     * Accessor for getting the expression (boolean) of the for statement.
     * @return An expression (of Boolean type).
     */
    public Expression expr() {
	return (Expression)children[1];
    }

    /**
     * Accessor for getting the increment part of the for statement.
     * @return A {@link Sequence} of {@link ExprStat}s.
     */
    public Sequence incr() {
	return (Sequence)children[2];
    }

    /**
     * Accessor for getting the statemnt (body) of the for statement.
     * @return The statement (body) of the for statement.
     */
    public Statement stats() {
	return (Statement)children[3];
    }
    
    /**
     * Calls {@link Visitor#visitForStat} on the visitor v.
     * @param v A reference to a Visitor object.
     * @return null
     */   
    public Object visit(Visitor v) {
	return v.visitForStat(this);
    }
}
