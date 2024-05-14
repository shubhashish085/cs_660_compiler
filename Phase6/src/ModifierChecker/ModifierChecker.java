package ModifierChecker;

import AST.*;
import Utilities.*;
import NameChecker.*;
import TypeChecker.*;
import Utilities.Error;
import java.util.*;

public class ModifierChecker extends Visitor {

	private SymbolTable classTable;
	private ClassDecl currentClass;
	private ClassBodyDecl currentContext;
	private boolean leftHandSide = false;
        

	public ModifierChecker(SymbolTable classTable, boolean debug) {
		this.classTable = classTable;
		this.debug = debug;
	}
    //<--
    /**
     * Implements the M algorithm from the Book. See Section XXX.
     * @param abstracts A hash set of the abstract methods.
     * @param concretes A hash set of the concrete methods.
     * @param cd The {@link ClassDecl ClassDecl} we are considering now.
     *
     * This method should traverse the class hierarchy exactly like
     * {@link #getMethod(String,ClassDecl) findMethod}. The string
     * associated with a method is as follows:<br><br>
     *
     * For example:<br><br>
     * void foo(int x, double d) { ... } we use the string "<tt>void bar( int double )</tt>"<br><br>
     *
     * To create this string we can use the Type.parseSignature() method on the return type as
     * well as on the paramaters and then glue it all together with the name and a set of ( ).    
     */

    public void M(HashSet<String> abstracts, HashSet<String> concretes, ClassDecl cd) {
	for (int i=0; i<cd.interfaces().nchildren; i++)
	    M(abstracts, concretes, ((ClassType)cd.interfaces().children[i]).myDecl);

	if (cd.superClass() != null)
	    M(abstracts, concretes, cd.superClass().myDecl);

	// Remove the concretes (these are from the super class only and will only ever
	// remove something that was added to the abstract set from interfaces of cd.
	Iterator<String> it = concretes.iterator();
        while (it.hasNext()) {
            String s = it.next();
            abstracts.remove(s);
        }

        // Remove the concretes from this class
	for (int i=0; i<cd.body().nchildren; i++) {
            if (cd.body().children[i] instanceof MethodDecl) {
                MethodDecl md = (MethodDecl)cd.body().children[i];
                if (!md.getModifiers().isAbstract()) {
                    concretes.add(Type.parseSignature(md.returnType().signature()) + " " +
				  md.name()+"("+Type.parseSignature(md.paramSignature()) + " )");
                    abstracts.remove(Type.parseSignature(md.returnType().signature()) + " " +
				     md.name()+"("+Type.parseSignature(md.paramSignature()) + " )");
                }
            }
        }

        // Add the abstract ones again
	for (int i=0; i<cd.body().nchildren; i++) {
            if (cd.body().children[i] instanceof MethodDecl) {
                MethodDecl md = (MethodDecl)cd.body().children[i];
                if (md.getModifiers().isAbstract() || md.block() == null) {
                    abstracts.add(Type.parseSignature(md.returnType().signature()) + " " +
				  md.name()+"("+Type.parseSignature(md.paramSignature()) + " )");
                    concretes.remove(Type.parseSignature(md.returnType().signature()) + " " +
				     md.name()+"("+Type.parseSignature(md.paramSignature()) + " )");
                }
            }
        }
    }
    //-->

    /** 
     * Uses the M algorithm from the Book to check that all abstract classes are 
     * implemented correctly in the class hierarchy. If not an error message is produced.
     *
     * @param cd A {@link ClassDecl ClassDecl} object.
     * @param methods The sequence of all methods of cd.
     */   
    public void checkImplementationOfAbstractClasses(ClassDecl cd) {
	//<--
	HashSet<String> con = new HashSet<String>();
	HashSet<String> abs = new HashSet<String>();
	M(abs, con, cd);
	if (abs.size() != 0) {
	    // NC3.java
	    Error.error(cd, "Class '" + cd.name() + "' is not abstract and does not override abstract methods:", false);
	    Iterator<String> it = abs.iterator();
	    while (it.hasNext()) {
		String s = it.next();
		Error.error(" " + s,false);
	    }
	    System.exit(1);
	}
	//-->
    }




    
	/** Assignment */
	public Object visitAssignment(Assignment as) {
	    println(as.line + ":\tVisiting an assignment (Operator: " + as.op()+ ").");

		boolean oldLeftHandSide = leftHandSide;

		leftHandSide = true;
		as.left().visit(this);

		// Added 06/28/2012 - no assigning to the 'length' field of an array type
		if (as.left() instanceof FieldRef) {
			FieldRef fr = (FieldRef)as.left();
			if (fr.target().type.isArrayType() && fr.fieldName().getname().equals("length"))
				Error.error(fr,"Cannot assign a value to final variable length.");
		}
		leftHandSide = oldLeftHandSide;
		as.right().visit(this);

		return null;
	}

	/** CInvocation */
	public Object visitCInvocation(CInvocation ci) {
	    println(ci.line + ":\tVisiting an explicit constructor invocation (" + (ci.superConstructorCall() ? "super" : "this") + ").");

		//<--
		// Check if the constructor invocation super(...) is private
		if (ci.superConstructorCall() && ci.constructor.getModifiers().isPrivate())
			Error.error(ci, "Constructor " + ci.targetClass.name() + "(" +
					Type.parseSignature(ci.constructor.paramSignature()) +
					" )  was declared 'private' in class '"	+ ci.targetClass.name() + "'.");

		super.visitCInvocation(ci);
		//-->

		return null;
	}

	/** ClassDecl */
	public Object visitClassDecl(ClassDecl cd) {
		println(cd.line + ":\tVisiting a class declaration for class '" + cd.name() + "'.");

		currentClass = cd;

		// If this class has not yet been declared public make it so.
		if (!cd.modifiers.isPublic())
			cd.modifiers.set(true, false, new Modifier(Modifier.Public));

		// If this is an interface declare it abstract!
		if (cd.isInterface() && !cd.modifiers.isAbstract())
			cd.modifiers.set(false, false, new Modifier(Modifier.Abstract));

		// If this class extends another class then make sure it wasn't declared
		// final.
		if (cd.superClass() != null)
			if (cd.superClass().myDecl.modifiers.isFinal())
				Error.error(cd, "Class '" + cd.name()
						+ "' cannot inherit from final class '"
						+ cd.superClass().typeName() + "'.");

		//<--
		super.visitClassDecl(cd);

		// If the class is not abstract and not an interface it must implement all
		// the abstract functions of its superclass(es) and its interfaces.
		if (!cd.isInterface() && !cd.modifiers.isAbstract()) {
		    checkImplementationOfAbstractClasses(cd);
		}
		
		//-->
	
		return null;
		

	}

	/** FieldDecl */
	public Object visitFieldDecl(FieldDecl fd) {
	    println(fd.line + ":\tVisiting a field declaration for field '" +fd.var().name() + "'.");

		// If field is not private and hasn't been declared public make it so.
		if (!fd.modifiers.isPrivate() && !fd.modifiers.isPublic())
			fd.modifiers.set(false, false, new Modifier(Modifier.Public));

		//<--
		// If the field is final is should have an initializer, else it can
		// never get a value!
		if (fd.modifiers.isFinal() && fd.var().init() == null && currentClass.isInterface())
		    Error.error(fd, "Field '" + fd.name() + "' in interface '" +
				currentClass.name() + "' must be initialized.");
		if (fd.modifiers.isFinal() && fd.var().init() == null && !currentClass.isInterface())
		    Error.error(fd, "Field '" + fd.name() + "' in class '" +
				currentClass.name() + "' must be initialized.");

		if (fd.modifiers.isAbstract())
			Error.error(fd, "Field '" + fd.name() + "' cannot be declared abstract.");

		if (fd.interfaceMember)
			if (fd.modifiers.isPrivate())
				Error.error(fd,	"Illegal use of 'private' modifier in interface.");

		currentContext = fd;
		super.visitFieldDecl(fd);
		currentContext = null;
		//-->

		return null;
	}

	/** FieldRef */
	public Object visitFieldRef(FieldRef fr) {
	    println(fr.line + ":\tVisiting a field reference '" + fr.fieldName() + "'.");

		//<--
		FieldDecl fd = fr.myDecl;
		// fields without targets were rewritten to either this.fieldName or <classname>.fieldName
		// this means that we might get errors like 'this cannot be refrenced from a static context'
		// which of course is not all that useful when we really want the field NAME.
		// Changed June 22 2012 ARRAY
		if (fd == null && !fr.rewritten) {  // can only happen if this is not really a field reference but a .length on an array.		
		    fr.target().visit(this);
		    return null;
		    
		}
		if (fd.modifiers.isPrivate() && ((FieldDecl)currentClass.fieldTable.get(fr.fieldName().getname())) != fd)
		    Error.error(fr, "field '" + fr.fieldName().getname() + "' was declared 'private' and cannot be accessed outside its class.");
		// because rewritten name exprs add a 'this' to non-static fields,
		// and this 'this' does not appear in the actual program code,
		// it is better to catch this error here and say "non-static field cannot be referenced."
		// rather than letting it get caught in the visit to This in fr.target().visit!
		if ((fr.target() instanceof This)
				&& fr.rewritten 
				&& currentContext.isStatic() 
				&& !fd.modifiers.isStatic())
			Error.error(fr, "non-static field '" + fr.fieldName().getname() + "' cannot be referenced from a static context.");
		
		// Class.field is only allowed if the field is static.
		if ((fr.target() instanceof NameExpr)
		    && (((NameExpr) fr.target()).myDecl instanceof ClassDecl)
		    && !(fd.modifiers.isStatic()))
		    Error.error(fr, "non-static field '" + fr.fieldName().getname() + "' cannot be referenced in a static context.");
		
		// If FieldRef is on a left hand side it better not be final.
		// experimental 3/21/2018 - it is OK to assign to a final field in a
		// constructor or a static initializer.
		if (leftHandSide && fd.modifiers.isFinal() &&
		    !((currentContext instanceof ConstructorDecl) ||
		      (currentContext instanceof StaticInitDecl))) {
		    Error.error(fr, "Cannot assign a value to final field '" + fr.fieldName().getname() + "'.");
		}
		// But only if it wasn't assigned a value at declaration time
		if (leftHandSide && fd.modifiers.isFinal() && fd.var().init() != null)
		    Error.error(fr, "Cannot assign a value to final field '" + fr.fieldName().getname() + "'.");
		
	//}
        boolean oldLeftHandSide = leftHandSide;
	// why do we do this ??
	if (fr.target() instanceof FieldRef) 
	    leftHandSide = false;
	if (!fr.rewritten)
  	  fr.target().visit(this);
	leftHandSide = oldLeftHandSide;
	//-->
	
	return null;
	}       

	/** MethodDecl */
    public Object visitMethodDecl(MethodDecl md) {
	println(md.line + ":\tVisiting a method declaration for method '" + md.name() + "'.");
	
	//<--
	currentContext = md;
	
	// Abstract methods cannot have a body!
	if (md.getModifiers().isAbstract() && md.block() != null)
	    Error.error(md, "Abstract method '" + md.getname() + "' cannot have a body.");
	// or be private
	if (md.getModifiers().isAbstract() && md.getModifiers().isPrivate())
	    Error.error(md, "Abstract method '" + md.getname() + "' cannot be declared private.");
	
	// Method has no body - i.e. it is abstract
	if (md.block() == null) {
	    if (currentClass.isClass() && !currentClass.modifiers.isAbstract())
		Error.error(md,	"Method '" + md.getname() + "' does not have a body, or class should be declared abstract.");
	    if (currentClass.isClass() && !md.getModifiers().isAbstract())
		Error.error(md, "Method '" + md.getname() + "' does not have a body, or should be declared abstract.");
	    if (currentClass.isInterface() && md.getModifiers().isFinal())
		Error.error(md, "Method '" + md.getname()+ "' cannot be declared final in an interface.");
	    if (md.getModifiers().isFinal())
		Error.error(md, "Abstract method '" + md.getname() + "' cannot be declared final.");
	}
	
	// If check if method reimplements a final version with same signature.
	if (currentClass.superClass() != null) {
	    MethodDecl mdecl = (MethodDecl) TypeChecker.findMethod(currentClass.superClass().myDecl.allMethods,
								   md.getname(), md.params(), true);
	    if (mdecl != null) {
		if (md.paramSignature().equals(mdecl.paramSignature())) {
		    // it is LEGAL to re-implement a private function final or not.
		    if (mdecl.getModifiers().isFinal() && !mdecl.getModifiers().isPrivate())
			Error.error(md, "Method '" + md.getname() + "' was implemented as final in super class, cannot be reimplemented.");
		    if (mdecl.getModifiers().isStatic() && !md.getModifiers().isStatic())
			Error.error(md, "Method '" + md.getname() + "' declared static in superclass, cannot be reimplemented non-static.");
		    if (!mdecl.getModifiers().isStatic() && md.getModifiers().isStatic())
			Error.error(md, "Method '" + md.getname() + "' declared non-static in superclass, cannot be reimplemented static.");
		    if (mdecl.getModifiers().isPublic() && md.getModifiers().isPrivate())
			Error.error(md, "Method '" + md.getname() + "' declared public in superclass, cannot be reimplemented as private.");
		    
		}
	    }
	}
       	
	if (md.isInterfaceMember() && md.getModifiers().isStatic())
	    Error.error(md,"Static method not allowed in interface");

	if (md.isInterfaceMember()) {
	    if (!md.getModifiers().isAbstract())
		md.getModifiers().set(false, false, new Modifier(Modifier.Abstract));
	}
	
	super.visitMethodDecl(md);
	currentContext = null;
	//-->
	
	return null;
    }
    
	/** Invocation */
	public Object visitInvocation(Invocation in) {
	    println(in.line + ":\tVisiting an invocation of method '" + in.methodName() + "'.");
	    
	    //<--
	    // 12/06/13 .length() for Strings
	    if (in.target() != null && in.methodName().getname().equals("length") && in.targetType.isStringType() && in.params().nchildren == 0) 
	    	return null; // there is nothing 
	    // 12/06/13 .charAt() for Strings
	    if (in.target() != null && in.methodName().getname().equals("charAt") && in.targetType.isStringType() && in.params().nchildren == 1 &&
		((Expression)in.params().children[0]).type.isIntegerType())
	    	return null; // there is nothing to do;
	    
	    boolean calleeMustBeStatic = false;
	    if (in.target() != null)
		in.target().visit(this);
	    
	    if ((in.target() instanceof NameExpr) &&
		(((NameExpr) in.target()).myDecl instanceof ClassDecl)) {
		// This invocation is of the form Class.method(...)
		calleeMustBeStatic = true;
	    }
	    
	    // Attempt to call local non-static method from static context.
	    if (in.target() == null && // local call: method(...)
		currentContext.isStatic() && // in a static context
		!in.targetMethod.getModifiers().isStatic()) // method(...) is not
		// static
		Error.error(in, "non-static method '" + in.methodName().getname()
			    + "' cannot be referenced from a static context.");
	    
	    // Attempt to call non-static method as a class method from a static
	    // context
	    // Class.method() (method non static, callee both static and non static)
	    if (calleeMustBeStatic && // Class.method(...)
		!in.targetMethod.getModifiers().isStatic()) // method(...) is not
		// static 
		Error.error(in, "non-static method '" + in.methodName().getname()
			    + "' cannot be referenced from a static context.");

	    // Attempt to call a private method in a different class
	    if (in.targetMethod.getModifiers().isPrivate()) {
		if (!in.targetMethod.getMyClass().equals(currentClass))
		    Error.error(in, "" + in.methodName().getname() + "(" + Type.parseSignature(in.targetMethod.paramSignature()) +
				" ) has private access in '" + in.targetMethod.getMyClass().className().getname() + "'.");
	    }
	    in.params().visit(this);
	    //-->
	    
	    return null;
	}
    

	public Object visitNameExpr(NameExpr ne) {
	    println(ne.line + ":\tVisiting a name expression '" + ne.name() + "'. (Nothing to do!)");
	    return null;
	}

	/** ConstructorDecl */
	public Object visitConstructorDecl(ConstructorDecl cd) {
	    println(cd.line + ":\tVisiting a constructor declaration for class '" + cd.name() + "'.");
	      
		//<--
	        
		currentContext = cd;
		// there is an explicit construtor invocation
		if (cd.cinvocation() != null) {
		    cd.cinvocation().visit(this);
		}
		cd.body().visit(this);

		currentContext = null;
		//-->

		return null;
	}

	/** New */
	public Object visitNew(New ne) {
	    println(ne.line + ":\tVisiting a new '" + ne.type().myDecl.name() + "'.");

		//<--
		// We cannot create a new object based on an abstract class
		if (ne.type().myDecl.modifiers.isAbstract())
			Error.error(ne, "Cannot instantiate abstract class '"
					+ ne.type().myDecl.name() + "'.");

		// private constructor can only be called from within the same class
		// as it is defined in.
		if (ne.getConstructorDecl().getModifiers().isPrivate()
				&& !currentClass.name().equals(ne.type().myDecl.name()))
			Error.error(ne, "" + ne.type().myDecl.name() + "(" + Type.parseSignature(ne.getConstructorDecl().paramSignature()) +
					" ) has private access in '" + ne.type().myDecl.name() + "'.");
		super.visitNew(ne);
		//-->

		return null;
	}

	/** StaticInit */
	public Object visitStaticInitDecl(StaticInitDecl si) {
		println(si.line + ":\tVisiting a static initializer.");

		//<--
		currentContext = si;
		super.visitStaticInitDecl(si);
		currentContext = null;
		//-->

		return null;
	}

	/** Super */
	public Object visitSuper(Super su) {
		println(su.line + ":\tVisiting a super.");

		if (currentContext.isStatic())
			Error.error(su,
					"non-static variable super cannot be referenced from a static context.");

		return null;
	}

	/** This */
	public Object visitThis(This th) {
		println(th.line + ":\tVisiting a this.");

		if (currentContext.isStatic())
			Error.error(th,	"non-static variable this cannot be referenced from a static context.");

		return null;
	}

	/** UnaryPostExpression */
    public Object visitUnaryPostExpr(UnaryPostExpr up) {
	println(up.line + ":\tVisiting a unary post expression with operator '" + up.op() + "'.");
	
	//<--
	if (up.expr() instanceof FieldRef && 
	    ((FieldRef)up.expr()).target().type.isArrayType() && 
	    ((FieldRef)up.expr()).fieldName().getname().equals("length")) {
	    Error.error(up,"cannot assign a value to final variable length.");
	    //if (((FieldDecl) fd).modifiers.isPrivate())
	    //if (!((ClassType) fr.targetType).myDecl.name().equals(currentClass.name()))
	    //    Error.error(up, "Cannot assign to a private field in a different class!");
	} else {
	    // even if x = 10; is legal for final fields, you cannot do x++ if the field is final.
	    if (up.expr() instanceof FieldRef &&
		((FieldRef)up.expr()).myDecl.modifiers.isFinal()) {
		Error.error(up, "Cannot assign a value to final field '" + ((FieldRef)up.expr()).fieldName().getname() + "'.");
	    }
	    
	    // we need this in case we have array references line a[1][2]++;
	    boolean oldLeftHandSide = leftHandSide;
	    leftHandSide = true;
	    up.expr().visit(this);
	    leftHandSide = oldLeftHandSide;
	}
	//-->
	return null;
    }
    
    /** UnaryPreExpr */
    public Object visitUnaryPreExpr(UnaryPreExpr up) {
	println(up.line + ":\tVisiting a unary pre expression with operator '" + up.op() + "'.");
	
	//<--
	if (up.expr() instanceof FieldRef && 
	    ((FieldRef)up.expr()).target().type.isArrayType() && 
	    ((FieldRef)up.expr()).fieldName().getname().equals("length") && 
	    (up.op().getKind() == PreOp.MINUSMINUS || up.op().getKind() == PreOp.PLUSPLUS)) {
	    Error.error(up,"cannot assign a value to final variable length.");
	} else {
	    if (up.expr() instanceof FieldRef &&
		((FieldRef)up.expr()).myDecl.modifiers.isFinal() &&
		(up.op().getKind() == PreOp.MINUSMINUS || up.op().getKind() == PreOp.PLUSPLUS)) {
                Error.error(up, "Cannot assign a value to final field '" + ((FieldRef)up.expr()).fieldName().getname() + "'.");
	    }
	    
	    // we need this in case we have array references line a[1][2]++;
	    boolean oldLeftHandSide = leftHandSide;
	    leftHandSide = (up.op().getKind() == PreOp.MINUSMINUS || up.op().getKind() == PreOp.PLUSPLUS);
	    up.expr().visit(this);
	    leftHandSide = oldLeftHandSide;
	}
	//-->
	return null;
    }
}
