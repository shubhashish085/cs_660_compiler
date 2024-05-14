package AST;
import Utilities.Visitor;
import Utilities.Error;
import java.math.*;

/** 
 * Node representing a cast expression (<i>(type)expression</i>)<br>
 * Examples:<br>
 * <code>(A)b</code><br>
 * <code>(float)5.4</code>
 */
public class CastExpr extends Expression {

    /**
     * Constructs a cast expression based on a type and an expression.
     * @param ct The type to which the expression will be cast.
     * @param expr The expression being cast.
     */
    public CastExpr(Type ct, Expression expr) {
	super(ct);
	nchildren = 2;
	children = new AST[] { ct, expr };
    }

    /**
     * Accessor method for getting the cast type.
     * @return The {@link Type} to which the expression will be cast.
     */
    public Type type() {
	return (Type)children[0];
    }

    /**
     * Accessor method for getting the expression.
     * @return The expression being cast.
     */
    public Expression expr() {
	return (Expression)children[1];
    }

    /**
     * Returns true if the expression is constant.
     * @return true if the expression is constant.
     */
    public boolean isConstant() {
	return expr().isConstant();
    }

    /**
     * Returns the value of expression if it is constant. Note, this method should only be called if the cast expression <b>is</b> a constant value.
     * @return A value of type BigDecimal.
     */
    public Object constantValue() {
	// TODO: I don't think this method is correctly implemented.     
	if (!isConstant())
	    Error.error(this, "Cast expression is not constant.");
	if (type().isIntegralType()) 
	    return new BigDecimal(((BigDecimal)expr().constantValue()).toBigInteger());
	return expr().constantValue();
    }

    /**
     * Calls {@link Visitor#visitCastExpr} on the visitor v.
     * @param v A reference to a Visitor object.
     * @return null
     */
    public Object visit(Visitor v) {
	return v.visitCastExpr(this);
    }    
}
