package AST;
import Utilities.Visitor;
import Utilities.Error;
import java.math.*;
/**
 * Represents a binary expression.<br>
 * Examples:<br>
 * <code>a + b</code><br>
 * <code>foo(a) &lt;&lt;&lt; o.c</code><br>
 * <code>r instanceof A</code>
 */
public class BinaryExpr extends Expression {

    /**
     * Constructs a binary expression based on two expressions and a
     * {@link BinOp} binary operator.
     * @param left An {@link Expression}.
     * @param right An {@link Expression}.
     * @param op A {@link BinOp} operator.
     */
    public BinaryExpr(Expression left, Expression right, BinOp op) {
	super(left);
	nchildren = 3;
	children = new AST[] { left, right, op };
    }

    /**
     * Accessor method for getting the left expression.
     * @return The left expression of the binary expression
     */
    public Expression left()  {
	return (Expression)children[0];
    }

    /**
     * Accessor method for getting the right expression.
     * @return The right expression of the binary expression
     */
    public Expression right() {
	return (Expression)children[1];
    }

    /**
     * Accessor method for getting the operator of the binary expression.
     * @return The {@link BinOp} binary operator.
     */
    public BinOp op() {
	return (BinOp)children[2];
    }

    /**
     * Returns true if both operands are constants.
     * @return true if both operands are constants.
     */
    public boolean isConstant() {
	return left().isConstant() && right().isConstant();
    }

    /**
     * Returns the value of the binary expression if it is constant. Note, this method should only be called if the binary expression <b>is</b> a constant value.
     * @return A value of type BigDecimal.
     */
    public Object constantValue() {
	if (!isConstant())
	    Error.error(this, "Binary Expression is not constant.");
	BigDecimal lval = (BigDecimal) left().constantValue();
	BigDecimal rval = (BigDecimal) right().constantValue();
	
	switch(op().kind) {
	case BinOp.PLUS:  return lval.add(rval); 
	case BinOp.MINUS: return lval.subtract(rval); 
	case BinOp.MULT:  return lval.multiply(rval); 
	case BinOp.DIV:   
	    if (left().type.isIntegralType() && right().type.isIntegralType()) 
		return new BigDecimal(lval.toBigInteger().divide(rval.toBigInteger()));
	    new BigDecimal(lval.doubleValue()/rval.doubleValue());
	case BinOp.MOD: 
	case BinOp.LSHIFT:
	case BinOp.RSHIFT:
	case BinOp.RRSHIFT:
	case BinOp.AND:
	case BinOp.OR:
	case BinOp.XOR:
	    long lint = lval.longValue();
	    long rint = rval.longValue();
	    switch(op().kind) {
	    case BinOp.MOD:    return new BigDecimal(Long.toString(lint % rint)); 
	    case BinOp.LSHIFT: return new BigDecimal(Long.toString(lint << rint));
	    case BinOp.RSHIFT: return new BigDecimal(Long.toString(lint >> rint));
	    case BinOp.RRSHIFT: return new BigDecimal(Long.toString(lint >>> rint));
	    case BinOp.AND:    return new BigDecimal(Long.toString(lint & rint)); 
	    case BinOp.OR:     return new BigDecimal(Long.toString(lint | rint)); 
	    case BinOp.XOR:    return new BigDecimal(Long.toString(lint ^ rint)); 
	    }
	}
	return null;
    } 


    /**
     * Calls {@link Visitor#visitBinaryExpr} on the visitor v.
     * @param v A reference to a Visitor object.
     */
    
    public Object visit(Visitor v) {
	return v.visitBinaryExpr(this);
    }
    
}

