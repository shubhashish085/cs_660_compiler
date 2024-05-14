package AST;
import Utilities.Visitor;
import java.util.*;

/**
 * Node representing a contructor declaration<br>
 * Examples:<br>
 * <code> public A() {<br>
 *    &nbsp;&nbsp;super();<br>
 *  }<br>
 *  <br>
 *  private B(int x) <br>
 *    &nbsp;&nbsp;this.myX = x;<br>
 *  }<br>
 *<br>
 *  public C(int x) {<br>
 *    &nbsp;&nbsp;this(0, x);<br>
 *    &nbsp;&nbsp;this.y = x-2;<br>
 *  }</code>
 */
public class ConstructorDecl extends ClassBodyDecl  {
    /**
     * An internal representation of the modifiers. See the {@link Modifiers} class.
     * Set in the constructor.
     */
    private Modifiers modifiers;
    
    /**
     * Constructs a constructor declaration node. Note, the explicit
     * constructor invocation and the sequence of statements making up
     * the body <b>both</b> come from the {@link ConstructorBody}.
     * @param modifiers A Sequence of modifiers.
     * @param name The name of the class/constructor.
     * @param params A sequence of parameters.
     * @param cInvocation An explicit constructor invocation (may be null).
     * @param body A sequence of statements (never null, but may be empty).
     */
    public ConstructorDecl(Sequence /* of Modifiers */ modifiers, Name name, 
			   Sequence /* of ParamDecl */ params, 
			   CInvocation cInvocation,
			   Sequence /* of Statement */ body) {
	super(name);
	nchildren = 5;
	this.modifiers = new Modifiers();
	this.modifiers.set(false, true, modifiers);
	children = new AST[] { modifiers, name, params, cInvocation, body };
    }

    /**
     * Accessor for getting the {@link Sequence} of {@link Modifier}s
     * for this constructor.
     * @return A {@link Sequence} of {@link Modifier}s.
     */
    public Sequence modifiers()  {
	return (Sequence)children[0];
    }

    /**
     * Accessor for getting the name of the constructor.
     * @return The name of the constructor.
     */
    public Name name() {
	return (Name)children[1];
    }

    /**
     * Accessor for getting the formal parameters of the constructor.
     * @return A {@link Sequence} of {@link ParamDecl}s.
     */
    public Sequence params() {
	return (Sequence)children[2];
    }

    /**
     * Accessor for getting the explicit constructor invocation (may be null).
     * @return A {@link CInvocation} or null.
     */
    public CInvocation cinvocation() {
	return (CInvocation)children[3];
    }

    /**
     * Accessor for getting the {@link Sequence} of statements that
     * make up the rest of the body of the constructor.
     * @return A {@link Sequence} of statements.
     */
    public Sequence body() {
	return (Sequence)children[4];
    }

    /**
     * Returns the name of the constructor as a string.
     * @return The name of the constructor as a string.
     */
    public String getname() {
	return name().getname();
    }   

    /**
     * Returns the signature of the parameters. This does <b>not</b>
     * include the ( ) around the signature or the V signatuer of the
     * return type.
     * @return The signature of the parameters as a string.
     */
    public String paramSignature() {
	String s = "";
	Sequence params = params();
	
	for (int i=0;i<params.nchildren;i++)
	    s = s + ((ParamDecl)params.children[i]).type().signature();
	return s;
    }

    /**
     * Always returns false. Constructors cannot be static.
     * @return false
     */
    public boolean isStatic() {
	return modifiers.isStatic();
    }

    /**
     * Returns the modifiers as a {@link Modifier} object.
     * @return The modifiers as a {@link Modifier} object.
     */
    public Modifiers getModifiers() {
	return this.modifiers;
    }

    /**
     * Calls {@link Visitor#visitConstructorDecl} on the visitor v.
     * @param v A reference to a Visitor object.
     * @return null
     */
    public Object visit(Visitor v) {
	return v.visitConstructorDecl(this);
    }
}

