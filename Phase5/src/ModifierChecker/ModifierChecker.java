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

	// YOUR CODE HERE
	public void M (HashMap<String, String> abs, HashMap<String, String> con, ClassDecl cd){

		Sequence interfaces = cd.interfaces();

		/*if(interfaces != null){
			for(int i = 0; i < interfaces.nchildren; i++){
				ClassType interfaceDeclType = (ClassType) (interfaces.children[i]);
				ClassDecl interfaceDecl = interfaceDeclType.myDecl;
				M(abs, con, interfaceDecl);
			}
		}

		if(cd.superClass() != null){
			ClassDecl superClass = cd.superClass().myDecl;
			M(abs, con, superClass);
		}

		for(String conKey : con.keySet()){
			if(abs.containsKey(conKey)){
				String absValue = abs.get(conKey);
				String conValue = con.get(conKey);
				if(absValue.equals(conValue)){
					abs.remove(conKey);
				}
			}
		}

		System.out.println("Class Name : " + cd.name());*/

		Sequence methods = cd.allMethods;

		if (methods != null) {
			for (int i = 0; i < methods.nchildren; i++) {
				MethodDecl method = (MethodDecl) (methods.children[i]);

				if (method.block() == null) {

					String absKey = method.name().getname();
					String absValue = method.paramSignature();
					if (absValue == null) {
						absValue = "";
					}
					abs.put(absKey, absValue);

				} else {
					String conKey = method.name().getname();
					String conValue = method.paramSignature();
					if (conValue == null) {
						conValue = "";
					}
					con.put(conKey, conValue);
				}
			}
		}

		for(String key: con.keySet()){
			if(abs.containsKey(key)){
				String param = abs.get(key);
				String conParam = con.get(key);
				if(param.equals(conParam)) {
					abs.remove(key);
				}
			}
		}


//		}else {
//
//			if (methods != null) {
//				for (int i = 0; i < methods.nchildren; i++) {
//					MethodDecl method = (MethodDecl) (methods.children[i]);
//					if (method.getModifiers().isAbstract()) {
//						System.out.println("Con :" + method.name());
//						continue;
//					}
//					String conKey = method.name().getname();
//					String conValue = method.paramSignature();
//					if (conValue == null) {
//						conValue = "";
//					}
//					con.put(conKey, conValue);
//					if (abs.containsKey(conKey)) {
//						String absValue = abs.get(conKey);
//						if (absValue.equals(conValue)) {
//							abs.remove(conKey);
//						}
//					}
//				}
//			}
//
//			if (methods != null) {
//				for (int i = 0; i < methods.nchildren; i++) {
//					MethodDecl method = (MethodDecl) (methods.children[i]);
//					if (!method.getModifiers().isAbstract()) {
//						System.out.println("abs :" + method.name());
//						continue;
//					}
//
//					String absKey = method.name().getname();
//					String absValue = method.paramSignature();
//					if (absValue == null) {
//						absValue = "";
//					}
//					abs.put(absKey, absValue);
//					if (con.containsKey(absKey)) {
//						String conValue = con.get(absKey);
//						if (absValue.equals(conValue)) {
//							con.remove(absKey);
//						}
//					}
//				}
//			}
//
//		}
		return;
	}



	/**
	 * Uses the M algorithm from the Book to check that all abstract classes are
	 * implemented correctly in the class hierarchy. If not an error message is produced.
	 *
	 * @param cd A {@link ClassDecl ClassDecl} object.
	 * @param methods The sequence of all methods of cd.
	 */
	public void checkImplementationOfAbstractClasses(ClassDecl cd) {
		// YOUR CODE HERE
		HashMap<String, String> absMap = new HashMap<>();
		HashMap<String, String> conMap = new HashMap<>();
		M(absMap, conMap, cd);


		if(absMap.keySet().size() > 0){
			String absMethods = "";
			for(String key: absMap.keySet()){
				absMethods += "\n" + key;
			}
			Error.error("Class '" + cd.name() + "' is not abstract and does not override abstract methods:" + absMethods);
		}
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
			FieldDecl fieldDecl = fr.myDecl;
			if(fieldDecl.modifiers.isFinal()){
				Error.error("Cannot assign a value to final field '" + fieldDecl.name() + "'");
			}
		}
		leftHandSide = oldLeftHandSide;
		as.right().visit(this);

		return null;
	}

	/** CInvocation */
	public Object visitCInvocation(CInvocation ci) {
		println(ci.line + ":\tVisiting an explicit constructor invocation (" + (ci.superConstructorCall() ? "super" : "this") + ").");

		// YOUR CODE HERE
		ci.visitChildren(this);

		ClassType classType = currentClass.superClass();

		if(ci.superConstructorCall()){
			ClassDecl superClassDecl = classType.myDecl;
			ConstructorDecl constructorDecl = (ConstructorDecl) TypeChecker.findMethod(superClassDecl.constructors, superClassDecl.name(), ci.args(), false);

			if(constructorDecl.getModifiers().isPrivate()){
				Error.error("'" + constructorDecl.getname() + "' can not access Super constructor");
			}
		}

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

		// YOUR CODE HERE
		if(cd.body() != null){
			cd.body().visit(this);
		}

		if(!cd.modifiers.isAbstract()){
			checkImplementationOfAbstractClasses(cd);
		}

		return null;


	}

	/** FieldDecl */
	public Object visitFieldDecl(FieldDecl fd) {
		println(fd.line + ":\tVisiting a field declaration for field '" +fd.var().name() + "'.");

		//fd.visit(this);

		// If field is not private and hasn't been declared public make it so.
		if (!fd.modifiers.isPrivate() && !fd.modifiers.isPublic())
			fd.modifiers.set(false, false, new Modifier(Modifier.Public));

		// YOUR CODE HERE
		if (fd.modifiers.isFinal() && fd.var().init() == null){
			Error.error("Field '" + fd.var().name() + "' in class '" + currentClass.name() + "' should be initialized");
		}

		if (fd.modifiers.isAbstract()){
			Error.error("Field '" + fd.var().name() + "' cannot be declared abstract.");
		}


		return null;
	}

	/** FieldRef */
	public Object visitFieldRef(FieldRef fr) {
		println(fr.line + ":\tVisiting a field reference '" + fr.fieldName() + "'.");

		// YOUR CODE HERE

		if(fr.target() != null) {
			fr.target().visit(this);
		}

		FieldDecl fieldDecl = fr.myDecl;

		if(fr.target() == null){

			if(!fieldDecl.isStatic() && currentContext.isStatic()){
				Error.error("non-static variable '" + fr.fieldName() + "' cannot be referenced from a static context.");
			}
		}else{
			Type targetType = fr.targetType;

			if(targetType instanceof ClassType){
				ClassDecl classDecl = ((ClassType) targetType).myDecl;
				if(fr.target().isClassName()){
					if(!fieldDecl.isStatic()){
						Error.error("non-static variable '" + fr.fieldName() + "' cannot be referenced from a static context.");
					}

					if(currentClass != classDecl && fieldDecl.modifiers.isPrivate()){
						Error.error("field '" + fr.fieldName() +"' was declared 'private' and cannot be accessed outside its class.");
					}

				}else{
					if(currentClass != classDecl){
						if(fieldDecl.modifiers.isPrivate()){
							Error.error("field '" + fr.fieldName() +"' was declared 'private' and cannot be accessed outside its class.");
						}
					}
				}
			}
		}
		return null;
	}

	/** MethodDecl */
	public Object visitMethodDecl(MethodDecl md) {
		println(md.line + ":\tVisiting a method declaration for method '" + md.name() + "'.");

		// YOUR CODE HERE
		currentContext = md;
		//md.modifiers().visit(this);


		if(currentClass.isInterface() && md.getModifiers().isFinal()){
			Error.error("Method '" + md.name() + "' cannot be declared final in an interface.");
		}

		if(currentClass.isInterface() && md.getModifiers().isStatic()){
			Error.error("Static method not allowed in interface");
		}

		if(md.block() == null && !currentClass.isInterface() && !md.getModifiers().isAbstract()){
			Error.error("Method '" + md.name().getname() + "' does not have a body, or should be declared abstract.");
		}

		if(md.block() != null && md.getModifiers().isAbstract()){
			Error.error("Abstract method '" + md.name().getname() + "' cannot have a body.");
		}

		if(!currentClass.modifiers.isAbstract() && md.getModifiers().isAbstract()){
			Error.error("Class '" + currentClass.name() + "' is not abstract and does not override abstract methods:\n" + md.name());
		}

		if(md.getModifiers().isAbstract() && md.getModifiers().isPrivate()){
			Error.error("Abstract method '" + md.name().getname() + "' cannot be declared private.");
		}

		ClassDecl superClass = null;

		if(currentClass.superClass() != null) {
			superClass = currentClass.superClass().myDecl;

			MethodDecl methodDecl = (MethodDecl) TypeChecker.findMethod(superClass.allMethods, md.name().getname(), md.params(), true);

			if(methodDecl != null && ((methodDecl.getModifiers().isPrivate() && md.getModifiers().isPrivate()) ||
					(!methodDecl.getModifiers().isPrivate() && !md.getModifiers().isPrivate()))) {
				if (methodDecl.getModifiers().isFinal()) {
					Error.error("Method '" + md.name() + "' was implemented as final in super class, cannot be reimplemented.");
				}

				if ((methodDecl.isStatic() && !md.isStatic())) {
					Error.error("Method '" + methodDecl.name() + "' declared static in superclass, cannot be reimplemented non-static.");
				}

				if(!methodDecl.isStatic() && md.isStatic()){
					Error.error("Method '" + methodDecl.name() + "' declared non-static in superclass, cannot be reimplemented static.");
				}

				if(methodDecl.getModifiers().isPrivate() && !md.getModifiers().isPrivate()){
					Error.error("Method '" + methodDecl.name() + "' declared private in superclass, cannot be reimplemented as public.");
				}

				if(!methodDecl.getModifiers().isPrivate() && md.getModifiers().isPrivate()){
					Error.error("Method '" + methodDecl.name() + "' declared public in superclass, cannot be reimplemented as private.");
				}

			}

		}

		if(md.block() != null){
			md.block().visit(this);
		}


		return null;
	}

	/** Invocation */
	public Object visitInvocation(Invocation in) {
		println(in.line + ":\tVisiting an invocation of method '" + in.methodName() + "'.");

		// YOUR CODE HERE
		if(in.target() != null) {
			in.target().visit(this);
		}
		MethodDecl methodDecl = in.targetMethod;

		if(in.target() == null){
			if(!methodDecl.isStatic() && currentContext.isStatic()){
				Error.error("non-static variable '" + methodDecl.name() + "' cannot be referenced from a static context.");
			}
		}else{
			Type targetType = in.targetType;

			if(targetType instanceof ClassType){
				ClassDecl classDecl = ((ClassType) targetType).myDecl;
				if(in.target().isClassName()){
					if(!methodDecl.isStatic()){
						Error.error("non-static variable '" + methodDecl.name() + "' cannot be referenced from a static context.");
					}

					if(currentClass != classDecl && methodDecl.getModifiers().isPrivate()){
						Error.error("'" + methodDecl.name() +"' has private access in '" + classDecl.name() + "'");
					}

				}else{
					if(currentClass != classDecl){
						if(methodDecl.getModifiers().isPrivate()){
							Error.error("'" + methodDecl.name() +"' has private access in '" + classDecl.name() + "'");
						}
					}
				}
			}
		}
		return null;
	}


	public Object visitNameExpr(NameExpr ne) {
		println(ne.line + ":\tVisiting a name expression '" + ne.name() + "'. (Nothing to do!)");
		return null;
	}

	/** ConstructorDecl */
	public Object visitConstructorDecl(ConstructorDecl cd) {
		println(cd.line + ":\tVisiting a constructor declaration for class '" + cd.name() + "'.");

		// YOUR CODE HERE
		currentContext = cd;
		/*if(cd.body() != null) {
			cd.body().visit(this);
		}*/
		cd.visitChildren(this);

		return null;
	}

	/** New */
	public Object visitNew(New ne) {
		println(ne.line + ":\tVisiting a new '" + ne.type().myDecl.name() + "'.");

		// YOUR CODE HERE
		//ne.visit(this);

		ClassType ct = ne.type();
		ClassDecl cd = ct.myDecl;

		if(ne.args() != null){
			ne.args().visit(this);
		}

		ConstructorDecl constructorDecl = (ConstructorDecl) TypeChecker.findMethod(cd.constructors, cd.name(), ne.args(), false);

		if(cd.modifiers != null){
			if(cd.modifiers.isAbstract()){
				Error.error("Can not instantiate the class '" + cd.name() + "'");
			}
		}

		if(constructorDecl.getModifiers() != null){
			if(currentClass != cd && constructorDecl.getModifiers().isPrivate()){
				Error.error("'" + constructorDecl.name() + "' has private access in '" +  cd.name() + "'");
			}
		}

		return null;
	}

	/** StaticInit */
	public Object visitStaticInitDecl(StaticInitDecl si) {
		println(si.line + ":\tVisiting a static initializer.");

		// YOUR CODE HERE
		currentContext = si;
		if(si.initializer() != null) {
			si.initializer().visit(this);
		}

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

		// YOUR CODE HERE
		//up.expr().visit(this);

		if(up.expr() instanceof FieldRef){
			FieldRef expr = (FieldRef) up.expr();
			if(expr.myDecl != null){
				FieldDecl fieldDecl = (FieldDecl) expr.myDecl;
				if(fieldDecl.modifiers.isFinal()){
					Error.error("Cannot assign a value to final field '" + fieldDecl.name() + "'.");
				}
			}

			if(expr.targetType instanceof ArrayType){
				if(expr.fieldName().getname().equals("length")){
					Error.error("cannot assign a value to final variable length.");
				}
			}

		}else if(up.expr() instanceof NameExpr){
			NameExpr expr = (NameExpr) up.expr();
			if(expr.myDecl instanceof FieldDecl){
				FieldDecl fieldDecl = (FieldDecl) expr.myDecl;
				if(fieldDecl.modifiers.isFinal()){
					Error.error("Cannot assign a value to final field '" + fieldDecl.name() + "'.");
				}
			}
		}

		return null;
	}

	/** UnaryPreExpr */
	public Object visitUnaryPreExpr(UnaryPreExpr up) {
		println(up.line + ":\tVisiting a unary pre expression with operator '" + up.op() + "'.");

		// YOUR CODE HERE
		//up.expr().visit(this);

		if(up.expr() instanceof FieldRef){
			FieldRef expr = (FieldRef) up.expr();
			if(expr.myDecl != null){
				FieldDecl fieldDecl = (FieldDecl) expr.myDecl;
				if(fieldDecl.modifiers.isFinal()){
					Error.error("Cannot assign a value to final field '" + fieldDecl.name() + "'.");
				}
			}

			if(expr.targetType instanceof ArrayType){
				if(expr.fieldName().getname().equals("length")){
					Error.error("cannot assign a value to final variable length.");
				}
			}
		}else if(up.expr() instanceof NameExpr){
			NameExpr expr = (NameExpr) up.expr();
			if(expr.myDecl instanceof FieldDecl){
				FieldDecl fieldDecl = (FieldDecl) expr.myDecl;
				if(fieldDecl.modifiers.isFinal()){
					Error.error("Cannot assign a value to final field '" + fieldDecl.name() + "'.");
				}
			}
		}

		return null;
	}
}
