package AST;
import Utilities.Visitor;

/**
 * Node representing a block.<br>
 * Example:<br>
 * <code>{ x=2; }</code>
 */
public class Block extends Statement {

    /**
     * Constructs a block based on a {@link Sequence} of statements.
     * @param stats A {@link Sequence} of statements.
     */
    public Block(Sequence /* of Statements */ stats) {
	super(stats);
	nchildren = 1;
	children = new AST[] { stats };
    }

    /**
     * Accessor for getting the sequence of statements.
     * @return A {@link Sequence} of statements.
     */
    public Sequence stats() { return (Sequence)children[0]; }

     /**
     * Calls {@link Visitor#visitBlock} on the visitor v.
     * @param v A reference to a Visitor object.
     * @return null.
     */    
    public Object visit(Visitor v) {
        return v.visitBlock(this);
    }  
}
