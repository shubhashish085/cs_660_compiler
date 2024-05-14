package AST;
import Utilities.Visitor;

/**
 * Node representing an entire source program. This node cotains a
 * {@link Sequence} of {@link ClassDecl} nodes.
 */
public class Compilation extends AST {

    /**
     * Constructs a compilation based on a sequence of classes and interfaces.
     * @param types {@link Sequence} of {@link ClassDecl}.
     */
    public Compilation(Sequence types) {
	super(types);
	nchildren = 1;
	children = new AST[] { types };
    }

    /**
     * Accessor for getting the sequence of classes and interfaces.
     * @return A {@link Sequence} of {@link ClassDecl}.
     */
    public Sequence types() {
	return (Sequence)children[0];
    }

    /**
     * Calls {@link Visitor#visitCompilation} on the visitor v.
     * @param v A reference to a Visitor object.
     * @return null
     */
    public Object visit(Visitor v) {
	return v.visitCompilation(this);
    }
}
