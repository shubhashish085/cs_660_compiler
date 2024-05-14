package AST;
import Utilities.Visitor;

/**
 * Node represeting a continue statement.<br>
 * Example:<br>
 * <code>continue</code>
 */ 
public class ContinueStat extends Statement {
    /**
     * Constructs a continue statement given a {@link Token}.
     * @param c A Token.
     */
    public ContinueStat(Token c) {
	super(c);
	nchildren = 0;
    }
    
    /**
     * Calls {@link Visitor#visitContinueStat} on the visitor v.
     * @param v A reference to a Visitor object.
     * @return null
     */
    public Object visit(Visitor v) {
	return v.visitContinueStat(this);
    }
}
