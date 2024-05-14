package AST;

import Utilities.Visitor;

/**
 * Represents an array type.<br>
 * Examples:<br>
 * <code>int[]</code> with base type <code>int</code> and depth 1.<br>
 * <code>double[][]</code> with base type <code>double</code> and depth 2.<br>
 * <code>A[][][]][</code> with base type <code>A</code> and depth 4.
 */
public class ArrayType extends Type {

    /** 
     * The number of <code>[]</code>, that is, the depth of the array.
     */
    private int depth = 0; // How many set of [ ] were there?

    /** 
     * Constructs an arraytype with base type <code>baseType</code> and depth <code>depth</code>.
     * @param baseType The base type of the array.
     * @param depth The dimensionality (depth) of the array.
     */
    public ArrayType(Type baseType, int depth) {
	super(baseType);
	nchildren = 1;
	this.depth = depth;
	children = new AST[] { baseType };		
    }

    /**
     * Accessor method for getting the base type.
     * @return The base type of the array.
     */
    public Type baseType() { 
	return (Type) children[0]; 
    }

    /**
     * Accessor method for getting the depth.
     * @return The depth of the array. 
     */
    public int getDepth() { 
	return depth; 
    }

    /**
     * Returns the name of the type.
     * @return (ArrayType: .... ).
     */
    public String toString() {
	return "(ArrayType: " + typeName() + ")";
    }

    /**
     * Returns the JVM signature of the type. Array types have signatures of the form [...[T where T is a type and the number of [ equals the depth. For example, <code>int[][][]</code> has signature <code>[[[I</code>.
     * @return The JVM signature of the array.
     */
    public String signature() {
	String s = "";
	for (int i=0;i<depth; i++)
	    s += "[";				
	return s + baseType().signature();
    }

    /**
     * Returns the name of the type in a readable fashion. T[]...[] where T is the base type and the number of [] equals the depth.
     * @return T[]...[] for a base type T.
     */
    public String typeName() {
	String s = baseType().typeName();
	for (int i=0; i<depth; i++)
	    s = s + "[]";
	return s;
    }

    /**
     * Calls {@link Visitor#visitArrayType} on the visitor v.
     * @param v A reference to a Visitor object.
     */
    public Object visit(Visitor v) {
	return v.visitArrayType(this);
    }
}             








