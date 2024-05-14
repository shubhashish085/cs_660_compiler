package TypeChecker;

import AST.*;
import Parser.sym;
import Utilities.Error;
import Utilities.SymbolTable;
import Utilities.Visitor;
import java.util.*;
import java.math.*;

/**
 * TypeChecker implements the type checker for the Espresso langauge.
 */
public class TypeChecker extends Visitor {

	/**
	 * <p>Returns the 'best-fitting' method or constructor from a list
	 * of potential candidates given a set of actual parameters.</p>
	 * <p>See Section 7.2.9: Finding the Right Method to Call in Practical Compiler Construction.</p>
	 * <p>Remember to visit all the arguments BEFORE calling findMethod. findMethod uses the <i>.type</i> field of the parameters.</p>
	 * @param candidateMethods Sequence of methods or constructors. Use {@link AST.ClassDecl#allMethods} or {@link AST.ClassDecl#constructors} from the appropriate {@link AST.ClassDecl}.
	 * @param name The name of the method or constructor you are looking for.
	 * @param actualParams The sequence of actual parameters being passed to the method invocation or constructor invocation.
	 * @param lookingForMethods <i>true</i> if you pass a sequence of methods ({@link AST.ClassDecl#allMethods}), <i>false</i> if you pass a sequence of constructors ({@link AST.ClassDecl#constructors}).
	 * @return The {@link AST.MethodDecl}/{@link AST.ConstructorDecl} found; null if nothing was found.
	 */
	public static ClassBodyDecl findMethod(Sequence candidateMethods, String name, Sequence actualParams,
										   boolean lookingForMethods) {

		if (lookingForMethods) {
			println("+------------- findMethod (Method) ------------");
			println("| Looking for method: " + name);
		} else {
			println("+---------- findMethod (Constructor) ----------");
			println("| Looking for constructor: " + name);
		}
		println("| With parameters:");
		for (int i=0; i<actualParams.nchildren; i++){
			println("|   " + i + ". " + ((actualParams.children[i] instanceof ParamDecl)?(((ParamDecl)actualParams.children[i]).type()):((Expression)actualParams.children[i]).type));
		}
		// The number of actual parameters in the invocation.
		int count = 0;

		// Make an array big enough to hold all the methods if needed
		ClassBodyDecl cds[] = new ClassBodyDecl[candidateMethods.nchildren];

		// Initialize the array to point to null
		for(int i=0;i<candidateMethods.nchildren;i++)
			cds[i] = null;

		Sequence args = actualParams;
		Sequence params;

		// Insert all the methods from the symbol table that:
		// 1.) has the right number of parameters
		// 2.) each formal parameter can be assigned its corresponding
		//     actual parameter.
		if (lookingForMethods)
			println("| Finding methods with the right number of parameters and types");
		else
			println("| Finding constructors with the right number of parameters and types");
		for (int cnt=0; cnt<candidateMethods.nchildren; cnt++) {
			ClassBodyDecl cbd = (ClassBodyDecl)candidateMethods.children[cnt];

			// if the method doesn't have the right name, move on!
			if (!(cbd.getname().equals(name)))
				continue;

			// Fill params with the formal parameters.
			if (cbd instanceof ConstructorDecl)
				params = ((ConstructorDecl)cbd).params();
			else if (cbd instanceof MethodDecl)
				params = ((MethodDecl)cbd).params();
			else
				// we have a static initializer, don't do anything - just skip it.
				continue;

			print("|   " + name + "(");
			if (cbd instanceof ConstructorDecl)
				print(Type.parseSignature(((ConstructorDecl)cbd).paramSignature()));
			else
				print(Type.parseSignature(((MethodDecl)cbd).paramSignature()));
			print(" )  ");

			if (args.nchildren == params.nchildren) {
				// The have the same number of parameters
				// now check that the formal parameters are
				// assignmentcompatible with respect to the
				// types of the actual parameters.
				// OBS this assumes the type field of the actual
				// parameters has been set (in Expression.java),
				// so make sure to call visit on the parameters first.
				boolean candidate = true;

				for (int i=0;i<args.nchildren; i++) {
					candidate = candidate &&
							Type.assignmentCompatible(((ParamDecl)params.children[i]).type(),
									(args.children[i] instanceof Expression) ?
											((Expression)args.children[i]).type :
											((ParamDecl)args.children[i]).type());

					if (!candidate) {
						println(" discarded");
						break;
					}
				}
				if (candidate) {
					println(" kept");
					cds[count++] = cbd;
				}
			}
			else {
				println(" discarded");
			}

		}
		// now count == the number of candidates, and cds is the array with them.
		// if there is only one just return it!
		println("| " + count + " candidate(s) were found:");
		for ( int i=0;i<count;i++) {
			ClassBodyDecl cbd = cds[i];
			print("|   " + name + "(");
			if (cbd instanceof ConstructorDecl)
				print(Type.parseSignature(((ConstructorDecl)cbd).paramSignature()));
			else
				print(Type.parseSignature(((MethodDecl)cbd).paramSignature()));
			println(" )");
		}

		if (count == 0) {
			println("| No candidates were found.");
			println("+------------- End of findMethod --------------");
			return null;
		}

		if (count == 1) {
			println("| Only one candidate - thats the one we will call then ;-)");
			println("+------------- End of findMethod --------------");
			return cds[0];
		}
		println("| Oh no, more than one candidate, now we must eliminate some >:-}");
		// there were more than one candidate.
		ClassBodyDecl x,y;
		int noCandidates = count;

		for (int i=0; i<count; i++) {
			// take out a candidate
			x = cds[i];

			if (x == null)
				continue;
			cds[i] = null; // this way we won't find x in the next loop;

			// compare to all other candidates y. If any of these
			// are less specialised, i.e. all types of x are
			// assignment compatible with those of y, y can be removed.
			for (int j=0; j<count; j++) {
				y = cds[j];
				if (y == null)
					continue;

				boolean candidate = true;

				// Grab the parameters out of x and y
				Sequence xParams, yParams;
				if (x instanceof ConstructorDecl) {
					xParams = ((ConstructorDecl)x).params();
					yParams = ((ConstructorDecl)y).params();
				} else {
					xParams = ((MethodDecl)x).params();
					yParams = ((MethodDecl)y).params();
				}

				// now check is y[k] <: x[k] for all k. If it does remove y.
				// i.e. check if y[k] is a superclass of x[k] for all k.
				for (int k=0; k<xParams.nchildren; k++) {
					candidate = candidate &&
							Type.assignmentCompatible(((ParamDecl)yParams.children[k]).type(),
									((ParamDecl)xParams.children[k]).type());

					if (!candidate)
						break;
				}
				if (candidate) {
					// x is more specialized than y, so throw y away.
					print("|   " + name + "(");
					if (y instanceof ConstructorDecl)
						print(Type.parseSignature(((ConstructorDecl)y).paramSignature()));
					else
						print(Type.parseSignature(((MethodDecl)y).paramSignature()));
					print(" ) is less specialized than " + name + "(");
					if (x instanceof ConstructorDecl)
						print(Type.parseSignature(((ConstructorDecl)x).paramSignature()));
					else
						print(Type.parseSignature(((MethodDecl)x).paramSignature()));
					println(" ) and is thus thrown away!");

					cds[j] = null;
					noCandidates--;
				}
			}
			// now put x back in to cds
			cds[i] = x;
		}
		if (noCandidates != 1) {
			// illegal function call
			println("| There is more than one candidate left!");
			println("+------------- End of findMethod --------------");
			return null;
		}

		// just find it and return it.
		println("| We were left with exactly one candidate to call!");
		println("+------------- End of findMethod --------------");
		for (int i=0; i<count; i++)
			if (cds[i] != null)
				return cds[i];

		return null;
	}

	/**
	 * Given a list of candiate methods and a name of the method this method prints them all out.
	 *
	 * @param cd The {@link AST.ClassDecl} for which the methods or constructors are being listed.
	 * @param candidateMethods A {@link AST.Sequence} of either {@link AST.MethodDecl}s ({@link AST.ClassDecl#allMethods}) or {@link AST.ConstructorDecl}s ({@link AST.ClassDecl#constructors}).
	 * @param name The name of the method or the constructor for which the candidate list should be produced.
	 */
	public void listCandidates(ClassDecl cd, Sequence candidateMethods, String name) {

		for (int cnt=0; cnt<candidateMethods.nchildren; cnt++) {
			ClassBodyDecl cbd = (ClassBodyDecl)(candidateMethods.children[cnt]);

			if (cbd.getname().equals(name)) {
				if (cbd instanceof MethodDecl)
					System.out.println("  " + name + "(" + Type.parseSignature(((MethodDecl)cbd).paramSignature()) + " )");
				else
					System.out.println("  " + cd.name() + "(" + Type.parseSignature(((ConstructorDecl)cbd).paramSignature()) + " )");
			}
		}
	}
	/**
	 * The global class tabel. This should be set in the constructor.
	 */
	private SymbolTable   classTable;
	/**
	 * The class of which children are currently being visited. This should be updated when visiting a {@link AST.ClassDecl}.
	 */
	private ClassDecl     currentClass;
	/**
	 * The particular {@link AST.ClassBodyDecl} (except {@link AST.FieldDecl}) of which children are currently being visited.
	 */
	private ClassBodyDecl currentContext;
	/**
	 * The current {@link AST.FieldDecl} of which children are currently being visited (if applicable).
	 */
	private FieldDecl     currentFieldDecl;
	/**
	 * Indicates if children being visited are part of a {@link AST.FieldDecl} initializer. (accessible though {@link AST.FieldDecl#var()}). Used for determining forward reference of a non-initialized field. You probably don't want to bother with this one.
	 */
	private boolean       inFieldInit;

	/**
	 * Constructs a new type checker.
	 * @pisaram classTable The global class table.
	 * @param debug determins if debug information should printed out.
	 */
	public TypeChecker(SymbolTable classTable, boolean debug) {
		this.classTable = classTable;
		this.debug = debug;
	}

	/** v
	 * @param ae An {@link AST.ArrayAccessExpr} parse tree node.
	 * @return Returns the type of the array access expression.
	 */
	public Object visitArrayAccessExpr(ArrayAccessExpr ae) {
		println(ae.line + ":\tVisiting ArrayAccessExpr.");
		// YOUR CODE HERE
		Type targetType = (Type) ae.target().visit(this);
		Type indexType = (Type) ae.index().visit(this);

		if(!targetType.isArrayType()){
			Error.error("Target of Array Access Expresion should be of Array Type");
		}
		if(!indexType.isIntegralType()){
			Error.error("Index of Array Access Expresion should be of Integral Type");
		}

		ArrayType targetArrayType = (ArrayType) targetType;

		if(targetArrayType.getDepth() == 0){
			ae.type = targetArrayType.baseType();
		}else{
			ae.type = new ArrayType(targetArrayType.baseType(), targetArrayType.getDepth() - 1);
		}

		return ae.type;
	}

	/**
	 * @param ae An {@link AST.ArrayType} parse tree node.
	 * @return Returns itself.
	 */
	public Object visitArrayType(ArrayType at) {
		println(at.line + ":\tVisiting an ArrayType.");
		println(at.line + ":\tArrayType type is " + at);
		// An ArrayType is already a type, so nothing to do.
		return at;
	}

	/** NewArray */
	public Object visitNewArray(NewArray ne) {

		println(ne.line + ":\tVisiting a NewArray.");
		// YOUR CODE HERE


		if(ne.dims() != null && ne.dims().nchildren > 0) {
			ne.type = new ArrayType(ne.baseType(), ne.dims().nchildren);
		}else if (ne.dimsExpr() != null && ne.dimsExpr().nchildren > 0){
			ne.type = new ArrayType(ne.baseType(), ne.dimsExpr().nchildren);
		}

		if(ne.init() != null){
			((ArrayLiteral)ne.init()).visit(this);
		}

		println(ne.line + ":\tNewArray type is: " + ne.type);
		return ne.type;
	}

	// arrayAssignmentCompatible: Determines if the expression 'e' can be assigned to the type 't'.
	// See Section 7.2.6 sub-heading 'Constructed Types'
	public boolean arrayAssignmentCompatible(Type t, Expression e) {
		if (t instanceof ArrayType && (e instanceof ArrayLiteral)) {
			ArrayType at = (ArrayType)t;
			e.type = at; //  we don't know that this is the type - but if we make it through it will be!
			ArrayLiteral al = (ArrayLiteral)e;

			// t is an array type i.e. XXXXXX[ ]
			// e is an array literal, i.e., { }
			if (al.elements().nchildren == 0) // the array literal is { }
				return true;   // any array variable can hold an empty array
			// Now check that XXXXXX can hold value of the elements of al
			// we have to make a new type: either the base type if |dims| = 1
			boolean b = true;
			for (int i=0; i<al.elements().nchildren; i++) {
				if (at.getDepth() == 1)
					b = b && arrayAssignmentCompatible(at.baseType(), (Expression)al.elements().children[i]);
				else {
					ArrayType at1 = new ArrayType(at.baseType(), at.getDepth()-1);
					b = b  && arrayAssignmentCompatible(at1, (Expression)al.elements().children[i]);
				}
			}
			return b;
		} else if (t instanceof ArrayType && !(e instanceof ArrayLiteral)) {
			Type t1 = (Type)e.visit(this);
			if (t1 instanceof ArrayType)
				if (!Type.assignmentCompatible(t,t1))
					Error.error("Incompatible type in array assignment.");
				else
					return true;
			Error.error(t, "Error:\tcannot assign non array to array type '" + t.typeName() + "'.");
		}
		else if (!(t instanceof ArrayType) && (e instanceof ArrayLiteral)) {
			Error.error(t, "Error:\tcannot assign value '" + ((ArrayLiteral)e).toString() + "' to type '" + t.typeName() + "'.");
		}
		return Type.assignmentCompatible(t,(Type)e.visit(this));
	}

	public Object visitArrayLiteral(ArrayLiteral al) {
		// Espresso does not allow array literals without the 'new <type>' part.
		//Error.error(al, "Array literal must be preceeded by a 'new <type>'.");
		Sequence sequence = al.elements();
		for(int i = 0; i < sequence.nchildren; i++){
			Type type = (Type)sequence.children[i].visit(this);
		}

		return null;
	}

	/** ASSIGNMENT */
	public Object visitAssignment(Assignment as) {
		println(as.line + ":\tVisiting an Assignment.");

		// get the types of the LHS (v) and the RHS(e)
		Type vType = (Type) as.left().visit(this);
		Type eType = (Type) as.right().visit(this);

		/** Note: as.left() should be of NameExpr or FieldRef class! */

		if (!vType.assignable())
			Error.error(as,"Left-hand side of assignment not assignable.");

		// Cannot assign to a classname
		if (as.left() instanceof NameExpr && (((NameExpr)as.left()).myDecl instanceof ClassDecl))
			Error.error(as,"Left-hand side of assignment not assignable.");

		// Now switch on the operator
		switch (as.op().kind) {
			case AssignmentOp.EQ :{
				// Check if the right hand side is a constant.
				// if we don't do this the following is illegal: byte b; b = 4; because 4 is an int!
				if (as.right().isConstant()) {
					if (vType.isShortType() && Literal.isShortValue(((BigDecimal)as.right().constantValue()).longValue()))
						break;
					if (vType.isByteType() && Literal.isByteValue(((BigDecimal)as.right().constantValue()).longValue()))
						break;
					if (vType.isCharType() && Literal.isCharValue(((BigDecimal)as.right().constantValue()).longValue()))
						break;
				}

				// Now just check for assignment compatability
				if (!Type.assignmentCompatible(vType,eType))
					Error.error(as,"Cannot assign value of type " + eType.typeName() + " to variable of type " + vType.typeName() + ".");
				break;
			}

			// YOUR CODE HERE
			case AssignmentOp.PLUSEQ:{

				if((!vType.isNumericType() ||  !eType.isNumericType()) && !vType.isStringType()){
					Error.error("Left-hand and Right-hand side should be of numeric type or String type");
				}

				if (!vType.isStringType() && !Type.assignmentCompatible(vType,eType))
					Error.error(as,"Cannot assign value of type " + eType.typeName() + " to variable of type " + vType.typeName() + ".");
				break;
			}
			case AssignmentOp.MINUSEQ:{
				if(!vType.isNumericType() || !eType.isNumericType()){
					Error.error("Left-hand and Right-hand side should should be of numeric type");
				}

				if (!Type.assignmentCompatible(vType,eType))
					Error.error(as,"Cannot assign value of type " + eType.typeName() + " to variable of type " + vType.typeName() + ".");
				break;
			}

			case AssignmentOp.DIVEQ:{
				if(!vType.isNumericType() || !eType.isNumericType()){
					Error.error("Left-hand and Right-hand side should should be of numeric type");
				}

				if (!Type.assignmentCompatible(vType,eType))
					Error.error(as,"Cannot assign value of type " + eType.typeName() + " to variable of type " + vType.typeName() + ".");
				break;
			}
			case AssignmentOp.MULTEQ:{
				if(!vType.isNumericType() || !eType.isNumericType()){
					Error.error("Left-hand and Right-hand side should should be of numeric type");
				}

				if (!Type.assignmentCompatible(vType,eType))
					Error.error(as,"Cannot assign value of type " + eType.typeName() + " to variable of type " + vType.typeName() + ".");
				break;
			}
			case AssignmentOp.MODEQ:{
				if(!vType.isNumericType() || !eType.isNumericType()){
					Error.error("Left-hand and Right-hand side should be of numeric type");
				}

				if (!Type.assignmentCompatible(vType,eType))
					Error.error(as,"Cannot assign value of type " + eType.typeName() + " to variable of type " + vType.typeName() + ".");
				break;
			}
			case AssignmentOp.LSHIFTEQ:{
				if(!vType.isIntegralType()){
					Error.error("Left-hand side operand of operator '<<=' must be of integral type.");
				}
				if(!eType.isIntegralType()){
					Error.error("Right-hand side operand of operator '<<=' must be of integral type.");
				}

				/*if (!Type.assignmentCompatible(vType,eType))
					Error.error(as,"Cannot assign value of type " + eType.typeName() + " to variable of type " + vType.typeName() + ".");*/
				break;
			}
			case AssignmentOp.RSHIFTEQ:{
				if(!vType.isIntegralType()){
					Error.error("Left-hand side operand of operator '>>=' must be of integral type.");
				}

				if(!eType.isIntegralType()){
					Error.error("Right-hand side operand of operator '>>=' must be of integral type.");
				}

				/*if (!Type.assignmentCompatible(vType,eType))
					Error.error(as,"Cannot assign value of type " + eType.typeName() + " to variable of type " + vType.typeName() + ".");*/
				break;
			}
			case AssignmentOp.RRSHIFTEQ:{
				if(!vType.isIntegralType()){
					Error.error("Left-hand side operand of operator '>>>=' must be of integral type.");
				}

				if(!eType.isIntegralType()){
					Error.error("Right-hand side operand of operator '>>>=' must be of integral type.");
				}

				/*if (!Type.assignmentCompatible(vType,eType))
					Error.error(as,"Cannot assign value of type " + eType.typeName() + " to variable of type " + vType.typeName() + ".");*/
				break;
			}
			case AssignmentOp.ANDEQ:{

				if((!vType.isBooleanType()|| !eType.isBooleanType()) && (!vType.isIntegralType() || !eType.isIntegralType())){
					Error.error("Both right and left-hand side operands of operator '&=' must be either of boolean or similar integral type.");
				}

				if(vType.isIntegralType() && eType.isIntegralType() && !(vType.getTypePrefix().equals(eType.getTypePrefix()))){
					Error.error("Both right and left-hand side operands of operator '&=' must be either of boolean or similar integral type.");
				}

				/*if (!Type.assignmentCompatible(vType,eType))
					Error.error(as,"Cannot assign value of type " + eType.typeName() + " to variable of type " + vType.typeName() + ".");*/
				break;
			}

			case  AssignmentOp.OREQ:{

				if((!vType.isBooleanType()|| !eType.isBooleanType()) && (!vType.isIntegralType() || !eType.isIntegralType())){
					Error.error("Both right and left-hand side operands of operator '|=' must be either of boolean or similar integral type.");
				}

				if(vType.isIntegralType() && eType.isIntegralType() && !(vType.getTypePrefix().equals(eType.getTypePrefix()))){
					Error.error("Both right and left-hand side operands of operator '&=' must be either of boolean or similar integral type.");
				}

				/*if (!Type.assignmentCompatible(vType,eType))
					Error.error(as,"Cannot assign value of type " + eType.typeName() + " to variable of type " + vType.typeName() + ".");*/
				break;
			}
			case  AssignmentOp.XOREQ:{

				if((!vType.isBooleanType()|| !eType.isBooleanType()) && (!vType.isIntegralType() || !eType.isIntegralType())){
					Error.error("Left-hand and Right-hand side should be of Integral type/ Boolean Type");
				}

				if(vType.isIntegralType() && eType.isIntegralType() && !(vType.getTypePrefix().equals(eType.getTypePrefix()))){
					Error.error("Both right and left-hand side operands of operator '&=' must be either of boolean or similar integral type.");
				}

				/*if (!Type.assignmentCompatible(vType,eType))
					Error.error(as,"Cannot assign value of type " + eType.typeName() + " to variable of type " + vType.typeName() + ".");*/
				break;
			}

		}
		// The overall type is always that of the LHS.
		as.type = vType;
		println(as.line + ":\tAssignment has type: " + as.type);

		return vType;
	}

	/** BINARY EXPRESSION */
	public Object visitBinaryExpr(BinaryExpr be) {
		println(be.line + ":\tVisiting a Binary Expression.");

		// YOUR CODE HERE

		Type lType = (Type) be.left().visit(this);
		Type rType = (Type) be.right().visit(this);


		if(be.op().kind == BinOp.INSTANCEOF){

			if(lType.isVoidType() || rType.isVoidType()){
				Error.error("Void type cannot be used here.");
			}

			if(!(be.right() instanceof NameExpr && ((NameExpr)be.right()).myDecl != null && ((NameExpr)be.right()).myDecl instanceof ClassDecl)){
				Error.error("'" + be.right().type.typeName() + "' is not a class name");
			}

			if(be.left().isClassName()){
				Error.error("Left-hand side of instanceof cannot be a class.");
			}

			if(!lType.isClassType()){
				Error.error("Left-hand side of instanceof needs expression of class type.");
			}

			be.type = new PrimitiveType(PrimitiveType.BooleanKind);
			println(be.line + ":\tBinary Expression has type: " + be.type);
			return be.type;
		}



		if(be.op().kind == BinOp.GT || be.op().kind == BinOp.LT || be.op().kind == BinOp.GTEQ || be.op().kind == BinOp.LTEQ){

			if(lType.isVoidType() || rType.isVoidType()){
				Error.error("Void type cannot be used here.");
			}

			if(! lType.isNumericType() || !rType.isNumericType()){
				if(be.op().kind == BinOp.GT) {
					Error.error("Operator '>' requires operands of numeric type.");
				}
				if(be.op().kind == BinOp.LT) {
					Error.error("Operator '<' requires operands of numeric type.");
				}
				if(be.op().kind == BinOp.GTEQ) {
					Error.error("Operator '>=' requires operands of numeric type.");
				}
				if(be.op().kind == BinOp.LTEQ) {
					Error.error("Operator '<=' requires operands of numeric type.");
				}
			}

			be.type = new PrimitiveType(PrimitiveType.BooleanKind);
		}else if(be.op().kind == BinOp.EQEQ || be.op().kind == BinOp.NOTEQ){

			if( !(lType.isNumericType() && rType.isNumericType())
					&& !lType.identical(rType)){
				if(be.op().kind == BinOp.EQEQ) {
					Error.error("Operator '==' requires operands of same type.");
				}if(be.op().kind == BinOp.NOTEQ) {
					Error.error("Operator '!=' requires operands of same type.");
				}

			}

			if(lType.isVoidType() || rType.isVoidType()){
				Error.error("Void type cannot be used here.");
			}

			if((be.left() instanceof NameExpr && ((NameExpr) be.left()).myDecl != null && ((NameExpr) be.left()).myDecl instanceof ClassDecl) ||
					(be.right() instanceof NameExpr && ((NameExpr) be.right()).myDecl != null && ((NameExpr) be.right()).myDecl instanceof ClassDecl)){
				if(be.op().kind == BinOp.EQEQ){
					Error.error("Class name '" + be.left().type.typeName() + "' cannot appear as parameter to operator '=='.");
				}else if(be.op().kind == BinOp.NOTEQ){
					Error.error("Class name '" + be.right().type.typeName() + "' cannot appear as parameter to operator '!='.");
				}
			}


			be.type = new PrimitiveType(PrimitiveType.BooleanKind);
		}else if(be.op().kind == BinOp.ANDAND || be.op().kind == BinOp.OROR){
			if(!lType.isBooleanType() || !rType.isBooleanType()){
				if(be.op().kind == BinOp.ANDAND){
					Error.error("Operator '&&' requires operands of boolean type.");
				}else if(be.op().kind == BinOp.OROR){
					Error.error("Operator '||' requires operands of boolean type.");
				}
			}

			if(lType.isVoidType() || rType.isVoidType()){
				Error.error("Void type cannot be used here.");
			}
			be.type = new PrimitiveType(PrimitiveType.BooleanKind);
		}else if(be.op().kind == BinOp.OR || be.op().kind == BinOp.AND || be.op().kind == BinOp.XOR){

			if(lType.isVoidType() || rType.isVoidType()){
				Error.error("Void type cannot be used here.");
			}

			if(lType.isBooleanType() && rType.isBooleanType()){
				be.type = new PrimitiveType(PrimitiveType.BooleanKind);
			}else if (lType.isIntegralType() && rType.isIntegralType()){
				be.type = new PrimitiveType(PrimitiveType.ceiling((PrimitiveType) lType, (PrimitiveType) rType));
			}else{
				if(be.op().kind == BinOp.OR) {
					Error.error("Operator '|' requires operands of boolean or integral type.");
				}
				if(be.op().kind == BinOp.AND) {
					Error.error("Operator '&' requires operands of boolean or integral type.");
				}
				if(be.op().kind == BinOp.XOR) {
					Error.error("Operator '^' requires operands of boolean or integral type.");
				}
			}
		}else if(be.op().kind == BinOp.MINUS || be.op().kind == BinOp.MULT || be.op().kind == BinOp.DIV || be.op().kind == BinOp.MOD){

			if(lType.isVoidType() || rType.isVoidType()){
				Error.error("Void type cannot be used here.");
			}

			if(!lType.isNumericType() || !rType.isNumericType()){
				if(be.op().kind == BinOp.MINUS) {
					Error.error("Operator '-'requires operands of numeric type.");
				}
				if(be.op().kind == BinOp.MULT) {
					Error.error("Operator '*'requires operands of numeric type.");
				}
				if(be.op().kind == BinOp.DIV) {
					Error.error("Operator '/' requires operands of numeric type.");
				}
				if(be.op().kind == BinOp.MOD) {
					Error.error("Operator '%'requires operands of numeric type.");
				}

			}
			be.type = new PrimitiveType(PrimitiveType.ceiling((PrimitiveType) lType, (PrimitiveType) rType));
		}else if(be.op().kind == BinOp.PLUS){

			if(lType.isVoidType() || rType.isVoidType()){
				Error.error("Void type cannot be used here.");
			}

			if(lType.isStringType() || rType.isStringType()){
				be.type = new PrimitiveType(PrimitiveType.StringKind);
			}else if((lType.isNumericType() && rType.isNumericType())){
				be.type = new PrimitiveType(PrimitiveType.ceiling((PrimitiveType) lType, (PrimitiveType) rType));
			}else{
				Error.error("Operator '+' requires operands of numeric type.");
			}
		}else if(be.op().kind == BinOp.LSHIFT || be.op().kind == BinOp.RSHIFT || be.op().kind == BinOp.RRSHIFT){

			if(lType.isVoidType() || rType.isVoidType()){
				Error.error("Void type cannot be used here.");
			}

			if(!lType.isIntegralType() || !rType.isIntegralType()){
				if(be.op().kind == BinOp.LSHIFT) {
					Error.error("Operator '<<' requires operands of integral type.");
				}
				if(be.op().kind == BinOp.RSHIFT) {
					Error.error("Operator '>>' requires operands of integral type.");
				}
				if(be.op().kind == BinOp.RRSHIFT) {
					Error.error("Operator '>>>' requires operands of integral type.");
				}
			}
			be.type = lType;
			if(lType.isByteType() || lType.isCharType() || lType.isShortType()){
				be.type = new PrimitiveType(PrimitiveType.IntKind);
			}
		}

		println(be.line + ":\tBinary Expression has type: " + be.type);
		return be.type;
	}

	/** CAST EXPRESSION */
	public Object visitCastExpr(CastExpr ce) {
		println(ce.line + ":\tVisiting a cast expression.");

		// We have two different types of casts:
		// Numeric: any numeric type can be cast to any other numeric type.
		// Class: (A)e, where Type(e) is B. This is legal ONLY if A :> B or B :> A
		//        that is either A has to be above B in the class hierarchy or
		//        B has to be above A.
		// Do note that if the cast type and the expression type are identical,
		// then the cast is fine: for example (String)"Hello" or (Boolean)true.
		//
		// One small caveat: (A)a is not legal is 'a' is a class name. So if the
		// expression is a NameExpr then it cannot be the name of a class - that is,
		// its myDecl cannot be a ClassDecl.
		// YOUR CODE HERE
		Type castType = ce.type();
		Type exprType = (Type) ce.expr().visit(this);

		if(ce.expr() instanceof NameExpr && (((NameExpr)ce.expr()).myDecl instanceof ClassDecl)){
			Error.error("Cannot use class name '" + ce.expr().type.typeName() + "'. Object name expected in cast.");
		}

		if(exprType.isClassType() && castType.isNumericType()){
			Error.error("Illegal type cast. Cannot cast type '" + exprType.typeName() +"' to type '" + castType.typeName() +"'.");
		}

		if(castType.isClassType() && exprType.isClassType()) {
			if (!(Type.isSuper((ClassType) castType, (ClassType) exprType) || Type.isSuper((ClassType) exprType, (ClassType) castType))) {
				Error.error("Illegal type cast.");
			}
			ce.type = ce.type();
		}

		if(castType.isNumericType() && exprType.isNumericType()){
			ce.type = ce.type();
		}

		// The overall type of a cast expression is always the cast type.

		println(ce.line + ":\tCast Expression has type: " + ce.type);
		return ce.type;
	}

	/** CLASSTYPE */
	public Object visitClassType(ClassType ct) {
		println(ct.line + ":\tVisiting a ClassType.");
		// A class type is alreayd a type, so nothing to do.
		println(ct.line + ":\tClassType has type: " + ct);
		return ct;
	}

	/** CONSTRUCTOR (EXPLICIT) INVOCATION */
	public Object visitCInvocation(CInvocation ci) {
		println(ci.line + ":\tVisiting an explicit constructor invocation.");

		// An explicit constructor invocation takes one of two forms:
		// this ( ... )  -- this calls a constructor in the same class (currentClass)
		// super ( ... ) -- this calls a constructor in the super class (of currentClass)

		// YOUR CODE HERE
		ClassDecl targetClass = null;

		if(ci.thisConstructorCall()){
			targetClass = currentClass;
		}else if(ci.superConstructorCall()){
			targetClass = currentClass.superClass().myDecl;
		}

		if(ci.args() != null) {
			ci.args().visitChildren(this);
		}
		ClassBodyDecl constructor = findMethod(targetClass.constructors, targetClass.name(), ci.args(), false);

		if(constructor == null){
			Error.error("No Constructor found");
		}

		if(currentContext instanceof ConstructorDecl && constructor == (ConstructorDecl) currentContext){
			Error.error("Recursive constructor invocation of constructor ");
		}

		return null;
	}

	/** CLASS DECLARATION */
	public Object visitClassDecl(ClassDecl cd) {
		println(cd.line + ":\tVisiting a ClassDecl(" + cd.name() + ")");

		// The only check to do here is that we cannot have repreated interface implementations.
		// E.g.: class A implements I, I { ... } is illegal.

		Sequence interfaces = cd.interfaces();

		Map<String, String> interfaceNameMap = new HashMap<>();

		if(interfaces != null){
			for(int i = 0; i < interfaces.nchildren; i++){
				ClassType classType = (ClassType) interfaces.children[i];
				if(interfaceNameMap.containsKey(classType.typeName())){
					Error.error("Repeated interface '" + classType.typeName() + "'.");
				}

				interfaceNameMap.put(classType.typeName(), "");
			}
		}


		// Update the current class.
		currentClass = cd;
		// YOUR CODE HERE
		cd.visitChildren(this);

		return null;
	}

	/** CONSTRUCTOR DECLARATION */
	public Object visitConstructorDecl(ConstructorDecl cd) {
		println(cd.line + ":\tVisiting a ConstructorDecl.");

		// Update the current context
		currentContext = cd;

		// YOUR CODE HERE
		cd.visitChildren(this);

		return null;
	}

	/** DO STATEMENT */
	public Object visitDoStat(DoStat ds) {
		println(ds.line + ":\tVisiting a DoStat.");

		// YOUR CODE HERE

		if(ds.stat() != null){
			ds.stat().visitChildren(this);
		}

		Type expressionType = (Type) ds.expr().visit(this);

		if(ds.expr().type == null || !ds.expr().type.isBooleanType()){
			Error.error("Non boolean Expression found as test in do-statement.");
		}

		return null;
	}

	/** FIELD DECLARATION */
	public Object visitFieldDecl(FieldDecl fd) {
		println(fd.line + ":\tVisiting a FieldDecl.");

		// Update the current context
		currentContext = fd;
		// set inFieldInit to true as we are about to visit the field initializer.
		// (happens from visitVar() if it isn't null.
		inFieldInit = true;
		// Set the current field.
		currentFieldDecl = fd;
		// Visit the var
		if (fd.var().init() != null)
			fd.var().visit(this);
		// Set current field back to null (fields cannot be nested, so this is OK)
		currentFieldDecl = null;
		// set ifFieldInit back to false as we are done with the initializer.
		inFieldInit = false;

		return fd.type();
	}

	/** FIELD REFERENCE */
	public Object visitFieldRef(FieldRef fr) {
		println(fr.line + ":\tVisiting a FieldRef.");

		Type targetType = (Type) fr.target().visit(this);
		String field    = fr.fieldName().getname();
		// Changed June 22 2012 ARRAY
		if (fr.fieldName().getname().equals("length")) {
			if (targetType.isArrayType()) {
				fr.type = new PrimitiveType(PrimitiveType.IntKind);
				println(fr.line + ":\tField Reference was a an Array.length reference, and it has type: " + fr.type);
				fr.targetType = targetType;
				return fr.type;
			}
		}

		if (targetType.isClassType()) {
			ClassType c = (ClassType)targetType;
			ClassDecl cd = c.myDecl;
			fr.targetType = targetType;

			println(fr.line + ":\tLooking up symbol '" + field + "' in fieldTable of class '" +
					c.typeName() + "'.");

			// Lookup field in the field table of the class associated with the target.
			FieldDecl lookup = (FieldDecl) NameChecker.NameChecker.getField(field, cd);

			// Field not found in class.
			if (lookup == null)
				Error.error(fr,"Field '" + field + "' not found in class '" + cd.name() + "'.");
			else {
				fr.myDecl = lookup;
				fr.type = lookup.type();
			}
		} else
			Error.error(fr,"Attempt to access field '" + field + "' in something not of class type.");
		println(fr.line + ":\tField Reference has type: " + fr.type);

	/*if (inFieldInit && currentFieldDecl.fieldNumber <= fr.myDecl.fieldNumber && currentClass.name().equals(   (((ClassType)fr.targetType).myDecl).name()))
	    Error.error(fr,"Illegal forward reference of non-initialized field.");
	*/
		return fr.type;
	}

	/** FOR STATEMENT */
	public Object visitForStat(ForStat fs) {
		println(fs.line + ":\tVisiting a ForStat.");

		// YOUR CODE HERE
		if(fs.expr() != null) {
			Type expressionType = (Type) fs.expr().visit(this);
		}else{
		}

		if(fs.expr() != null && (fs.expr().type == null || !fs.expr().type.isBooleanType())){
			Error.error("Non boolean Expression found in for-statement.");
		}

		if(fs.stats() != null){
			fs.stats().visitChildren(this);
		}
		return null;
	}

	/** IF STATEMENT */
	public Object visitIfStat(IfStat is) {
		println(is.line + ":\tVisiting an IfStat");
		// YOUR CODE HERE

		Type expressionType = (Type) is.expr().visit(this);

		if(expressionType == null || !expressionType.isBooleanType()){
			Error.error("Non boolean Expression found as test in if-statement.");
		}

		if(is.thenpart() != null){
			is.thenpart().visitChildren(this);
		}

		if(is.elsepart() != null){
			is.elsepart().visitChildren(this);
		}
		return null;
	}

	/** INVOCATION */
	public Object visitInvocation(Invocation in) {
		println(in.line + ":\tVisiting an Invocation.");

		ClassDecl targetClass = null;

		System.out.println(in.methodName());
		// YOUR CODE HERE
		if(in.target() == null){
			targetClass = currentClass;
		}else if (in.target() instanceof Super){
			targetClass = currentClass.superClass().myDecl;
		}else {
			Type ct = (Type) in.target().visit(this);
			if(!(ct instanceof ClassType) && !(ct.isStringType())){
				Error.error("Attempt to invoke method in '" + in.methodName() + "' something not of class type.");
			}
			targetClass = ((ClassType)ct).myDecl;
		}

		if(in.params() != null){
			in.params().visitChildren(this);
		}

		ClassBodyDecl method = findMethod(targetClass.allMethods, in.methodName().getname(), in.params(), true);

		if(method == null){
			Error.error("No method found");
		}else{
			MethodDecl methodDecl = (MethodDecl) method;
			in.targetMethod = methodDecl;
			in.type = methodDecl.returnType();
		}

		println(in.line + ":\tInvocation has type: " + in.type);
		return in.type;
	}

	/** LITERAL */
	public Object visitLiteral(Literal li) {
		println(li.line + ":\tVisiting a literal (" + li.getText() + ").");

		// YOUR CODE HERE
		if(li.getKind() == Literal.NullKind){
			li.type = new NullType(li);
		}else {
			li.type = new PrimitiveType(li.getKind());
		}
		println(li.line + ":\tLiteral has type: " + li.type);
		return li.type;
	}

	/** METHOD DECLARATION */
	public Object visitMethodDecl(MethodDecl md) {
		println(md.line + ":\tVisiting a MethodDecl.");
		currentContext = md;

		// YOUR CODE HERE
		md.visitChildren(this);

		return null;
	}

	/** NAME EXPRESSION */
	public Object visitNameExpr(NameExpr ne) {
		println(ne.line + ":\tVisiting a NameExpr.");

		// YOUR CODE HERE

		if(ne.myDecl instanceof ClassDecl){
			ClassDecl classDecl = (ClassDecl) ne.myDecl;
			ClassType classType = new ClassType(classDecl.className());
			classType.myDecl = classDecl;
			ne.type = classType;
		}else if(ne.myDecl instanceof FieldDecl){
			FieldDecl fd = (FieldDecl) ne.myDecl;
			ne.type = fd.type();
		}else if(ne.myDecl instanceof ParamDecl){
			ParamDecl paramDecl = (ParamDecl) ne.myDecl;
			ne.type = paramDecl.type();
		}else if(ne.myDecl instanceof LocalDecl){
			LocalDecl ld = (LocalDecl) ne.myDecl;
			ne.type = ld.type();
		}

		println(ne.line + ":\tName Expression has type: " + ne.type);
		return ne.type;
	}

	/** NEW */
	public Object visitNew(New ne) {
		println(ne.line + ":\tVisiting a New.");

		// YOUR CODE HERE

		Type classType = (Type) ne.type().visit(this);

		if(((ClassType) classType).myDecl.isInterface()){
			Error.error("Cannot instantiate interface '" + classType.typeName() + "'");
		}

		if(ne.args() != null){
			ne.args().visitChildren(this);
		}

		//
		ClassDecl targetClass = (ClassDecl) classTable.get(ne.type().typeName());

		ClassBodyDecl constructor = findMethod(targetClass.constructors, targetClass.name(), ne.args(), false);

		if(constructor == null){
			Error.error("VisitNew : No Constructor found");
		}else{
			ne.type = (ClassType) classType;
		}

		println(ne.line + ":\tNew has type: " + ne.type);
		return ne.type;
	}


	/** RETURN STATEMENT */
	public Object visitReturnStat(ReturnStat rs) {
		println(rs.line + ":\tVisiting a ReturnStat.");
		Type returnType;

		if (currentContext instanceof MethodDecl)
			returnType = ((MethodDecl)currentContext).returnType();
		else
			returnType = null;

		// Check is there is a return in a Static Initializer
		if (currentContext instanceof StaticInitDecl)
			Error.error(rs,"return outside method.");

		// Check if a void method is returning something.
		if (returnType == null || returnType.isVoidType()) {
			if (rs.expr() != null)
				Error.error(rs, "Return statement of a void function cannot return a value.");
			return null;
		}

		// Check if a non void method is returning without a proper value.
		if (rs.expr() == null && returnType != null)
			Error.error(rs, "Non void function must return a value.");

		Type returnValueType = (Type) rs.expr().visit(this);
		if (rs.expr().isConstant()) {
			if (returnType.isShortType() && Literal.isShortValue(((BigDecimal)rs.expr().constantValue()).longValue()))
				;// is ok break;
			else if (returnType.isByteType() && Literal.isByteValue(((BigDecimal)rs.expr().constantValue()).longValue()))
				; // is ok break;
			else if (returnType.isCharType() && Literal.isCharValue(((BigDecimal)rs.expr().constantValue()).longValue()))
				; // break;
			else if (!Type.assignmentCompatible(returnType,returnValueType))
				Error.error(rs, "Illegal value of type " + returnValueType.typeName() +
						" in method expecting value of type " + returnType.typeName() + ".");
		} else if (!Type.assignmentCompatible(returnType,returnValueType))
			Error.error(rs, "Illegal value of type " + returnValueType.typeName() +
					" in method expecting value of type " + returnType.typeName() + ".");
		rs.setType(returnType);
		return null;
	}

	/** STATIC INITIALIZER */
	public Object visitStaticInitDecl(StaticInitDecl si) {
		println(si.line + ":\tVisiting a StaticInitDecl.");

		// YOUR CODE HERE
		currentContext = si;
		si.visitChildren(this);
		return null;
	}

	/** SUPER */
	public Object visitSuper(Super su) {
		println(su.line + ":\tVisiting a Super.");

		// YOUR CODE HERE
		ClassType superClass = currentClass.superClass();
		if(superClass == null){
			Error.error("No super class available");
		}
		su.type = superClass;

		return su.type;
	}

	/** SWITCH STATEMENT */
	public Object visitSwitchStat(SwitchStat ss) {
		println(ss.line + ":\tVisiting a SwitchStat.");

		// YOUR CODE HERE
		Type expressionType = (Type) ss.expr().visit(this);

		if(!(expressionType.isIntegralType() || expressionType.isStringType())){
			Error.error("Switch statement expects integer or string type, found type '" + expressionType + "'.");
		}

		if(ss.switchBlocks() != null){
			//ss.switchBlocks().visitChildren(this);
			boolean hasDefault = false;
			HashMap<String, String> caseMap = new HashMap<>();

			for(int i = 0; i < ss.switchBlocks().nchildren; i++) {
				SwitchGroup group = (SwitchGroup) ss.switchBlocks().children[i];
				if (group != null) {
					Sequence labels = group.labels();
					for (int j = 0; j < labels.nchildren; j++) {
						SwitchLabel label = (SwitchLabel)labels.children[j];
						if(label != null && label.expr() != null) {
							Type labelType = (Type) label.expr().visit(this);
							if(caseMap.containsKey(label.expr().toString())){
								Error.error("Duplicate case label.");
							}

							if(expressionType.isIntegralType() && !labelType.isIntegralType()){
								Error.error("Switch labels must match the type of the expression.");
							}

							if (!(labelType.isIntegralType())){
								Error.error("Switch labels must be of type int.");
							}
							if(!(labelType.isIntegralType() && label.expr() instanceof Literal)) {
								Error.error("Switch labels must be of type constants.");
							}
							caseMap.put(label.expr().toString(), "");
						}else if(label.expr() == null) {
							if(hasDefault){
								Error.error("Duplicate default label.");
							}
							hasDefault = true;
						}
					}
				}
			}
		}

		return null;
	}

	// YOUR CODE HERE
	/** TERNARY EXPRESSION */
	public Object visitTernary(Ternary te) {
		println(te.line + ":\tVisiting a Ternary.");

		// YOUR CODE HERE
		Type expressionType = (Type) te.expr().visit(this);

		if(!te.expr().type.isBooleanType()){
			Error.error("Non-Boolean Expression found as test in ternary expression.");
		}

		Type tb = (Type) te.trueBranch().visit(this);
		Type fb = (Type) te.falseBranch().visit(this);

		if(tb.isPrimitiveType() && fb.isPrimitiveType()){
			te.type = new PrimitiveType(PrimitiveType.ceiling((PrimitiveType) tb, (PrimitiveType) fb));
		}else if(tb.isNullType() && !fb.isNullType()){
			te.type = fb;
		}else if(!tb.isNullType() && fb.isNullType()){
			te.type = tb;
		}else if(tb.isClassType() || fb.isClassType()){
			ClassType firstClassType = (ClassType) tb;
			ClassType secondClassType = (ClassType) fb;

			List<ClassDecl> firstClassHierarchy = new ArrayList<>();
			List<ClassDecl> secondClassHierarchy = new ArrayList<>();

			ClassDecl fcd = firstClassType.myDecl;

			while (fcd != null){
				firstClassHierarchy.add(fcd);
				if(fcd.superClass() != null) {
					fcd = (ClassDecl) fcd.superClass().myDecl;
				}else{
					fcd = null;
				}
			}

			ClassDecl scd = secondClassType.myDecl;

			while (scd != null){
				secondClassHierarchy.add(scd);
				if(scd.superClass() != null) {
					scd = (ClassDecl) scd.superClass().myDecl;
				}else{
					scd = null;
				}
			}

			ClassType commonSuperClassType = null;

			for(int i = 0; i < firstClassHierarchy.size(); i++){
				for(int j = 0; j < secondClassHierarchy.size(); j++){
					if(firstClassHierarchy.get(i).getname().equals(secondClassHierarchy.get(j).getname())){
						commonSuperClassType = new ClassType(firstClassHierarchy.get(i).className());
						commonSuperClassType.myDecl = (ClassDecl) classTable.get(firstClassHierarchy.get(i).className().getname());
						break;
					}
				}
				if(commonSuperClassType != null){
					break;
				}
			}

			te.type = commonSuperClassType;

			ClassType commonInterfaceType = null;

			/*for(int i = 0; i < fcd.interfaces().nchildren; i++){
				for(int j = 0; j < scd.interfaces().nchildren; j++){
					if(((ClassType)fcd.interfaces().children[i]))
				}
			}*/

		}

		println(te.line + ":\tTernary has type: " + te.type);
		return te.type;
	}

	/** THIS */
	public Object visitThis(This th) {
		println(th.line + ":\tVisiting a This.");

		th.type = th.type();

		println(th.line + ":\tThis has type: " + th.type);
		return th.type;
	}

	/** UNARY POST EXPRESSION */
	public Object visitUnaryPostExpr(UnaryPostExpr up) {
		println(up.line + ":\tVisiting a UnaryPostExpr.");
		// YOUR CODE HERE
		Type expressionType = (Type)up.expr().visit(this);

		if(!(up.expr() instanceof NameExpr || up.expr() instanceof FieldRef || up.expr() instanceof ArrayAccessExpr)){
			Error.error("Variable expected, found value.");
		}
		if(expressionType.isStringType()){
			Error.error("Cannot apply operator '" + up.op().operator() + "' to something of type String.");
		}
		if(!expressionType.isNumericType()){
			Error.error("Cannot apply operator '" + up.op().operator() + "' to something of type " + expressionType.typeName() + ".");
		}

		up.type = expressionType;

		println(up.line + ":\tUnary Post Expression has type: " + up.type);
		return up.type;
	}

	/** UNARY PRE EXPRESSION */
	public Object visitUnaryPreExpr(UnaryPreExpr up) {
		println(up.line + ":\tVisiting a UnaryPreExpr.");

		// YOUR CODE HERE
		Type expressionType = (Type)up.expr().visit(this);

		if(up.op().getKind() == PreOp.PLUSPLUS || up.op().getKind() == PreOp.MINUSMINUS) {
			if (!(up.expr() instanceof NameExpr || up.expr() instanceof FieldRef || up.expr() instanceof ArrayAccessExpr)) {
				Error.error("Variable expected, found value.");
			}
		}

		if(expressionType.isBooleanType()){
			if(! (up.op().getKind() == PreOp.NOT)){
				Error.error("Cannot apply operator to something of type boolean.");
			}
		}

		if(!(expressionType.isNumericType() || expressionType.isBooleanType())){
			Error.error("Cannot apply operator '" + up.op().operator() + "' to something of type " + expressionType.typeName() + ".");
		}

		if((up.op().getKind() == PreOp.PLUSPLUS || up.op().getKind() == PreOp.MINUSMINUS) && !up.expr().type.isNumericType()){
			Error.error("UnaryPreExpr should be of numeric type +/-");
		}else if((up.op().getKind() == PreOp.NOT) && !up.expr().type.isBooleanType()){
			Error.error("UnaryPreExpr should be of boolean type");
		}else if((up.op().getKind() == PreOp.COMP) && !up.expr().type.isIntegralType()){
			Error.error("UnaryPreExpr should be of integral type");
		}

		if(expressionType.isCharType() || expressionType.isByteType() || expressionType.isShortType()){
			up.type = new PrimitiveType(PrimitiveType.IntKind);
		}else{
			up.type = expressionType;
		}

		println(up.line + ":\tUnary Pre Expression has type: " + up.type);
		return up.type;
	}

	/** VAR */
	public Object visitVar(Var va) {
		println(va.line + ":\tVisiting a Var.");

		// YOUR CODE HERE
		Type varType = (Type) va.myDecl.type();
		Type initType = null;
		if(va.init() != null) {
			initType = (Type) va.init().visit(this);
		}
		if(va.init() != null && !Type.assignmentCompatible(varType, initType)){
			Error.error("Cannot assign value of type " + initType.typeName() + " to variable of type " + varType.typeName() + ".");
		}

		return varType;
	}

	/** WHILE STATEMENT */
	public Object visitWhileStat(WhileStat ws) {
		println(ws.line + ":\tVisiting a WhileStat.");

		// YOUR CODE HERE
		Type expressionType = (Type)ws.expr().visit(this);

		if(ws.expr().type == null || !ws.expr().type.isBooleanType()){
			Error.error("Non boolean Expression found as test in while-statement.");
		}

		if(ws.stat() != null){
			ws.stat().visitChildren(this);
		}

		return null;
	}

}
