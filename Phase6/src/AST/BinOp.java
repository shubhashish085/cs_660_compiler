package AST;
import Utilities.Visitor;

/**
 * Represents a binary operator. Only used in connection with an {@link BinaryExpr} node.
 */
public class BinOp extends AST {

    /**
     * The operator kind. This determines what the operator is.
     */
    public int kind;

    /**
     * +
     */
    public static final int PLUS       = 1;

    /**
     * -
     */
    public static final int MINUS      = 2;

    /**
     * *
     */
    public static final int MULT       = 3;

    /**
     * /
     */
    public static final int DIV        = 4;

    /**
     * %
     */
    public static final int MOD        = 5;

    /**
     * &lt;&lt;
     */
    public static final int LSHIFT     = 6;

    /**
     * &gt;&gt;
     */
    public static final int RSHIFT     = 7;
    
    /**
     * &gt;&gt;&gt;
     */
    public static final int RRSHIFT    = 8;

    /**
     * &lt;
     */
    public static final int LT         = 9;

    /**
     * &gt;
     */    
    public static final int GT         = 10;

    /**
     * &lt;=
     */
    public static final int LTEQ       = 11;

    /**
     * &gt;=
     */
    public static final int GTEQ       = 12;

    /**
     * <i>instanceof</i>
     */
    public static final int INSTANCEOF = 13;

    /**
     * ==
     */
    public static final int EQEQ       = 14;

    /**
     * !=
     */
    public static final int NOTEQ      = 15;

    /**
     * &amp;
     */
    public static final int AND        = 16;

    /**
     * |
     */
    public static final int OR         = 17;

    /**
     * ^
     */
    public static final int XOR        = 18;

    /**
     * &amp;&amp;
     */
    public static final int ANDAND     = 19;

    /**
     * ||
     */
    public static final int OROR       = 20;
   
    /**
     * Array of strings representing the operators (in source code). Indexed by 'kind'.
     */
    public static final String [] opSyms = {
	"", "+", "-", "*", "/", "%", "<<", ">>", ">>>", "<", ">",
	"<=", ">=", "instanceof", "==", "!=", "&", "|", "^",
	"&&", "||" };

    /**
     * Constructs a binary operator based on a Token (for line numbers) and a kind.
     * @param t An object of type {@link Token}.
     * @param kind The kind of the operators (One of the constants of this file).
     */
    public BinOp(Token t, int kind) {
	super(t);
	this.kind = kind;
    }

    /**
     * Constructs a binary operator based on a kind.
     * @param kind The kind of the operators (One of the constants of this file).
     */
    public BinOp(int kind) {
	super(0,0);
	this.kind = kind;
    }

    /**
     * Accessor method for getting the operator as a string.
     * @return The operator as a string.
     */
    public String operator() { return opSyms[kind]; }

    /**
     * Calls {@link Visitor#visitBinOp} on the visitor v.
     * @param v A reference to a Visitor object.
     */    
    public Object visit(Visitor v) {
        return v.visitBinOp(this);
    }
}
