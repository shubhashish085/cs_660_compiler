package CodeGenerator;

import AST.*;
import Utilities.Error;
import Utilities.Visitor;
import Utilities.SymbolTable;

import java.util.*;

import Instruction.*;
import Jasmin.*;

class GenerateCode extends Visitor {

	private Generator gen;
	private ClassDecl currentClass;
	private boolean insideLoop = false;
	private boolean insideSwitch = false;
	private ClassFile classFile;
	private boolean RHSofAssignment = false;
	private ClassBodyDecl currentContext = null;

	// if a left-hand side of an assignment is an actual parameter,
	// RHSofAssignment will be false, but the extra dup is needed; so
	// use this for that. Should be set to true before parameters are
	// visited in Inovcation, CInvocation, and New
	private boolean isParameter = false;
	private boolean StringBuilderCreated = false;

	String switchBreakLabel = null;


	public GenerateCode(Generator g, boolean debug) {
		gen = g;
		this.debug = debug;
		classFile = gen.getClassFile();
	}

	public void setCurrentClass(ClassDecl cd) {
		this.currentClass = cd;
	}

	// ARRAY VISITORS START HERE

	/** ArrayAccessExpr */
	public Object visitArrayAccessExpr(ArrayAccessExpr ae) {
		println(ae.line + ": Visiting ArrayAccessExpr");
		classFile.addComment(ae, "ArrayAccessExpr");
		// YOUR CODE HERE
		ae.target().visit(this);
		ae.index().visit(this);


		classFile.addComment(ae,"End ArrayAccessExpr");
		return null;
	}

	/** ArrayLiteral */
	public Object visitArrayLiteral(ArrayLiteral al) {
		println(al.line + ": Visiting an ArrayLiteral ");
		// YOUR CODE HERE
		return null;
	}

	/** NewArray */
	public Object visitNewArray(NewArray ne) {
		println(ne.line + ": NewArray:\t Creating new array of type " + ne.type.typeName());
		// YOUR CODE HERE
		return null;
	}

	// END OF ARRAY VISITORS

	// ASSIGNMENT
	public Object visitAssignment(Assignment as) {
		println(as.line + ": Assignment:\tGenerating code for an Assignment.");
		classFile.addComment(as, "Assignment");
		/* If a reference is needed then compute it
	          (If array type then generate reference to the	target & index)
	          - a reference is never needed if as.left() is an instance of a NameExpr
	          - a reference can be computed for a FieldRef by visiting the target
	          - a reference can be computed for an ArrayAccessExpr by visiting its target 
		 */
		if (as.left() instanceof FieldRef) {
			println(as.line + ": Generating reference for FieldRef target ");
			FieldRef fr= (FieldRef)as.left();
			fr.target().visit(this);
			// if the target is a New and the field is static, then the reference isn't needed, so pop it! 
			if (fr.myDecl.isStatic()) // && fr.target() instanceof New) // 3/10/2017 - temporarily commented out
				// issue pop if target is NOT a class name.
				if (fr.target() instanceof NameExpr && (((NameExpr)fr.target()).myDecl instanceof ClassDecl))
					;
				else
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_pop));
		} else if (as.left() instanceof ArrayAccessExpr) {
			println(as.line + ": Generating reference for Array Access target");
			ArrayAccessExpr ae = (ArrayAccessExpr)as.left();
			classFile.addComment(as, "ArrayAccessExpr target");
			ae.target().visit(this);
			classFile.addComment(as, "ArrayAccessExpr index");
			ae.index().visit(this);
		}

		/* If the assignment operator is <op>= then
	            -- If the left hand side is a non-static field (non array): dup (object ref) + getfield
	            -- If the left hand side is a static field (non array): getstatic   
	            -- If the left hand side is an array reference: dup2 +	Xaload 
   		    -- If the left hand side is a local (non array): generate code for it: Xload Y 
		 */


		// TODO: This doesn't work: s += ("." + (int)d);, but s = s + ("." + (int)d); does


		if (as.op().kind != AssignmentOp.EQ) {
			if (as.left() instanceof FieldRef) {
				println(as.line + ": Duplicating reference and getting value for LHS (FieldRef/<op>=)");
				FieldRef fr = (FieldRef)as.left();
				if (!fr.myDecl.isStatic()) {
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup));
					classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_getfield, fr.targetType.typeName(),
							fr.fieldName().getname(), fr.type.signature()));
				} else
					classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_getstatic, fr.targetType.typeName(),
							fr.fieldName().getname(), fr.type.signature()));
			} else if (as.left() instanceof ArrayAccessExpr) {
				println(as.line + ": Duplicating reference and getting value for LHS (ArrayAccessRef/<op>=)");
				ArrayAccessExpr ae = (ArrayAccessExpr)as.left();
				classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup2));
				classFile.addInstruction(new Instruction(Generator.getArrayLoadInstruction(ae.type)));
			} else { // NameExpr
				println(as.line + ": Getting value for LHS (NameExpr/<op>=)");
				NameExpr ne = (NameExpr)as.left();
				int address = ((VarDecl)ne.myDecl).address();

				if (address < 4)
					classFile.addInstruction(new Instruction(Generator.getLoadInstruction(((VarDecl)ne.myDecl).type(), address, true)));
				else
					classFile.addInstruction(new SimpleInstruction(Generator.getLoadInstruction(((VarDecl)ne.myDecl).type(), address, true), address));
			}
		}

		/* Visit the right hand side (RHS) */
		boolean oldRHSofAssignment = RHSofAssignment;
		RHSofAssignment = true;
		as.right().visit(this);
		RHSofAssignment = oldRHSofAssignment;
		/* Convert the right hand sides type to that of the entire assignment */

		if (as.op().kind != AssignmentOp.LSHIFTEQ &&
				as.op().kind != AssignmentOp.RSHIFTEQ &&
				as.op().kind != AssignmentOp.RRSHIFTEQ)
			gen.dataConvert(as.right().type, as.type);

		/* If the assignment operator is <op>= then
				- Execute the operator
		 */
		if (as.op().kind != AssignmentOp.EQ)
			classFile.addInstruction(new Instruction(Generator.getBinaryAssignmentOpInstruction(as.op(), as.type)));

		/* If we are the right hand side of an assignment
		     -- If the left hand side is a non-static field (non array): dup_x1/dup2_x1
			 -- If the left hand side is a static field (non array): dup/dup2
			 -- If the left hand side is an array reference: dup_x2/dup2_x2 
			 -- If the left hand side is a local (non array): dup/dup2 
		 */
		if (RHSofAssignment || isParameter) {
			String dupInstString = "";
			if (as.left() instanceof FieldRef) {
				FieldRef fr = (FieldRef)as.left();
				if (!fr.myDecl.isStatic())
					dupInstString = "dup" + (fr.type.width() == 2 ? "2" : "") + "_x1";
				else
					dupInstString = "dup" + (fr.type.width() == 2 ? "2" : "");
			} else if (as.left() instanceof ArrayAccessExpr) {
				ArrayAccessExpr ae = (ArrayAccessExpr)as.left();
				dupInstString = "dup" + (ae.type.width() == 2 ? "2" : "") + "_x2";
			} else { // NameExpr
				NameExpr ne = (NameExpr)as.left();
				dupInstString = "dup" + (ne.type.width() == 2 ? "2" : "");
			}
			classFile.addInstruction(new Instruction(Generator.getOpCodeFromString(dupInstString)));
		}

		/* Store
		     - If LHS is a field: putfield/putstatic
			 -- if LHS is an array reference: Xastore 
			 -- if LHS is a local: Xstore Y
		 */
		if (as.left() instanceof FieldRef) {
			FieldRef fr = (FieldRef)as.left();
			if (!fr.myDecl.isStatic())
				classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_putfield,
						fr.targetType.typeName(), fr.fieldName().getname(), fr.type.signature()));
			else
				classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_putstatic,
						fr.targetType.typeName(), fr.fieldName().getname(), fr.type.signature()));
		} else if (as.left() instanceof ArrayAccessExpr) {
			ArrayAccessExpr ae = (ArrayAccessExpr)as.left();
			classFile.addInstruction(new Instruction(Generator.getArrayStoreInstruction(ae.type)));
		} else { // NameExpr				
			NameExpr ne = (NameExpr)as.left();
			int address = ((VarDecl)ne.myDecl).address() ;

			// CHECK!!! TODO: changed 'true' to 'false' in these getStoreInstruction calls below....
			if (address < 4)
				classFile.addInstruction(new Instruction(Generator.getStoreInstruction(((VarDecl)ne.myDecl).type(), address, false)));
			else {
				classFile.addInstruction(new SimpleInstruction(Generator.getStoreInstruction(((VarDecl)ne.myDecl).type(), address, false), address));
			}
		}
		classFile.addComment(as, "End Assignment");
		return null;
	}


	// EXPERIMENTAL
	// only blocks where currentMethod is set should be visited.
	public Object visitBlock(Block bl) {
		if (currentContext != null)
			super.visitBlock(bl);
		return null;
	}

	// BINARY EXPRESSION
	public Object visitBinaryExpr(BinaryExpr be) {
		println(be.line + ": BinaryExpr:\tGenerating code for " + be.op().operator() + " :  " + be.left().type.typeName() + " -> " + be.right().type.typeName() + " -> " + be.type.typeName() + ".");
		classFile.addComment(be, "Binary Expression");

		// YOUR CODE HERE


		if(be.op().kind == BinOp.LT || be.op().kind == BinOp.GT || be.op().kind == BinOp.LTEQ || be.op().kind == BinOp.GTEQ
				|| be.op().kind == BinOp.NOTEQ || be.op().kind == BinOp.EQEQ){


			String trueLabel = "L" + gen.getLabel();
			String finishedComparisonLabel = "L" + gen.getLabel();


			if(be.op().kind == BinOp.LT){
				PrimitiveType conversionType = PrimitiveType.ceilingType((PrimitiveType) be.left().type, (PrimitiveType) be.right().type);

				be.left().visit(this);
				gen.dataConvert(be.left().type, conversionType);
				be.right().visit(this);
				gen.dataConvert(be.right().type, conversionType);

				if(conversionType.isIntegerType()) {
					classFile.addInstruction(new JumpInstruction(Generator.getOpCodeFromString("if_" + conversionType.getTypePrefix() + "cmplt"), trueLabel));
				}else if(conversionType.isFloatType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_fcmpg));
					classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_iflt, trueLabel));
				}else if(conversionType.isLongType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_lcmp));
					classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_iflt, trueLabel));
				}else if(conversionType.isDoubleType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_dcmpg));
					classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_iflt, trueLabel));
				}
			}else if(be.op().kind == BinOp.GT){
				PrimitiveType conversionType = PrimitiveType.ceilingType((PrimitiveType) be.left().type, (PrimitiveType) be.right().type);

				be.left().visit(this);
				gen.dataConvert(be.left().type, conversionType);
				be.right().visit(this);
				gen.dataConvert(be.right().type, conversionType);

				if(conversionType.isIntegerType()) {
					classFile.addInstruction(new JumpInstruction(Generator.getOpCodeFromString("if_" + conversionType.getTypePrefix() + "cmpgt"), trueLabel));
				}else if(conversionType.isFloatType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_fcmpg));
					classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifgt, trueLabel));
				}else if(conversionType.isLongType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_lcmp));
					classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifgt, trueLabel));
				}else if(conversionType.isDoubleType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_dcmpg));
					classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifgt, trueLabel));
				}
			}else if(be.op().kind == BinOp.LTEQ) {
				PrimitiveType conversionType = PrimitiveType.ceilingType((PrimitiveType) be.left().type, (PrimitiveType) be.right().type);

				be.left().visit(this);
				gen.dataConvert(be.left().type, conversionType);
				be.right().visit(this);
				gen.dataConvert(be.right().type, conversionType);

				if(conversionType.isIntegerType()) {
					classFile.addInstruction(new JumpInstruction(Generator.getOpCodeFromString("if_" + conversionType.getTypePrefix() + "cmple"), trueLabel));
				}else if(conversionType.isFloatType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_fcmpg));
					classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifle, trueLabel));
				}else if(conversionType.isLongType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_lcmp));
					classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifle, trueLabel));
				}else if(conversionType.isDoubleType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_dcmpg));
					classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifle, trueLabel));
				}
			}else if(be.op().kind == BinOp.GTEQ) {
				PrimitiveType conversionType = PrimitiveType.ceilingType((PrimitiveType) be.left().type, (PrimitiveType) be.right().type);

				be.left().visit(this);
				gen.dataConvert(be.left().type, conversionType);
				be.right().visit(this);
				gen.dataConvert(be.right().type, conversionType);

				if(conversionType.isIntegerType()) {
					classFile.addInstruction(new JumpInstruction(Generator.getOpCodeFromString("if_" + conversionType.getTypePrefix() + "cmpge"), trueLabel));
				}else if(conversionType.isFloatType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_fcmpg));
					classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifge, trueLabel));
				}else if(conversionType.isLongType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_lcmp));
					classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifge, trueLabel));
				}else if(conversionType.isDoubleType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_dcmpg));
					classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifge, trueLabel));
				}

			}else if(be.op().kind == BinOp.EQEQ) {

				if(be.left().type.isNullType() || be.right().type.isNullType()){
					if(!be.left().type.isNullType()){
						be.left().visit(this);
					}else if(!be.right().type.isNullType()){
						be.right().visit(this);
					}

					classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifnull, trueLabel));

				}else {

					PrimitiveType conversionType = PrimitiveType.ceilingType((PrimitiveType) be.left().type, (PrimitiveType) be.right().type);

					be.left().visit(this);
					gen.dataConvert(be.left().type, conversionType);
					be.right().visit(this);
					gen.dataConvert(be.right().type, conversionType);

					if (conversionType.isIntegerType()) {
						classFile.addInstruction(new JumpInstruction(Generator.getOpCodeFromString("if_" + conversionType.getTypePrefix() + "cmpeq"), trueLabel));
					} else if (conversionType.isFloatType()) {
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_fcmpg));
						classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifeq, trueLabel));
					} else if (conversionType.isLongType()) {
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_lcmp));
						classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifeq, trueLabel));
					} else if (conversionType.isDoubleType()) {
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_dcmpg));
						classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifeq, trueLabel));
					} else {
						classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_if_acmpeq, trueLabel));
					}
				}
			} else if (be.op().kind == BinOp.NOTEQ) {

				if(be.left().type.isNullType() || be.right().type.isNullType()){
					if(!be.left().type.isNullType()){
						be.left().visit(this);
					}else if(!be.right().type.isNullType()){
						be.right().visit(this);
					}

					classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifnonnull, trueLabel));

				}else{
					PrimitiveType conversionType = PrimitiveType.ceilingType((PrimitiveType) be.left().type, (PrimitiveType) be.right().type);

					be.left().visit(this);
					gen.dataConvert(be.left().type, conversionType);
					be.right().visit(this);
					gen.dataConvert(be.right().type, conversionType);

					if (conversionType.isIntegerType()) {
						classFile.addInstruction(new JumpInstruction(Generator.getOpCodeFromString("if_" + conversionType.getTypePrefix() + "cmpne"), trueLabel));
					} else if (conversionType.isFloatType()) {
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_fcmpg));
						classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifne, trueLabel));
					} else if (conversionType.isLongType()) {
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_lcmp));
						classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifne, trueLabel));
					} else if (conversionType.isDoubleType()) {
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_dcmpg));
						classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifne, trueLabel));
					} else {
						classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_if_acmpne, trueLabel));
					}
				}
			}else {
				classFile.addInstruction(new JumpInstruction(Generator.getOpCodeFromString("if_acmp"), trueLabel));
			}

			classFile.addInstruction(new Instruction(RuntimeConstants.opc_iconst_0));
			classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_goto, finishedComparisonLabel));
			classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, trueLabel));
			classFile.addInstruction(new Instruction(RuntimeConstants.opc_iconst_1));
			classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, finishedComparisonLabel));
		}

		else if(be.op().kind == BinOp.PLUS || be.op().kind == BinOp.MINUS || be.op().kind == BinOp.MULT || be.op().kind == BinOp.DIV
				|| be.op().kind == BinOp.MOD){

			be.left().visit(this);
			gen.dataConvert(be.left().type, be.type);
			be.right().visit(this);
			gen.dataConvert(be.right().type, be.type);

			switch (be.op().kind) {
				case BinOp.PLUS: {
					classFile.addInstruction(new Instruction(Generator.getOpCodeFromString(be.type.getTypePrefix() + "add")));
					break;
				}
				case BinOp.MINUS:{
					classFile.addInstruction(new Instruction(Generator.getOpCodeFromString(be.type.getTypePrefix() + "sub")));
					break;
				}
				case BinOp.MULT:{
					classFile.addInstruction(new Instruction(Generator.getOpCodeFromString(be.type.getTypePrefix() + "mul")));
					break;
				}
				case BinOp.DIV:{
					classFile.addInstruction(new Instruction(Generator.getOpCodeFromString(be.type.getTypePrefix() + "div")));
					break;
				}
				case BinOp.MOD: {
					classFile.addInstruction(new Instruction(Generator.getOpCodeFromString(be.type.getTypePrefix() + "rem")));
					break;
				}
			}
		}


		else if(be.op().kind == BinOp.LSHIFT || be.op().kind == BinOp.RSHIFT || be.op().kind == BinOp.RRSHIFT){
			be.left().visit(this);
			be.right().visit(this);


			if(be.op().kind == BinOp.LSHIFT){
				if(be.left().type.isIntegerType()){
					classFile.addInstruction(new Instruction(Generator.getOpCodeFromString("ishl")));
				}else if(be.left().type.isLongType()){
					classFile.addInstruction(new Instruction(Generator.getOpCodeFromString("lshl")));
				}
			}else if(be.op().kind == BinOp.RSHIFT){
				if(be.left().type.isIntegerType()){
					classFile.addInstruction(new Instruction(Generator.getOpCodeFromString("ishr")));
				}else if(be.left().type.isLongType()){
					classFile.addInstruction(new Instruction(Generator.getOpCodeFromString("lshr")));
				}
			}else if(be.op().kind == BinOp.RRSHIFT){
				if(be.left().type.isIntegerType()){
					classFile.addInstruction(new Instruction(Generator.getOpCodeFromString("iushr")));
				}else if(be.left().type.isLongType()){
					classFile.addInstruction(new Instruction(Generator.getOpCodeFromString("ilshr")));
				}
			}
		}

		else if (be.op().kind == BinOp.AND || be.op().kind == BinOp.OR || be.op().kind == BinOp.XOR){

			be.left().visit(this);
			gen.dataConvert(be.left().type, be.type);
			be.right().visit(this);
			gen.dataConvert(be.right().type, be.type);

			if(be.op().kind == BinOp.AND){
				classFile.addInstruction(new Instruction(Generator.getOpCodeFromString(be.type.getTypePrefix() + "and")));
			}else if(be.op().kind == BinOp.OR){
				classFile.addInstruction(new Instruction(Generator.getOpCodeFromString(be.type.getTypePrefix() + "or")));
			}else if(be.op().kind == BinOp.XOR){
				classFile.addInstruction(new Instruction(Generator.getOpCodeFromString(be.type.getTypePrefix() + "xor")));
			}


		}

		else if(be.op().kind == BinOp.ANDAND || be.op().kind == BinOp.OROR ) {
			be.left().visit(this);
			String endLabel = "L" + gen.getLabel();

			if(be.op().kind == BinOp.ANDAND){
				classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup));
				classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifeq, endLabel));
				classFile.addInstruction(new Instruction(RuntimeConstants.opc_pop));

			}else if(be.op().kind == BinOp.OROR){
				classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup));
				classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifne, endLabel));
				classFile.addInstruction(new Instruction(RuntimeConstants.opc_pop));
			}
			be.right().visit(this);
			classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, endLabel));
		}

		else if(be.op().kind == BinOp.INSTANCEOF){

			String trueLabel = "L" + gen.getLabel();
			String endLabel = "L" + gen.getLabel();

			be.left().visit(this);
			be.right().visit(this);

			classFile.addInstruction(new ClassRefInstruction(RuntimeConstants.opc_instanceof, be.right().type.typeName()));
		}

		classFile.addComment(be, "End BinaryExpr");
		return null;
	}

	// BREAK STATEMENT
	public Object visitBreakStat(BreakStat br) {
		println(br.line + ": BreakStat:\tGenerating code.");
		classFile.addComment(br, "Break Statement");

		// YOUR CODE HERE
		String labelForBreak = Generator.getBreakLabel();
		if(insideSwitch){
			classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_goto, switchBreakLabel));
		}else{
			classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_goto, labelForBreak));
		}

		classFile.addComment(br, "End BreakStat");
		return null;
	}

	// CAST EXPRESSION
	public Object visitCastExpr(CastExpr ce) {
		println(ce.line + ": CastExpr:\tGenerating code for a Cast Expression.");
		classFile.addComment(ce, "Cast Expression");
		String instString;
		// YOUR CODE HERE

		if(ce.expr() != null){
			ce.expr().visit(this);
		}

		if(ce.expr().type instanceof PrimitiveType && ce.expr().type instanceof PrimitiveType){
			if(ce.expr().type.isIntegerType() && (ce.expr().type.isByteType() || ce.expr().type.isShortType())){

			}else {
				gen.dataConvert(ce.expr().type, ce.type());
			}
		}

		classFile.addComment(ce, "End CastExpr");
		return null;
	}

	// CONSTRUCTOR INVOCATION (EXPLICIT)
	public Object visitCInvocation(CInvocation ci) {
		println(ci.line + ": CInvocation:\tGenerating code for Explicit Constructor Invocation.");
		classFile.addComment(ci, "Explicit Constructor Invocation");

		// YOUR CODE HERE
		if(ci == null || ci.superConstructorCall()){
			classFile.addInstruction(new Instruction(RuntimeConstants.opc_aload_0));
			Sequence argSequence = ci.args();
			for (int i = 0; i < argSequence.nchildren; i++){
				Expression expr = (Expression) argSequence.children[i];
				expr.visit(this);
				gen.dataConvert(expr.type, ((ParamDecl)ci.constructor.params().children[i]).type());
			}

			ConstructorDecl constructorDecl = (ConstructorDecl) (currentClass.constructors.children[0]);

			classFile.addInstruction(new MethodInvocationInstruction(RuntimeConstants.opc_invokespecial, currentClass.superClass().typeName(), "<init>",
					"(" + ci.constructor.paramSignature() + ")V"));
		}else if(ci.thisConstructorCall()){
			Sequence argSequence = ci.args();
			for (int i = 0; i < argSequence.nchildren; i++){
				Expression expr = (Expression) argSequence.children[i];
				expr.visit(this);
				gen.dataConvert(((Expression)argSequence.children[i]).type, ((ParamDecl)ci.constructor.params().children[i]).type());
			}
			classFile.addInstruction(new Instruction(RuntimeConstants.opc_aload_0));
			classFile.addInstruction(new MethodInvocationInstruction(RuntimeConstants.opc_invokespecial,ci.getname(), "<init>", "(" + ci.constructor.paramSignature() + ")V"));
		}

		classFile.addComment(ci, "End CInvocation");
		return null;
	}

	// CLASS DECLARATION
	public Object visitClassDecl(ClassDecl cd) {
		println(cd.line + ": ClassDecl:\tGenerating code for class '" + cd.name() + "'.");

		// We need to set this here so we can retrieve it when we generate
		// field initializers for an existing constructor.
		currentClass = cd;


		GenerateFieldInits generateFieldInits = new GenerateFieldInits(gen, currentClass, true);
		// YOUR CODE HERE
		if(cd.methodTable.get("<clinint>") == null){
			Sequence body = cd.body();
			for(int i = 0; i < body.nchildren; i++){
				if(body.children[i] instanceof FieldDecl){
					FieldDecl fd = (FieldDecl) (body.children[i]);
					if(fd.isStatic() && fd.var().init() != null && (!fd.modifiers.isFinal() || (fd.modifiers.isFinal() && !(fd.var().init() instanceof Literal)))){
						cd.body().append(new StaticInitDecl(new Block(new Sequence())));
					}
				}
			}
		}


		if(cd.body() != null){
			for(int i = 0; i < cd.body().nchildren; i++){
				cd.body().children[i].visit(this);
			}
		}

		return null;
	}

	// CONSTRUCTOR DECLARATION
	public Object visitConstructorDecl(ConstructorDecl cd) {
		println(cd.line + ": ConstructorDecl: Generating Code for constructor for class " + cd.name().getname());

		classFile.startMethod(cd);
		classFile.addComment(cd, "Constructor Declaration");

		currentContext = cd;

		cd.params().visit(this);

		// 12/05/13 = removed if (just in case this ever breaks ;-) )

		// YOUR CODE HERE
		//classFile.addComment(cd, "Field Init Generation Start");

		GenerateFieldInits generateFieldInits = new GenerateFieldInits(gen, currentClass, false);

		if(cd.cinvocation() == null || cd.cinvocation().superConstructorCall()){
			cd.cinvocation().visit(this);
			Hashtable<String, Object> fieldMap = currentClass.fieldTable.entries;
			for(String fieldName: fieldMap.keySet()){
				FieldDecl fieldDecl = (FieldDecl) fieldMap.get(fieldName);
				if(!fieldDecl.isStatic()){
					generateFieldInits.visitFieldDecl(fieldDecl);
				}
			}
		}else if(cd.cinvocation().thisConstructorCall()){
			cd.cinvocation().visit(this);
		}
		classFile.addComment(cd, "Field Init Generation End");


		if(cd.body() != null){
			cd.body().visit(this);
		}

		classFile.addInstruction(new Instruction(RuntimeConstants.opc_return));

		// We are done generating code for this method, so transfer it to the classDecl.
		cd.setCode(classFile.getCurrentMethodCode());
		classFile.endMethod();
		currentContext = null;
		return null;
	}


	// CONTINUE STATEMENT
	public Object visitContinueStat(ContinueStat cs) {
		println(cs.line + ": ContinueStat:\tGenerating code.");
		classFile.addComment(cs, "Continue Statement");

		// YOUR CODE HERE
		String labelForContinue = Generator.getContinueLabel();
		classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_goto, labelForContinue));

		classFile.addComment(cs, "End ContinueStat");
		return null;
	}

	// DO STATEMENT
	public Object visitDoStat(DoStat ds) {
		println(ds.line + ": DoStat:\tGenerating code.");
		classFile.addComment(ds, "Do Statement");

		// YOUR CODE HERE
		String topLabel = "L" + gen.getLabel();
		String endLabel = "L" + gen.getLabel();

		boolean oldInside = insideLoop;
		insideLoop = true;

		Generator.setContinueLabel(topLabel);
		Generator.setBreakLabel(endLabel);

		classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, topLabel));

		if(ds.stat() != null){
			ds.stat().visit(this);
		}

		ds.expr().visit(this);
		classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifeq, endLabel));
		classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_goto, topLabel));
		classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, endLabel));

		insideLoop = oldInside;
		classFile.addComment(ds, "End DoStat");
		return null;
	}


	// EXPRESSION STATEMENT
	public Object visitExprStat(ExprStat es) {
		println(es.line + ": ExprStat:\tVisiting an Expression Statement.");
		classFile.addComment(es, "Expression Statement");

		es.expression().visit(this);
		if (es.expression() instanceof Invocation) {
			Invocation in = (Invocation)es.expression();

			if (in.targetType.isStringType() && in.methodName().getname().equals("length")) {
				println(es.line + ": ExprStat:\tInvocation of method length, return value not uses.");
				gen.dup(es.expression().type, RuntimeConstants.opc_pop, RuntimeConstants.opc_pop2);
			} else if (in.targetType.isStringType() && in.methodName().getname().equals("charAt")) {
				println(es.line + ": ExprStat:\tInvocation of method charAt, return value not uses.");
				gen.dup(es.expression().type, RuntimeConstants.opc_pop, RuntimeConstants.opc_pop2);
			} else if (in.targetMethod.returnType().isVoidType())
				println(es.line + ": ExprStat:\tInvocation of Void method where return value is not used anyways (no POP needed).");
			else {
				println(es.line + ": ExprStat:\tPOP added to remove non used return value for a '" + es.expression().getClass().getName() + "'.");
				gen.dup(es.expression().type, RuntimeConstants.opc_pop, RuntimeConstants.opc_pop2);
			}
		}
		else
		if (!(es.expression() instanceof Assignment)) {
			gen.dup(es.expression().type, RuntimeConstants.opc_pop, RuntimeConstants.opc_pop2);
			println(es.line + ": ExprStat:\tPOP added to remove unused value left on stack for a '" + es.expression().getClass().getName() + "'.");
		}
		classFile.addComment(es, "End ExprStat");
		return null;
	}

	// FIELD DECLARATION
	public Object visitFieldDecl(FieldDecl fd) {
		println(fd.line + ": FieldDecl:\tGenerating code.");

		classFile.addField(fd);

		return null;
	}

	// FIELD REFERENCE
	public Object visitFieldRef(FieldRef fr) {
		println(fr.line + ": FieldRef:\tGenerating code (getfield code only!).");

		// Changed June 22 2012 Array
		// If we have and field reference with the name 'length' and an array target type
		if (fr.myDecl == null) { // We had a array.length reference. Not the nicest way to check!!
			classFile.addComment(fr, "Array length");
			fr.target().visit(this);
			classFile.addInstruction(new Instruction(RuntimeConstants.opc_arraylength));
			return null;
		}

		//classFile.addComment(fr,  "Field Reference");

		// Note when visiting this node we assume that the field reference
		// is not a left hand side, i.e. we always generate 'getfield' code.

		// Generate code for the target. This leaves a reference on the 
		// stack. pop if the field is static!
		fr.target().visit(this);
		if (!fr.myDecl.modifiers.isStatic())
			classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_getfield,
					fr.targetType.typeName(), fr.fieldName().getname(), fr.type.signature()));
		else {
			// If the target is that name of a class and the field is static, then we don't need a pop; else we do:
			if (!(fr.target() instanceof NameExpr && (((NameExpr)fr.target()).myDecl instanceof ClassDecl)))
				classFile.addInstruction(new Instruction(RuntimeConstants.opc_pop));
			classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_getstatic,
					fr.targetType.typeName(), fr.fieldName().getname(),  fr.type.signature()));
		}
		classFile.addComment(fr, "End FieldRef");
		return null;
	}


	// FOR STATEMENT
	public Object visitForStat(ForStat fs) {
		println(fs.line + ": ForStat:\tGenerating code.");
		classFile.addComment(fs, "For Statement");
		// YOUR CODE HERE

		boolean oldInside = insideLoop;
		insideLoop = true;
		String startLabel = "L" + gen.getLabel();
		String trueLabel = "L" + gen.getLabel();
		String continueLabel = "L" + gen.getLabel();
		String falseLabel = "L" + gen.getLabel();

		Generator.setContinueLabel(continueLabel);
		Generator.setBreakLabel(falseLabel);


		fs.init().visit(this);
		classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, startLabel));
		if(fs.expr() != null) {
			fs.expr().visit(this);
		}

		if(fs.expr() != null){
			classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, trueLabel));
			classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifeq, falseLabel));
		}
		if(fs.stats() != null) {
			fs.stats().visit(this);
		}

		classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, continueLabel));
		if(fs.incr() != null){
			fs.incr().visit(this);
			classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_goto, startLabel));
		}

		classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, falseLabel));

		insideLoop = oldInside;
		classFile.addComment(fs, "End ForStat");
		return null;
	}

	// IF STATEMENT
	public Object visitIfStat(IfStat is) {
		println(is.line + ": IfStat:\tGenerating code.");
		classFile.addComment(is, "If Statement");

		// YOUR CODE HERE
		String elseLabel = "L" + gen.getLabel();
		String doneLabel = "L" + gen.getLabel();


		is.expr().visit(this);
		classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifeq, elseLabel));

		if(is.thenpart() != null) {
			is.thenpart().visit(this);
			classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_goto, doneLabel));
		}

		classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, elseLabel));

		if(is.elsepart() != null){
			is.elsepart().visit(this);
		}

		classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, doneLabel));

		classFile.addComment(is,  "End IfStat");

		return null;
	}


	// INVOCATION
	public Object visitInvocation(Invocation in) {
		println(in.line + ": Invocation:\tGenerating code for invoking method '" + in.methodName().getname() + "' in class '" + in.targetType.typeName() + "'.");
		classFile.addComment(in, "Invocation");
		// YOUR CODE HERE
		if(in.target() == null && !in.targetMethod.isStatic()){
			classFile.addInstruction(new Instruction(RuntimeConstants.opc_aload_0));
		}else if(in.target() != null){
			in.target().visit(this);
		}

		if (in.target() != null && in.targetType.isStringType() && in.methodName().getname().equals("length") && in.params().nchildren == 0) {
			classFile.addInstruction(new MethodInvocationInstruction(RuntimeConstants.opc_invokevirtual, in.target().type.typeName() , in.methodName().getname(), "()I"));

		}else if (in.target() != null && in.targetType.isStringType() && in.methodName().getname().equals("charAt") && in.params().nchildren == 1) {
			Expression expr = ((Expression) in.params().children[0]);
			expr.visit(this);
			gen.dataConvert(expr.type, new PrimitiveType(PrimitiveType.IntKind));
			classFile.addInstruction(new MethodInvocationInstruction(RuntimeConstants.opc_invokevirtual, in.target().type.typeName() , in.methodName().getname(), "(I)C"));

		}else if(in.targetMethod.isStatic()){
			if(in.target() != null && !in.target().isClassName()){
				classFile.addInstruction(new Instruction(RuntimeConstants.opc_pop));
			}

			Sequence argSequence = in.params();
			for (int i = 0; i < argSequence.nchildren; i++){
				Expression expr = (Expression) argSequence.children[i];
				expr.visit(this);
				gen.dataConvert(expr.type, ((ParamDecl)in.targetMethod.params().children[i]).type());
			}

			if(in.target() != null) {
				ClassType ct = (ClassType)(in.targetType);
				classFile.addInstruction(new MethodInvocationInstruction(RuntimeConstants.opc_invokestatic, ct.typeName() , in.methodName().getname(),"(" + in.targetMethod.paramSignature() + ")" + in.targetMethod.returnType().signature()));
			}else{
				classFile.addInstruction(new MethodInvocationInstruction(RuntimeConstants.opc_invokestatic, currentClass.name(), in.methodName().getname(), "(" + in.targetMethod.paramSignature() + ")" + in.targetMethod.returnType().signature()));
			}
		}else {

			Sequence argSequence = in.params();
			for (int i = 0; i < argSequence.nchildren; i++){
				Expression expr = (Expression) argSequence.children[i];
				expr.visit(this);
				gen.dataConvert(expr.type, ((ParamDecl)in.targetMethod.params().children[i]).type());
			}

			if(in.target() != null) {
				ClassType ct = (ClassType) (in.targetType);
				if(ct.typeName().equals(currentClass.superClass().typeName())){
					classFile.addInstruction(new MethodInvocationInstruction(RuntimeConstants.opc_invokespecial, ct.typeName(), in.methodName().getname(), "(" + in.targetMethod.paramSignature() + ")" + in.targetMethod.returnType().signature()));
				}else{
					classFile.addInstruction(new MethodInvocationInstruction(RuntimeConstants.opc_invokevirtual, ct.typeName(), in.methodName().getname(), "(" + in.targetMethod.paramSignature() + ")" + in.targetMethod.returnType().signature()));
				}
			}else{
				classFile.addInstruction(new MethodInvocationInstruction(RuntimeConstants.opc_invokevirtual, currentClass.name(), in.methodName().getname(), "(" + in.targetMethod.paramSignature() + ")" + in.targetMethod.returnType().signature()));
			}
		}

		classFile.addComment(in, "End Invocation");

		return null;
	}

	// LITERAL
	public Object visitLiteral(Literal li) {
		println(li.line + ": Literal:\tGenerating code for Literal '" + li.getText() + "'.");
		classFile.addComment(li, "Literal");

		switch (li.getKind()) {
			case Literal.ByteKind:
			case Literal.CharKind:
			case Literal.ShortKind:
			case Literal.IntKind:
				gen.loadInt(li.getText());
				break;
			case Literal.NullKind:
				classFile.addInstruction(new Instruction(RuntimeConstants.opc_aconst_null));
				break;
			case Literal.BooleanKind:
				if (li.getText().equals("true"))
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_iconst_1));
				else
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_iconst_0));
				break;
			case Literal.FloatKind:
				gen.loadFloat(li.getText());
				break;
			case Literal.DoubleKind:
				gen.loadDouble(li.getText());
				break;
			case Literal.StringKind:
				gen.loadString(li.getText());
				break;
			case Literal.LongKind:
				gen.loadLong(li.getText());
				break;
		}
		classFile.addComment(li,  "End Literal");
		return null;
	}

	// LOCAL VARIABLE DECLARATION
	public Object visitLocalDecl(LocalDecl ld) {
		println(ld.line + ": LocalDecl:\tGenerating code for LocalDecl");
		//    classFile.addComment(ld, "#LOCAL " + ld.name());
		//classFile.addComment(ld, "#LOCAL " + ld.address);
		//classFile.addComment(ld, "#LOCAL " + ld.type().typeName());
		//classFile.addComment(ld, "#LOCAL " + ld.name() + " " + ld.address + " " + ld.type().typeName());

		if (ld.var().init() != null) {
			println(ld.line + ": LocalDecl:\tGenerating code for the initializer for variable '" +
					ld.var().name().getname() + "'.");
			classFile.addComment(ld, "Local Variable Declaration");

			// YOUR CODE HERE
			boolean oldrhos = RHSofAssignment;

			int address = ld.address();

			RHSofAssignment = true;

			ld.var().init().visit(this);

			RHSofAssignment = oldrhos;

			if (address < 4)
				classFile.addInstruction(new Instruction(Generator.getStoreInstruction(ld.type(), address, false)));
			else {
				classFile.addInstruction(new SimpleInstruction(Generator.getStoreInstruction(ld.type(), address, false), address));
			}

			classFile.addComment(ld, "End LocalDecl");
		}
		else
			println(ld.line + ": LocalDecl:\tVisiting local variable declaration for variable '" + ld.var().name().getname() + "'.");

		return null;
	}

	// METHOD DECLARATION
	public Object visitMethodDecl(MethodDecl md) {
		println(md.line + ": MethodDecl:\tGenerating code for method '" + md.name().getname() + "'.");
		classFile.startMethod(md);
		currentContext = md;

		classFile.addComment(md, "Method Declaration (" + md.name() + ")");

		md.params().visit(this);
		if (md.block() != null) {
			md.block().visit(this);
		}


		gen.endMethod(md);
		currentContext = null;
		return null;
	}


	// NAME EXPRESSION
	public Object visitNameExpr(NameExpr ne) {
		classFile.addComment(ne, "Name Expression --");

		// ADDED 22 June 2012 
		if (ne.myDecl instanceof ClassDecl) {
			println(ne.line + ": NameExpr:\tWas a class name - skip it :" + ne.name().getname());
			classFile.addComment(ne, "End NameExpr");
			return null;
		}

		// YOUR CODE HERE
		else{

			VarDecl fieldDecl = (VarDecl) ne.myDecl;
			int address = fieldDecl.address();

			if(address < 4){
				classFile.addInstruction(new Instruction(Generator.getOpCodeFromString(ne.type.getTypePrefix() + "load_" + address)));
			}else{
				classFile.addInstruction(new SimpleInstruction(Generator.getOpCodeFromString(ne.type.getTypePrefix() + "load"), address));
			}

		}

		classFile.addComment(ne, "End NameExpr");
		return null;
	}

	// NEW
	public Object visitNew(New ne) {
		println(ne.line + ": New:\tGenerating code");
		classFile.addComment(ne, "New");
		boolean OldStringBuilderCreated = StringBuilderCreated;
		StringBuilderCreated = false;

		// YOUR CODE HERE
		classFile.addInstruction(new ClassRefInstruction(RuntimeConstants.opc_new, ne.type().myDecl.className().getname()));
		classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup));
		ne.args().visit(this);

		classFile.addInstruction(new MethodInvocationInstruction(RuntimeConstants.opc_invokespecial, ne.type().myDecl.className().getname(), "<init>",
				"(" + ne.getConstructorDecl().paramSignature() + ")V"));

		classFile.addComment(ne, "End New");
		StringBuilderCreated = OldStringBuilderCreated;

		return null;
	}

	// PARAM DECL
	//public Object visitParamDecl(ParamDecl pd) {
	//	println(pd.line + ": Visiting a ParamDecl.");
	//	classFile.addComment(pd, "#PARAM " + pd.name() + " " + pd.address + " " + pd.type().typeName());
	//	return null;
	//}

	// RETURN STATEMENT
	public Object visitReturnStat(ReturnStat rs) {
		println(rs.line + ": ReturnStat:\tGenerating code.");
		classFile.addComment(rs, "Return Statement");

		// YOUR CODE HERE
		if(rs.expr() != null){
			rs.expr().visit(this);
			if(rs.expr().type instanceof ClassType || rs.expr().type.isStringType()) {
				classFile.addInstruction(new Instruction(RuntimeConstants.opc_areturn));
			}else if(rs.expr().type.isIntegerType() || rs.expr().type.isByteType() || rs.expr().type.isBooleanType() || rs.expr().type.isShortType()){
				classFile.addInstruction(new Instruction(RuntimeConstants.opc_ireturn));
			}else if(rs.expr().type.isFloatType()){
				classFile.addInstruction(new Instruction(RuntimeConstants.opc_freturn));
			}else if(rs.expr().type.isLongType()){
				classFile.addInstruction(new Instruction(RuntimeConstants.opc_lreturn));
			}else if(rs.expr().type.isDoubleType()){
				classFile.addInstruction(new Instruction(RuntimeConstants.opc_dreturn));
			}else{
				classFile.addInstruction(new Instruction(RuntimeConstants.opc_areturn));
			}

		}else{
			classFile.addInstruction(new Instruction(RuntimeConstants.opc_return));
		}


		classFile.addComment(rs, "End ReturnStat");
		return null;
	}

	// STATIC INITIALIZER
	public Object visitStaticInitDecl(StaticInitDecl si) {
		println(si.line + ": StaticInit:\tGenerating code for a Static initializer.");

		classFile.startMethod(si);
		classFile.addComment(si, "Static Initializer");

		currentContext = si;

		// YOUR CODE HERE
		GenerateFieldInits generateFieldInits = new GenerateFieldInits(gen, currentClass, true);

		Hashtable<String, Object> fieldMap = currentClass.fieldTable.entries;
		for(String fieldName: fieldMap.keySet()){
			FieldDecl fieldDecl = (FieldDecl) fieldMap.get(fieldName);
			if(fieldDecl.isStatic()){
				generateFieldInits.visitFieldDecl(fieldDecl);
			}
		}

		if(si.initializer() != null){
			si.initializer().visit(this);
		}

		classFile.addInstruction(new Instruction(RuntimeConstants.opc_return));

		si.setCode(classFile.getCurrentMethodCode());
		classFile.endMethod();
		currentContext = null;
		return null;
	}

	// SUPER
	public Object visitSuper(Super su) {
		println(su.line + ": Super:\tGenerating code (access).");
		classFile.addComment(su, "Super");

		// YOUR CODE HERE
		classFile.addInstruction(new Instruction(RuntimeConstants.opc_aload_0));

		classFile.addComment(su, "End Super");
		return null;
	}

	// SWITCH STATEMENT
	public Object visitSwitchStat(SwitchStat ss) {
		println(ss.line + ": Switch Statement:\tGenerating code for Switch Statement.");
		// YOUR CODE HERE

		boolean oldInsideSwitch = insideSwitch;
		insideSwitch = true;

		String defaultLabel = "L" + gen.getLabel();
		String breakLabel = "L" + gen.getLabel();

		Generator.setBreakLabel(breakLabel);
		switchBreakLabel = breakLabel;

		if(ss.expr() != null){
			ss.expr().visit(this);
		}

		if(!ss.expr().type.isStringType()){
			SortedMap<Object, String> map = new TreeMap<>();

			Sequence sequence = ss.switchBlocks();
			for(int i = 0; i < sequence.nchildren; i++){
				SwitchGroup group = (SwitchGroup) sequence.children[i];
				SwitchLabel switchLabel = (SwitchLabel) (group.labels().children[0]);

				String label = null;

				if(switchLabel.expr() != null) {
					label = switchLabel.expr().constantValue().toString();
				}

				if(label != null){
					String tag = gen.getLabel();
					map.put(label, tag);
				}
			}


			classFile.addInstruction(new LookupSwitchInstruction(RuntimeConstants.opc_lookupswitch, map, defaultLabel));

			for(int i = 0; i < sequence.nchildren; i++){
				SwitchGroup group = (SwitchGroup) sequence.children[i];
				SwitchLabel switchLabel = (SwitchLabel) (group.labels().children[0]);

				String tag = null;

				String label = null;

				if(switchLabel.expr() != null) {
					label = switchLabel.expr().constantValue().toString();
				}

				if(label != null) {
					tag = "L" + map.get(label);
					classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, tag));
					if(group.statements() != null){
						group.statements().visitChildren(this);
					}
				}else {
					classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, defaultLabel));
					if(group.statements() != null){
						group.statements().visitChildren(this);
					}
				}
			}

			classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, breakLabel));

		}


		insideSwitch = oldInsideSwitch;
		classFile.addComment(ss, "End SwitchStat");
		return null;
	}

	// TERNARY EXPRESSION 
	public Object visitTernary(Ternary te) {
		println(te.line + ": Ternary:\tGenerating code.");
		classFile.addComment(te, "Ternary Statement");

		boolean OldStringBuilderCreated = StringBuilderCreated;
		StringBuilderCreated = false;

		// YOUR CODE HERE
		String trueLabel = "L" + gen.getLabel();
		String falseLabel = "L" + gen.getLabel();
		String endLabel = "L" + gen.getLabel();

		te.expr().visit(this);
		classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifne, falseLabel));
		classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, trueLabel));
		te.trueBranch().visit(this);
		classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_goto, endLabel));
		classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, falseLabel));
		te.falseBranch().visit(this);
		classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, endLabel));

		classFile.addComment(te, "Ternary");
		StringBuilderCreated = OldStringBuilderCreated;
		return null;
	}

	// THIS
	public Object visitThis(This th) {
		println(th.line + ": This:\tGenerating code (access).");
		classFile.addComment(th, "This");

		// YOUR CODE HERE
		classFile.addInstruction(new Instruction(RuntimeConstants.opc_aload_0));

		classFile.addComment(th, "End This");
		return null;
	}

	// UNARY POST EXPRESSION
	public Object visitUnaryPostExpr(UnaryPostExpr up) {
		println(up.line + ": UnaryPostExpr:\tGenerating code.");
		classFile.addComment(up, "Unary Post Expression");

		// YOUR CODE HERE
		if(up.expr() instanceof NameExpr){
			up.expr().visit(this);

			int address = -1;

			if (((NameExpr) up.expr()).myDecl instanceof LocalDecl){
				address = ((LocalDecl) ((NameExpr) up.expr()).myDecl).address;
			}else if(((NameExpr) up.expr()).myDecl instanceof ParamDecl){
				address = ((ParamDecl) ((NameExpr) up.expr()).myDecl).address;
			}

			if(up.expr().type.isIntegerType()){
				if(up.op().getKind() == PostOp.PLUSPLUS) {
					classFile.addInstruction(new IincInstruction(RuntimeConstants.opc_iinc, address, 1));
				}else if(up.op().getKind() == PostOp.MINUSMINUS) {
					classFile.addInstruction(new IincInstruction(RuntimeConstants.opc_iinc, address, -1));
				}
			}else{
				if(up.expr().type.isFloatType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup));
					classFile.addInstruction(new LdcFloatInstruction(RuntimeConstants.opc_ldc_w, 1));
				}else if(up.expr().type.isLongType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup2));
					classFile.addInstruction(new LdcLongInstruction(RuntimeConstants.opc_ldc2_w, 1L));
				}else if(up.expr().type.isDoubleType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup2));
					classFile.addInstruction(new LdcDoubleInstruction(RuntimeConstants.opc_ldc2_w, 1.00));
				}
				classFile.addInstruction(new Instruction(Generator.addOrSub(up.expr().type, up.op().getKind() == PostOp.PLUSPLUS ? true: false)));
				//store

			}


		}else if(up.expr() instanceof FieldRef){

			FieldRef fieldRef = (FieldRef) up.expr();
			if(fieldRef.myDecl.isStatic()) {
				fieldRef.target().visit(this);
				classFile.addInstruction(new Instruction(RuntimeConstants.opc_pop));
				classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_getstatic, fieldRef.target().type.typeName(), fieldRef.fieldName().getname(), up.expr().type.getTypePrefix()));
				if(fieldRef.type.isDoubleType() || fieldRef.type.isLongType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup2));
				}else{
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup));
				}
			}else{
				fieldRef.target().visit(this);
				ClassType ct = (ClassType) fieldRef.target().type;
				if(fieldRef.type.isDoubleType() || fieldRef.type.isLongType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup2));
				}else{
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup));
				}
				classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_getfield, ct.typeName(), fieldRef.fieldName().getname(), up.expr().type.getTypePrefix()));
				if(fieldRef.type.isDoubleType() || fieldRef.type.isLongType()){
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup2_x1));
				}else{
					classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup_x1));
				}
			}

			if(up.expr().type.isIntegerType()){
				classFile.addInstruction(new Instruction(RuntimeConstants.opc_iconst_1));
			}else if(up.expr().type.isFloatType()){
				classFile.addInstruction(new LdcFloatInstruction(RuntimeConstants.opc_ldc_w, 1));
			}else if(up.expr().type.isLongType()){
				classFile.addInstruction(new LdcLongInstruction(RuntimeConstants.opc_ldc2_w, 1L));
			}else if(up.expr().type.isDoubleType()){
				classFile.addInstruction(new LdcDoubleInstruction(RuntimeConstants.opc_ldc2_w, 1.00));
			}

			classFile.addInstruction(new Instruction(Generator.addOrSub(up.expr().type, (up.op().getKind() == PostOp.PLUSPLUS) ? true: false)));

			if(fieldRef.myDecl.isStatic()){
				classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_putstatic, fieldRef.target().type.typeName(), fieldRef.fieldName().getname(), up.expr().type.getTypePrefix()));
			}else {
				ClassType ct = (ClassType) fieldRef.target().type;
				classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_putfield, ct.typeName(), fieldRef.fieldName().getname(), up.expr().type.getTypePrefix()));
			}


		}else if(up.expr() instanceof ArrayAccessExpr){


		}


		classFile.addComment(up, "End UnaryPostExpr");
		return null;
	}

	// UNARY PRE EXPRESSION
	public Object visitUnaryPreExpr(UnaryPreExpr up) {
		println(up.line + ": UnaryPreExpr:\tGenerating code for " + up.op().operator() + " : " + up.expr().type.typeName() + " -> " + up.expr().type.typeName() + ".");
		classFile.addComment(up,"Unary Pre Expression");

		// YOUR CODE HERE


		Type expressionType = up.type;


		if(up.op().getKind() == PreOp.MINUS){
			up.expr().visit(this);
			classFile.addInstruction(new Instruction(Generator.getOpCodeFromString(expressionType.getTypePrefix() + "neg")));
		}else if(up.op().getKind() == PreOp.PLUS){
			up.expr().visit(this);
			//classFile.addInstruction(new Instruction(Generator.getOpCodeFromString(expressionType.getTypePrefix() + "add")));
		}else if(up.op().getKind() == PreOp.COMP){
			up.expr().visit(this);
			if(up.type.isIntegerType()) {
				classFile.addInstruction(new Instruction(Generator.getOpCodeFromString(expressionType.getTypePrefix() + "const_m1")));
				classFile.addInstruction(new Instruction(Generator.getOpCodeFromString(expressionType.getTypePrefix() + "xor")));
			}else{
				classFile.addInstruction(new SimpleInstruction(RuntimeConstants.opc_ldc2_w, -1));
				classFile.addInstruction(new Instruction(Generator.getOpCodeFromString("lxor")));
			}
		}else if(up.op().getKind() == PreOp.NOT){
			up.expr().visit(this);
			classFile.addInstruction(new Instruction(Generator.getOpCodeFromString( "iconst_1")));
			classFile.addInstruction(new Instruction(Generator.getOpCodeFromString( "ixor")));
		}else if(up.op().getKind() == PreOp.PLUSPLUS){
			if(up.expr() instanceof NameExpr){
				VarDecl varDecl = (VarDecl) ((NameExpr) up.expr()).myDecl;

				System.out.println("Variable Name : " + varDecl.name());

				int address = varDecl.address();

				if(up.expr().type.isIntegerType()){
					classFile.addInstruction(new IincInstruction(RuntimeConstants.opc_iinc, address, 1));
					up.expr().visit(this);
				}else {
					up.expr().visit(this);
					if(up.expr().type.isFloatType()){
						classFile.addInstruction(new LdcFloatInstruction(RuntimeConstants.opc_ldc, 1));
					}else if(up.expr().type.isLongType()){
						classFile.addInstruction(new LdcLongInstruction(RuntimeConstants.opc_ldc2_w, 1L));
					}else if(up.expr().type.isDoubleType()){
						classFile.addInstruction(new LdcDoubleInstruction(RuntimeConstants.opc_ldc2_w, 1.00));
					}
					classFile.addInstruction(new Instruction(Generator.addOrSub(up.expr().type, true)));
					if(up.expr().type.isFloatType()){
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup));
					}else{
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup2));
					}

					if(address < 4){
						classFile.addInstruction(new Instruction(Generator.getOpCodeFromString(up.expr().type.getTypePrefix() + "store_" + address)));
					}else{
						classFile.addInstruction(new SimpleInstruction(Generator.getOpCodeFromString(up.expr().type.getTypePrefix() + "store"), address));
					}
				}

			} else if(up.expr() instanceof FieldRef){

				FieldRef fieldRef = (FieldRef) up.expr();
				if(fieldRef.myDecl.isStatic()){
					fieldRef.target().visit(this);
					if(!(fieldRef.target() instanceof NameExpr && (((NameExpr)fieldRef.target()).myDecl instanceof ClassDecl))){
						if(up.expr().type.isLongType() || up.expr().type.isDoubleType()){
							classFile.addInstruction(new Instruction(RuntimeConstants.opc_pop2));
						}else{
							classFile.addInstruction(new Instruction(RuntimeConstants.opc_pop));
						}
					}
					classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_getstatic, fieldRef.targetType.typeName(),
							fieldRef.fieldName().getname(), fieldRef.type.signature()));

				}else{
					fieldRef.target().visit(this);
					classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_getfield, fieldRef.targetType.typeName(),
							fieldRef.fieldName().getname(), fieldRef.type.signature()));
				}

				if(up.expr().type.isIntegerType() || up.expr().type.isByteType() || up.expr().type.isShortType()){
					classFile.addInstruction(new LdcIntegerInstruction(RuntimeConstants.opc_ldc, 1));
				}else if(up.expr().type.isFloatType()){
					classFile.addInstruction(new LdcFloatInstruction(RuntimeConstants.opc_ldc, 1));
				}else if(up.expr().type.isLongType()){
					classFile.addInstruction(new LdcLongInstruction(RuntimeConstants.opc_ldc2_w, 1L));
				}else if(up.expr().type.isDoubleType()){
					classFile.addInstruction(new LdcDoubleInstruction(RuntimeConstants.opc_ldc2_w, 1.00));
				}

				classFile.addInstruction(new Instruction(Generator.addOrSub(up.expr().type, true)));

				if(fieldRef.myDecl.isStatic()){
					if((up.expr().type.isIntegerType() || up.expr().type.isByteType() || up.expr().type.isShortType()) || up.expr().type.isFloatType()){
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup));
					}else{
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup2));
					}

					classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_putstatic, fieldRef.targetType.typeName(),
							fieldRef.fieldName().getname(), fieldRef.type.signature()));

				}else{
					if((up.expr().type.isIntegerType() || up.expr().type.isByteType() || up.expr().type.isShortType()) || up.expr().type.isFloatType()){
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup_x1));
					}else{
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup2_x1));
					}

					classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_putfield, fieldRef.targetType.typeName(),
							fieldRef.fieldName().getname(), fieldRef.type.signature()));
				}

			}


		}else if(up.op().getKind() == PreOp.MINUSMINUS){
			if(up.expr() instanceof NameExpr){
				VarDecl varDecl = (VarDecl) ((NameExpr) up.expr()).myDecl;

				System.out.println("Variable Name : " + varDecl.name());

				int address = varDecl.address();

				if(up.expr().type.isIntegerType()){
					classFile.addInstruction(new IincInstruction(RuntimeConstants.opc_iinc, address, -1));
					up.expr().visit(this);
				}else {
					up.expr().visit(this);
					if(up.expr().type.isFloatType()){
						classFile.addInstruction(new LdcFloatInstruction(RuntimeConstants.opc_ldc, 1));
					}else if(up.expr().type.isLongType()){
						classFile.addInstruction(new LdcLongInstruction(RuntimeConstants.opc_ldc2_w, 1L));
					}else if(up.expr().type.isDoubleType()){
						classFile.addInstruction(new LdcDoubleInstruction(RuntimeConstants.opc_ldc2_w, 1.00));
					}
					classFile.addInstruction(new Instruction(Generator.addOrSub(up.expr().type, false)));
					if(up.expr().type.isFloatType()){
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup));
					}else{
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup2));
					}

					if(address < 4){
						classFile.addInstruction(new Instruction(Generator.getOpCodeFromString(up.expr().type.getTypePrefix() + "store_" + address)));
					}else{
						classFile.addInstruction(new SimpleInstruction(Generator.getOpCodeFromString(up.expr().type.getTypePrefix() + "store"), address));
					}
				}

			} else if(up.expr() instanceof FieldRef){

				FieldRef fieldRef = (FieldRef) up.expr();
				if(fieldRef.myDecl.isStatic()){
					fieldRef.target().visit(this);
					if(!(fieldRef.target() instanceof NameExpr && (((NameExpr)fieldRef.target()).myDecl instanceof ClassDecl))){
						if(up.expr().type.isLongType() || up.expr().type.isDoubleType()){
							classFile.addInstruction(new Instruction(RuntimeConstants.opc_pop2));
						}else{
							classFile.addInstruction(new Instruction(RuntimeConstants.opc_pop));
						}
					}
					classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_getstatic, fieldRef.targetType.typeName(),
							fieldRef.fieldName().getname(), fieldRef.type.signature()));

				}else{
					fieldRef.target().visit(this);
					classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_getfield, fieldRef.targetType.typeName(),
							fieldRef.fieldName().getname(), fieldRef.type.signature()));
				}

				if(up.expr().type.isIntegerType() || up.expr().type.isByteType() || up.expr().type.isShortType()){
					classFile.addInstruction(new LdcIntegerInstruction(RuntimeConstants.opc_ldc, 1));
				}else if(up.expr().type.isFloatType()){
					classFile.addInstruction(new LdcFloatInstruction(RuntimeConstants.opc_ldc, 1));
				}else if(up.expr().type.isLongType()){
					classFile.addInstruction(new LdcLongInstruction(RuntimeConstants.opc_ldc2_w, 1L));
				}else if(up.expr().type.isDoubleType()){
					classFile.addInstruction(new LdcDoubleInstruction(RuntimeConstants.opc_ldc2_w, 1.00));
				}

				classFile.addInstruction(new Instruction(Generator.addOrSub(up.expr().type, false)));

				if(fieldRef.myDecl.isStatic()){
					if((up.expr().type.isIntegerType() || up.expr().type.isByteType() || up.expr().type.isShortType()) || up.expr().type.isFloatType()){
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup));
					}else{
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup2));
					}

					classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_putstatic, fieldRef.targetType.typeName(),
							fieldRef.fieldName().getname(), fieldRef.type.signature()));

				}else{
					if((up.expr().type.isIntegerType() || up.expr().type.isByteType() || up.expr().type.isShortType()) || up.expr().type.isFloatType()){
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup_x1));
					}else{
						classFile.addInstruction(new Instruction(RuntimeConstants.opc_dup2_x1));
					}

					classFile.addInstruction(new FieldRefInstruction(RuntimeConstants.opc_putfield, fieldRef.targetType.typeName(),
							fieldRef.fieldName().getname(), fieldRef.type.signature()));
				}
			}
		}

		classFile.addComment(up, "End UnaryPreExpr");
		return null;
	}

	// WHILE STATEMENT
	public Object visitWhileStat(WhileStat ws) {
		println(ws.line + ": While Stat:\tGenerating Code.");

		classFile.addComment(ws, "While Statement");

		// YOUR CODE HERE
		boolean oldInside = insideLoop;
		insideLoop = true;

		String firstLabel = "L" + gen.getLabel();
		String lastLabel = "L" + gen.getLabel();

		Generator.setContinueLabel(firstLabel);
		Generator.setBreakLabel(lastLabel);

		classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, firstLabel));
		ws.expr().visit(this);
		classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_ifeq, lastLabel));

		if(ws.stat() != null){
			ws.stat().visit(this);
		}

		classFile.addInstruction(new JumpInstruction(RuntimeConstants.opc_goto, firstLabel));
		classFile.addInstruction(new LabelInstruction(RuntimeConstants.opc_label, lastLabel));

		insideLoop = oldInside;

		classFile.addComment(ws, "End WhileStat");
		return null;
	}
}

