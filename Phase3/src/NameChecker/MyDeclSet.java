package NameChecker;

import AST.*;
import Utilities.Error;
import Utilities.SymbolTable;
import Utilities.Visitor;

/**
 * This visitor is not needed if classes and interfaces are defined in
 * the order of inheritence, i.e., superclasses and superinterfaces
 * are defined first and so on. if you want the freedom to put it in
 * any order you need this traversal. If you don't care then make sure
 * you have an implementation of visitClassType in
 * ClassAndMemberFinder.
 */
public class MyDeclSet extends Visitor {

    /**
     * The global class table. This field is set by the constructor.
     */
    private SymbolTable classTable;

    /**
     * Constructs a MyDeclSet visitor object.
     * @param classTable The (global) table of classes (stored in {@link Phases.Phase#classTable Phases/Phase.ClassTable}).
     * @param debug Determine if this visitor should produce output.
     */
    public MyDeclSet(SymbolTable classTable, boolean debug) { 
	this.classTable = classTable; 
	this.debug = debug;
    }

    /**
     * Sets myDecl for the the paramter ct.
     * @param ct A {@link ClassType} object.
     */
    public Object visitClassType(ClassType ct) {
	ClassDecl cd = (ClassDecl) classTable.get(ct.typeName());
	
	println("ClassType:\t Setting myDecl for '" + ct.typeName() + "'");
	
	if (cd == null) 
	    Error.error(ct,"Class '" + ct.typeName() + "' not found.");
	ct.myDecl = cd;
	return null;
    }
}

