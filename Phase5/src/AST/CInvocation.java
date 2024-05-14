package AST;
import Utilities.Visitor;

/**
 * Node representing an explicit constructor invocation;  an explicit
 * constructor invocation takes the form super(...) or this(...) where
 * the former calls a constructor in the super class and the latter a
 * different constructor in the same class; an explicit constructor
 * invocation always appears on the first line of a constructor<br>
 * Examples:<br>
 * <code>super()</code><br>
 * <code>this(4,5,null)</code>
 */
public class CInvocation extends Expression {
    /**
     * Reference to the constructor that gets called by this explicit
     * constructor invocation
     */
    public ConstructorDecl constructor; // This one is needed in the code generation phase

    /**
     * Reference to the class on which the explicit constructor
     * invocation calls the constructor.
     */
    public ClassDecl targetClass;
    // This one is needed for modifier checking

    /**
     * True if the explicit invocation call is of the form super(...).
     */
    private boolean superInv;

    /**
     * True if the explicit invocation call is of the form this(...).
     */
    private boolean thisInv;

    /**
     * Constructs an explicit constructor invocation based on a token
     * and a sequence of expressions.
     * @param cl The token 'super' or 'this'.
     * @param args The arguments passed to the constructor.
     */
    public CInvocation(Token cl, Sequence /* of Expression */ args) {
	super(cl);
	nchildren = 1;
	superInv = (cl.getLexeme().equals("super")); //(cl.sym == sym.SUPER);
	thisInv  = !superInv;
	children = new AST [] { args };
    }

    /**
     * Accssor for getting the {@link Sequence} of expressions that make up the arguments.
     * @return A {@link Sequence} of expressions making up the arguments.
     */
    public Sequence args() {
	return (Sequence)children[0];
    }

    /**
     * Returns true is the explicit constructor invocation is of the form super(...).
     * @return true if the call is super(...).
     */
    public boolean superConstructorCall() {
	return superInv;
    }

    /**
     * Returns true is the explicit constructor invocation is of the form this(...).
     * @return true if the call is this(...).
     */
    public boolean thisConstructorCall() {
	return thisInv;
    }

     /**
     * Calls {@link Visitor#visitCInvocation} on the visitor v.
     * @param v A reference to a Visitor object.
     */
    public Object visit(Visitor v) {
	return v.visitCInvocation(this);
    }
}

