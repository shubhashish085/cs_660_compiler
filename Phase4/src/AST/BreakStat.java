package AST;
import Utilities.Visitor;

/**
 * Node representing a break statement; a break statement is used for breaking out of a loop (while, do, for) or a swtich Statement<br>
 * Examples:<br>
 * <code>break</code><br>
 */
public class BreakStat extends Statement {

    /**
     * Constructs a break statement based on a token (representing the
     * keyword 'break'). 
     * @param b A {@link Token} representing the 'break' keyword.
     */
    public BreakStat(Token b) {
	super(b);
	nchildren = 0;
    }
    
    /**
     * Calls {@link Visitor#visitBreakStat} on the visitor v.
     * @param v A reference to a Visitor object.
     * @return null.
     */
    public Object visit(Visitor v) {
        return v.visitBreakStat(this);
    }
}
