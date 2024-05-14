package AST;
import Utilities.Visitor;

/**
 * Node representing a class type.
 */
public class ClassType extends Type {

    /**
     * Points to the declaration of the type (i.e., the class
     * declaration) that this class type rerpresents.
     */
    public ClassDecl myDecl; 

    /**
     * True if this class type represents an intersection type.
     * Intersection types are computed by the type system when a
     * ternary expressions ({@link Ternary}) has class types in
     * both branched, but where neither is a super class of the other.
     */
    public Boolean isIntersectionType = false; 

    /**
     * Constructs a class type based on a class name. Note, the myDecl
     * gets set by the name resolution phase (Phase 3).
     * @param className The name of a class.
     */
    public ClassType(Name className) { 
	super(className);
	nchildren = 1;
	children = new AST[] { className };
    }

    /**
     * Accessor for getting the name of the class type.
     * @return The name of the class type.
     */
    public Name name() { 
	return (Name)children[0]; 
    }

    /**
     * Returns the name of the class type as a string <code>(ClassTyper: ...)</code>.
     * @return the name of the class type <code>(ClassTyper: ...)</code>.
     */
    public String toString() {
	return "(ClassType: " + name() + ")";
    }

    /**
     * Returns the name of the type. If not an intersection type then
     * the same result as toString().
     * @return The name of the class type.
     */
    public String typeName() {
	if (name().getname().startsWith("INT#")) {
	    String s = name().getname() + " (extends ";
	    s += myDecl.superClass().name().getname();
	    if (myDecl.interfaces().nchildren>0) {
		s += " implements ";
		for (int i=0; i<myDecl.interfaces().nchildren; i++) {
		    s += ((ClassType)myDecl.interfaces().children[i]).name().getname();
		    if (i <myDecl.interfaces().nchildren-1)
			s += ", ";
		}
	    }
	    s += ")"; 
	    
	    return s;
	} else	    
	    return name().getname();
    }

    /**
     * Returns the signature of the class type: <code>L...;</code>.
     * @return The signature of the class type as a string <code>L...;</code>.
     */
    public String signature() {
	return "L"+typeName()+";";
    }

    /**
     * Calls {@link Visitor#visitClassType} on the visitor v.
     * @param v A reference to a Visitor object.
     */
    public Object visit(Visitor v) {
	return v.visitClassType(this);
    }
}




