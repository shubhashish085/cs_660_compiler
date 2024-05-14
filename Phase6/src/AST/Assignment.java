package AST;
import Utilities.Visitor;
/**
 * Represents an assignment.<br>
 * Examples:<br>
 * <code>o.f = a</code><br>
 * <code>b += 8 + f(4)</code>
 */
public class Assignment extends Expression {

    /**
     * Constructs an assignment with a left-hand side being a NameExpr, FieldRef or ArrayAccessExpr only.
     * @param left Left-hand side of assignment.
     * @param right Right-hand side of assignment.
     * @param op An {@link AssignmentOp}.
     */
    public Assignment(Expression /* NameExpr, FieldRef or ArrayAccess only */ left,
		      Expression right,
		      AssignmentOp op) {
	super(left);
	nchildren = 3;
	children = new AST[] { left, right, op};
    }

    /**
     * Accessor method for getting the left-hand side. This is one of
     * the following node types: NameExpr, FieldRef or
     * ArrayAccessExpr.
     * @return An {@link Expression} of type {@link NameExpr}, {@link FieldRef}, or {@link ArrayAccessExpr}.
     */
    public Expression left()  {
	return (Expression)children[0];
    }

    /**
     * Accessor method for getting the right-hand side. This can be
     * any expression except an ArrayLiteral.
     * @return An {@link Expression}.
     */
    public Expression right() {
	return (Expression)children[1];
    }

    /**
     * Accessor method for getting the assignment operator.
     * @return An {@link AssignmentOp}.
     */
    public AssignmentOp op()  {
	return (AssignmentOp)children[2];
    }

    /**
     * Calls {@link Visitor#visitAssignment} on the visitor v.
     * @param v A reference to a Visitor object.
     * @return null.
     */
    public Object visit(Visitor v) {
        return v.visitAssignment(this);
    }    
}
