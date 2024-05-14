package AST;

/** 
 * Abstract class representing an expression. Note, this is an
 * abstract class, thus none of the constructors can be called using
 * 'new'.
 */
public abstract class Expression extends AST {
    
    /** 
     * The {@link Type} of the expression. This field should be set by the {@link TypeChecker}. 
     */
    public Type type = null;

    /**
     * Constructs an expression based on a token.
     * @param t A {@link Token}.
     */
    public Expression(Token t) {
	super(t);
    }

    /**
     * Constructs an Expression based on another AST node.
     * @param a Some AST or subclass there of. 
     */
    public Expression(AST a) {
	super(a);
    }

    /**
     * Returns true if this expression is constant. This method always
     * returns 'false' and thus should be re-implemented in all
     * subclasses of this class.
     * @return false
     */
    public boolean isConstant() {
	return false;
    }

    /**
     * This method should also be re-implemented by all sub classes.
     * @return null
     */
    public Object constantValue() {
	return null;
    }

    /**
     * Always returns false.
     * @return false
     */
    public boolean isClassName() {
	return false;
    }
}
