package NameChecker;

import AST.*;
import Utilities.Error;
import Utilities.SymbolTable;
import Utilities.Visitor;
import Parser.*;
import Phases.Phase2;

/** 
 * A visitor class which visits classes, and their members and enters
 * them into the appropriate tables.
 */
public class ClassAndMemberFinder extends Visitor {

    /**
     * Adds a method to the method table of a class.
     * @param cd The class that the method should be added to.
     * @param md The MethodDeclaration that is being added
     * @param name The name of the method or constructor being added to the class' method table.
     * @param sig The signature of the method/constructor being added.
     */     
    private void addMethod(ClassDecl cd, AST md, String name, String sig) {
	SymbolTable st = (SymbolTable)cd.methodTable.get(name);
	
	// Are there methods defined in this class' symbol table with the right name?
	if (st != null) {
	    // Are there any methods with the same parameter signature?
	    Object m = st.get(sig);
	    if (m != null) 		
		// CMF1.java
		Error.error(md,"Method " + name + "(" + Type.parseSignature(sig) + " ) already defined.");
	}
	
	// If we are inserting a constructor just insert in now - don't
	// go looking in the super classes.
	if (name.equals("<init>")) {
	    if (st == null) {
		//A constructor with this name has never been inserted before
		// Create a new symbol table to hold all constructors.
		SymbolTable mt = new SymbolTable();
		// Insert the signature with the method decl.
		mt.put(sig, md);
		// Insert this symbol table into the method table.
		cd.methodTable.put(name, mt);		
	    } else 
		st.put(sig, md);
	    return ;
	}
	
	// Static initializers
	if (name.equals("<clinit>")) {
	    // We can only have one static initializer, so it doesn't exist in the table.
	    SymbolTable mt = new SymbolTable();
	    mt.put(sig, md);
	    cd.methodTable.put(name, mt);
	    return;
	}
	
	// Ok, we have dealt with constructors and static initializers, now deal with methods.
	
	// We will not search the hierarchy now - we do that later
	// when the entire class hierarchy has been defined. That
	// means we might be violating certain things now, but that is
	// ok, we will catch it later.
	
	// It's all good - just insert it.
	if (st == null) {
	    // A method of this name has never been inserted before.
	    // Create a new symbol table to hold all methods of name 'name'
	    SymbolTable mt = new SymbolTable();
	    // Insert the signature with the method decl.
	    mt.put(sig, md);
	    // Insert this symbol table into the method table.
	    cd.methodTable.put(name, mt);
	} else 
	    // Methods with this name have been defined before, so just use that entry.
	    st.put(sig, md);
    }

    /**
     * Adds a {@link FieldDecl} to a class' field table.
     * @param cd The class to which we are adding the field.
     * @param f The FieldDecl being added.
     * @param name The name of the field being added to the class' field table.    
     */
    private void addField(ClassDecl cd, FieldDecl f, String name) {
	// We will not search the hierarchy now - we do that later when the 
	// entire class hierarchy has been defined.
	cd.fieldTable.put(name,f);
    }
    
    // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        
    /** 
     * Contains references to all class declarations. This is set in the constructor.
     */
    private SymbolTable classTable;

    /** 
     * Holds a reference to the current class. This is set in {@link #visitClassDecl(ClassDecl)}.
     */
    private ClassDecl currentClass;

    /**
     * Constructs a ClassAndMemberFinder visitor object.
     * @param classTable The (global) table of classes (stored in {@link Phases.Phase#classTable Phases/Phase.ClassTable}).
     * @param debug Determine if this visitor should produce output.
     */
    public ClassAndMemberFinder(SymbolTable classTable, boolean debug) { 
	this.classTable = classTable; 
	this.debug = debug;
    }
    

    /**
     * Visits a {@link Block}.
     * @param bl A {@link Block} object.
     * Inserts bl into the method table of the current class.<br>
     * <ul>
     * <li>Put bl into the current method's method table.</li>
     * <li>The block represents a common constructor, but has not name or parameters.</li>
     * <li>We will simply use the name &le;cinit&gt; (note, not &lt;clinit&gt; which is the name of the static initializer.</li>
     * </ul>
     * @return null
     */
    public Object visitBlock(Block bl) {
	println(bl.line + ":\tVisiting a Block.");
	println(bl.line + ":\tInserting common contructor <cinit> into method table of class '" + currentClass + "'.");
	if (currentClass.methodTable.get("<cinit>") != null) {
	    Error.error("There can only be one common constructor.");
	}
	addMethod(currentClass, bl, "<cinit>", "");
	
	return null;
    }

    /** 
     * Visits a {@link ClassDecl}.
     * @param cd A {@link ClassDecl} object.
     * Inserts cd into the global class table.<br>
     * <ul>
     * <li>Put cd into the global class table.</li>
     * <li>If no super class was defined, set cd's super class to be Object. The Object class and its myDecl lives in {@link Phases.Phase2}.</li>
     * <li>Updates currentClass.</li>
     * <li>Visit the class.</li>
     * <li>If there are no constructors defined in cd, then create a default contructor and add it to the class and the method table.</li>
     * </ul>
     * @return null
     */
    public Object visitClassDecl(ClassDecl cd) {	
	println(cd.line + ":\tVisiting a ClassDecl.");
	println(cd.line + ":\tInserting class '" + cd.name() +"' into global class table.");
	
	// Enter this class into the class table 
	classTable.put(cd.name(), cd);
	
	// 01/17/2012 added for allowing for common superclass 'Object'
	// For espresso it is simiilar to java/lang/Object for Java
	// see Phases/Phases2.java for the class 'Object'
	if (cd.superClass() == null && !cd.name().equals("Object")) {
	    cd.children[2] = new ClassType(new Name(new Token(sym.IDENTIFIER,"Object",cd.line,0,0)));
	    ((ClassType)cd.children[2]).myDecl = Phase2.Objects_myDecl;
	}		
	// Update the current class 
	currentClass = cd;
	
	// Visit the children
	super.visitClassDecl(cd);
	
	// If there are not constructors at all - insert the default -
	// don't actually make any parse tree stuff - just generate
	// the code automatically in the code generation phase.
	if (cd.methodTable.get("<init>") == null && !cd.isInterface()) { 
	    Token t = new Token(sym.IDENTIFIER, cd.name(), 0, 0, 0);
	    Modifier m = new Modifier(Modifier.Public);
	    
	    ConstructorDecl c = new ConstructorDecl(new Sequence(m),
						    new Name(t),
						    new Sequence(),
						    null,
						    new Sequence());
	    addMethod(cd, c, "<init>", "");
	    cd.body().append(c);
	    println(cd.line + ":\tGenerating default construction <init>() for class '" + cd.name() + "'.");
	}
	
	return null;
    }
    

    /**
     * Visits a {@link ConstructorDecl}.
     * @param cd A {@link ConstructorDecl} object.
     * <ul>
     * <li>A constructor {\bf must} have the same name as the clas in which it lives (test file: CMF2.java).</li>
     * <li>Insert the constructor into the class' method table with the name &lt;init&gt;.</li>
     * </ul>
     * @return null
     */
    public Object visitConstructorDecl(ConstructorDecl cd) {
	println(cd.line + ":\tVisiting a ConstructorDecl.");
	//<--
	String methodName = cd.name().getname();
	String s = cd.paramSignature();
	
	if (!methodName.equals(currentClass.name())) 
	    //CMF2.java
	    Error.error(cd,"Constructor must be named the same as the class.");
	else {
	    println(cd.line + ":\tInserting constructor '<init>' with signature '" + s + 
		    "' into method table for class '" + 
		    currentClass.name() + "'.");
	    addMethod(currentClass, cd, "<init>", s);
	}
	//-->
	return null;
    }
    
    /** 
     * Visits a {@link FieldDecl}.  
     * @param fd A {@link FieldDecl} object.
     * <ul>
     * <li>Inserts the field into the field table of the class.</li>
     * <li>Sets the myDecl of the {@link Var} of fd.</li>
     * </ul>
     * @return null
     */
    public Object visitFieldDecl(FieldDecl fd) {
	println(fd.line + ":\tVisiting a FieldDecl.");
	//<--
	println(fd.line + ":\tInserting field '" + fd.name() + 
		"' into field table of class '" + currentClass.name() + "'.");
	// Set var's myDecl to point to this FieldDecl so we can type check its initializer later.
	fd.var().myDecl = fd;
	addField(currentClass, fd, fd.name());
	//-->
	return null;
    }
    
    /** 
     * Visits a {@link MethodDecl}.
     * @param md A {@link MethodDecl} object.
     * <ul>
     * <li> Insert the method into the the class' method table.</li>
     * </ul>
     * @return null
     */
    public Object visitMethodDecl(MethodDecl md) {
	println(md.line + ":\tVisiting a MethodDecl.");
	//<--
	String methodName = md.name().getname();
	String s = md.paramSignature();
	md.setMyClass(currentClass);
	
	println(md.line + ":\tInserting method '" + methodName + 
		"' with signature '" + s + "' into method table for class '" + 
		currentClass.name() + "'.");
	addMethod(currentClass, md, methodName, s);
	//-->	
	return null;
    }
    
    /** 
     * Visit a {@link StaticInitDecl}.
     * @param si A {@link StaticInitDecl} object.
     * <ul>
     * <li> Insert static initializer with name &lt;clinit&gt; into class' method table.</li>
     * </ul>
     * @return null
     */
    public Object visitStaticInitDecl(StaticInitDecl si) {
	println(si.line + ":\tVisiting a StaticInitDecl.");
	println(si.line + ":\tInserting <clinit> into method table for class '" + 
		currentClass.name() + "'.");
	
	addMethod(currentClass, si, "<clinit>", "");
	return null;
    }
}

