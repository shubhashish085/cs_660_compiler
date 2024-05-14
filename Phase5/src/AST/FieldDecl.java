package AST;
import Utilities.Visitor;

/**
 * Node representing a field declaration
 * Example:<br>
 * <code>private int x = 9</code>
 */
public class FieldDecl extends ClassBodyDecl implements VarDecl  {

    /**
     * An internal representation of the modifiers. See the {@link Modifiers} class.
     * Set in the constructor.
     */
    public Modifiers modifiers;

    /**
     * True if the field is a member of an interface.
     */
    public boolean interfaceMember = false;

    /**
     * Constructs a field declaration node.
     * @param modifiers A Sequence of modifiers.
     * @param type The type of the field.
     * @param var The {@link Var} containing the name and the initializer of the field.
     * @param interfaceMember True if the field is in an interface.
     */
    public FieldDecl(Sequence /* of Modifier */ modifiers,
		     Type type, Var var, boolean interfaceMember) {
	super(type);
	nchildren = 3;
	this.modifiers = new Modifiers();
	this.modifiers.set(false, false, modifiers);
	children = new AST[] { modifiers, type, var };
	this.interfaceMember = interfaceMember;
    }

    /**
     * Accessor for getting the {@link Sequence} of modifiers of the field declaration.
     * @return Sequence of modifiers.
     */
    public Sequence modifiers() {
	return (Sequence)children[0];
    }

    /**
     * Accessor for getting the type of the field declaration.
     * @return The type of the field.
     */
    public Type type() {
	return (Type)children[1];
    }

    /**
     * Accessor for getting the variable part of the field declaration. This contains the name and the initializer. See {@link Var} for the accessors.
     * @return The variable part of the field (name and initializer).
     */
    public Var  var() {
	return (Var)children[2];
    }

    /**
     * Returns the name of the field as a string.
     * @return The name of the field as a string.
     */
    public String name() {
	return var().name().getname();
    }

    /**
     * Returns the name of the field as a string.
     * @return The name of the field as a string.
     */
    public String getname() {
	return var().name().getname();
    }
    
    /**
     * Returns a string representation of the field: FieldDecl(Type: ... Name: ...).
     * @return A string representation of the field.
     */
    public String toString() {
	return "FieldDecl>(Type:" + type() + " " + "Name:" + var() + ")";
    }

    /**
     * Returns true if the type of the field is a class.
     * @return True if the type of the field is a class, false otherwise.
     */
    public boolean isClassType() {
	return (type() instanceof ClassType);
    }

    /**
     * Not applicable for fields. Always returns 0.
     * @return 0 - This method is not applicable for fields.
     */
    public int address() {
	return 0;
    }

    /**
     * Returns true if the field is declared static.
     * @return True if the field is static, false otherwise.
     */
    public boolean isStatic() {
	return modifiers.isStatic();
    }

    /**
     * Returns the modifiers as a {@link Modifier} object.
     * @return A {@link Modifier} object.
     */
    public Modifiers getModifiers() {
	return this.modifiers;
    }

    /**
     * Calls {@link Visitor#visitFieldDecl} on the visitor v.
     * @param v A reference to a Visitor object.
     * @return null
     */
    public Object visit(Visitor v) {
	return v.visitFieldDecl(this);
    }
}
