package AST;

import Utilities.Visitor;

/** 
 * Node representing an array access expression (<i>target[index]</i>)<br>
 * Examples:<br>
 * <code>a[5]</code> (<code>a</code> is the target, <code>5</code> the index.)<br>
 * <code>o.get(7)[b.f(7)]</code> (<code>o.get(7)</code> is the target, <code>b.f(7)</code> is the index.)<br>
 * <code>b[4][5][6]</code> (<code>b[4][5]</code> is the target, <code>6</code> is the index.)
 */
public class ArrayAccessExpr extends Expression {
    /**
     * Constructs an array access expression based on a target expression and an index expression. Neither can be null.
     * @param target The target expression.
     * @param index The index expression.
     */
    public ArrayAccessExpr(Expression target, Expression index) {
	super(target);
	nchildren = 2;
	children = new AST[] { target, index };
    }

    /** 
     * Accessor method for getting the target expression of the array
     * access expression
     * @return Returns the target expression.
     */
    public Expression target() { return (Expression)children[0]; }
    /** 
     * Accessor method for getting the index expression of the array
     * access expression
     * @return Returns the index expression.
     */
    public Expression index()  { return (Expression)children[1]; }

    /** 
     * Returns a representation of the expression as <i>target[index]</i>. 
     */
    public String toString() {
	return target() + " [" + index() + "]";
    }

    /**
     * Calls {@link Visitor#visitArrayAccessExpr} on the visitor v.
     * @param v A reference to a Visitor object.
     */
    public Object visit(Visitor v) {
	return v.visitArrayAccessExpr(this);
    }
    
}
