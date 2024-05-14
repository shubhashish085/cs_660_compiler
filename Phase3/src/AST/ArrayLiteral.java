package AST;

import Utilities.Visitor;

/**
 * Node representing an array literal.<br>
 * Examples:<br>
 * <code>{1,2,3}</code> with three elements: <code>1</code>, <code>2</code>, and <code>3</code><br>
 * <code>{{1,2,3},{4,5,6},{7,8,9}}</code> with three elements: <code>{1,2,3}</code>, <code>{4,5,6}</code>, and <code>{7,8,9}</code>.
 */
public class ArrayLiteral extends Expression {
    
    /** 
     * Constructs an array literal based on a {@link Sequence Sequence} of expressions.
     * @param seq Sequence of expressions.
     */
    public ArrayLiteral(Sequence /* Expression */ seq) {
	super(seq);
	nchildren = 1;
	children = new AST[] { seq };
    }

    /**
     * Accessor method for getting the sequence of expressions.
     * @return A sequence of expressions.
     */
    public Sequence elements() {
	return (Sequence)children[0];
    }

    /**
     * Returns a string representing the array literal.
     * @return the constant string <code>{.,.,.,.,.}</code>.
     */
    public String toString() {
	return "{.,.,.,.,.}";
    }

    /**
     * Calls {@link Visitor#visitArrayLiteral} on the visitor v.
     * @param v A reference to a Visitor object.
     */
    public Object visit(Visitor v) {
	return v.visitArrayLiteral(this);
    }
}
