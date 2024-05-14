package AST;
import Utilities.Visitor;

/**
 * Node representing an invocation of a method.
 * Example:<br>
 * <code>f(6)</code><br>
 * <code>this.g()</code><br>  
 * <code>A.foo(6)</code><br>           
 * <code>a.c.d.e.f.g()</code>
 */
public class Invocation extends Expression {
    
    /** Note target() can return null */

    /**
     * This method that get called by this invocation. This field is set by the Type Checker.
     */
    public MethodDecl targetMethod;

    /**
     * The type of the target of the invocation. Set by the Type Checker.
     */
    public Type targetType; 
    
    public Invocation(Expression target, Name name, 
		      Sequence /* of Expressions*/ params) {
	super(target);
	nchildren = 3;
	children = new AST[] { target, name, params };
    }
    
    public Invocation(Name name , Sequence /* of  Expressions */ params) {
	super(name);
	nchildren = 3;
	children = new AST[] { null, name, params };
    }
    
    public Expression target()     {
	return (Expression)children[0];
    }
    
    public Name methodName() {
	return (Name)children[1];
    }

    public Sequence params() {
	return (Sequence)children[2];
    }
    
    public Object visit(Visitor v) {
	return v.visitInvocation(this);
    }
}
