package AST;
import Utilities.Visitor;

/**
 * Represents an assignment operator. Only used in connection with an {@link Assignment} node.
 */
public class AssignmentOp extends AST {
    /**
     * The operator kind. This determines what the operator is.
     */
    public int kind;

    /** 
     * = 
     */
    public static final int EQ        = 1;
    /** 
     * *= 
     */
    public static final int MULTEQ    = 2;
    /**
     * /=
     */
    public static final int DIVEQ     = 3;
    /**
     * %=
     */
    public static final int MODEQ     = 4;
    /**
     * +=
     */
    public static final int PLUSEQ    = 5;
    /**
     * -=
     */
    public static final int MINUSEQ   = 6;
    /**
     * &lt;&lt;=
     */
    public static final int LSHIFTEQ  = 7;
    /**
     * &gt;&gt;=
     */
    public static final int RSHIFTEQ  = 8;
    /**
     * &gt;&gt;&gt;= 
     */
    public static final int RRSHIFTEQ = 9;
    /**
     * &amp;=
     */
    public static final int ANDEQ     = 10;
    /**
     * |=
     */
    public static final int OREQ      = 11;
    /**
     * ^=
     */
    public static final int XOREQ     = 12;

    /**
     * Array of strings representing the operators (in source code). Indexed by 'kind'.
     */
    public static final String [] opSyms = {
	"", "=", "*=", "/=", "%=", "+=", "-=", "<<=", 
	">>=", ">>>=", "&=", "|=", "^="  };

    /**
     * Constructs an assingment operator based on a Token (for line numbers) and a kind.
     * @param t An object of type {@link Token}.
     * @param kind The kind of the operators (One of the constants of this file).
     */
    public AssignmentOp(Token t, int kind) {
	super(t);
	this.kind = kind;
    }

    
    /**                                                                                                                      
     * Constructs an assingment operator based on a kind.
     * @param kind The kind of the operators (One of the constants of this file).
     */
    public AssignmentOp(int kind) {
	super(0,0);
	this.kind = kind;
    }

    /**
     * Accessor method for getting the operator as a string.
     * @return The operator as a string.
     */
    public String operator() {
	return opSyms[kind];
    }

    /**
     * Returns the operator as a string.
     * @return The operator as a string.
     */
    public String toString() {
	return opSyms[kind];
    }
    
    /**
     * Calls {@link Visitor#visitAssignmentOp} on the visitor v.
     * @param v A reference to a Visitor object.
     */
    public Object visit(Visitor v) {
	return v.visitAssignmentOp(this);
    }
}
