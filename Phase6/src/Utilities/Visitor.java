package Utilities;
import AST.*;

/** Abstract class for the visitor pattern. This abstract class
 *  must be re-implemented for each traversal through the tree.
 * 
 * @author Matt Pedersen
 *
 */
public abstract class Visitor {
    
    // The 'debug' field should be set in the constructor of the 
    // extending class.
    protected static boolean debug;
    
    public static void print(String info) {
	if (debug)
	    System.out.print(info);
    }
    
    /**
     * Prints with a newline if debug is true.
     * @param info The string to be printed
     */    
    public static void println(String info) {
	if (debug) 
	    System.out.println(info);
    }

    /**
     * Visits an ArrayAccessExpr node.
     * @param ae An {@link ArrayAccessExpr} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitArrayAccessExpr(ArrayAccessExpr ae) {
	return ae.visitChildren(this);
    }
    
    /**
     * Visits an ArrayLiteral node.
     * @param al An {@link ArrayLiteral} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitArrayLiteral(ArrayLiteral al) {
	return al.visitChildren(this);
    }
    
    /**
     * Visits an ArrayType node.
     * @param at An {@link ArrayType} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitArrayType(ArrayType at) {
	return at.visitChildren(this);
    }
    
    /**
     * Visits an Assignment node.
     * @param as An {@link Assignment} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitAssignment(Assignment as) {
	return as.visitChildren(this);
    }
    
    /**
     * Visits an AssignmentOp node.
     * @param ao An {@link AssignmentOp} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitAssignmentOp(AssignmentOp ao) {
	return ao.visitChildren(this);
    }

    /**
     * Visits a BinaryExpr node.
     * @param be A {@link BinaryExpr} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitBinaryExpr(BinaryExpr be) {
	return be.visitChildren(this);
    }

    /**
     * Visits a BinOp node.
     * @param bo A {@link BinOp} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitBinOp(BinOp bo) {
	return bo.visitChildren(this);
    }

    /**
     * Visits a Block node.
     * @param bl A {@link Block} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */    
    public Object visitBlock(Block bl) {
	return bl.visitChildren(this);
    }

    /**
     * Visits a BreakStat node.
     * @param bs A {@link BreakStat} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitBreakStat(BreakStat bs) {
	return null;
    }

    /**
     * Visits a CastExpr node.
     * @param ce A {@link CastExpr} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitCastExpr(CastExpr ce) {
	return ce.visitChildren(this);
    }

    /**
     * Visits a CInovcation node.
     * @param ci A {@link CInvocation} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */   
    public Object visitCInvocation(CInvocation ci) {
	return ci.visitChildren(this);
    }

    /**
     * Visits a ClassDecl node.
     * @param cd A {@link ClassDecl} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitClassDecl(ClassDecl cd) {
	return cd.visitChildren(this);
    }

    /**
     * Visits a ClassType node.
     * @param ct A {@link ClassType} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitClassType(ClassType ct) {
	return ct.visitChildren(this);
    }

    /**
     * Visits a Compilation node.
     * @param co A {@link Compilation} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitCompilation(Compilation co) {
	return co.visitChildren(this);
    }

    /**
     * Visits a ConstructorDecl node.
     * @param cd A {@link ConstructorDecl} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitConstructorDecl(ConstructorDecl cd) {
	return cd.visitChildren(this);
    }

    /**
     * Visits a ContinueStat node.
     * @paramcs. A {@link ContinueStat} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitContinueStat(ContinueStat cs) {
	return null;
    }

    /**
     * Visits a DoStat node.
     * @param ds A {@link DoStat} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitDoStat(DoStat ds) {
	return ds.visitChildren(this);
    }

    /**
     * Visits a ExprStat node.
     * @param es A {@link ExprStat} node
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitExprStat(ExprStat es) {
	return es.visitChildren(this);
    }

    /**
     * Visits a FieldDecl node.
     * @param fd A {@link FieldDecl} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitFieldDecl(FieldDecl fd) {
	return fd.visitChildren(this);
    }

    /**
     * Visits a FieldRef node.
     * @param fr A {@link FieldRef} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitFieldRef(FieldRef fr) {
	return fr.visitChildren(this);
    }

    /**
     * Visits a ForStat node.
     * @param fs A {@link ForStat} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitForStat(ForStat fs) {
	return fs.visitChildren(this);
    }

    /**
     * Visits an IfStat node.
     * @param is An {@link IfStat} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitIfStat(IfStat is) {
	return is.visitChildren(this);
    }

    /**
     * Visits an Invocation node.
     * @param in An {@link Invocation} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitInvocation(Invocation in) {
	return in.visitChildren(this);
    }

    /**
     * Visits a Literal node.
     * @param li A {@link Literal} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitLiteral(Literal li) {
	return null;
    }

    /**
     * Visits a LocalDecl node.
     * @param ld A {@link LocalDecl} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitLocalDecl(LocalDecl ld) {
	return ld.visitChildren(this);
    }

    /**
     * Visits a MethodDecl node.
     * @param md A {@link MethodDecl} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */    
    public Object visitMethodDecl(MethodDecl md) {
	return md.visitChildren(this);
    }

    /**
     * Visits a Modifier node.
     * @param mo A {@link Modifier} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitModifier(Modifier mo) {
	return null;
    }

    /**
     * Visits a Name node.
     * @param na A {@link Name} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */ 
    public Object visitName(Name na) {
	return null;
    }

    /**
     * Visits a NameExpr node.
     * @param ne A {@link NameExpr} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitNameExpr(NameExpr ne) {
	return ne.visitChildren(this);
    }

    /**
     * Visits a New node.
     * @param ne A {@link New} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitNew(New ne) {
	return ne.visitChildren(this);
    }
    
    /**
     * Visits a NewArray node.
     * @param ne A {@link NewArray} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitNewArray(NewArray ne) {
	return ne.visitChildren(this);
    }
    
    /**
     * Visits a NullType node.
     * @param nt A {@link NullType} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitNullType(NullType nt) {
	return nt.visitChildren(this);
    }

    /**
     * Visits a ParamDecl node.
     * @param pd A {@link ParamDecl} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitParamDecl(ParamDecl pd) {
	return pd.visitChildren(this);
    }

    /**
     * Visits a PostOp node.
     * @param po A {@link PostOp} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitPostOp(PostOp po) {
	return po.visitChildren(this);
    }

    /**
     * Visits a PreOp node.
     * @param po A {@link PreOp} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitPreOp(PreOp po) {
	return po.visitChildren(this);
    }

    /**
     * Visits a PrimititveType node.
     * @param pt A {@link PrimitiveType} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitPrimitiveType(PrimitiveType pt) {
	return null;
    }

    /**
     * Visits a ReturnStat node.
     * @param rs A {@link ReturnStat} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitReturnStat(ReturnStat rs) {
	return rs.visitChildren(this);
    }

    /**
     * Visits a Sequence node.
     * @param se A {@link Sequence} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */ 
    public Object visitSequence(Sequence se) {
	return se.visitChildren(this);
    }

    /**
     * Visits a StaticInitDecl node.
     * @param si A {@link StaticInitDecl} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitStaticInitDecl(StaticInitDecl si) {
	return si.visitChildren(this);
    }

    /**
     * Visits a Super node.
     * @param su A {@link Super} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitSuper(Super su) {
	return null;
    }

    /**
     * Visits a SwitchGroup node.
     * @param sg A {@link SwitchGroup} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitSwitchGroup(SwitchGroup sg) {
	return sg.visitChildren(this);
    }

    /**
     * Visits a SwitchLabel node.
     * @param sl A {@link SwitchLabel} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitSwitchLabel(SwitchLabel sl) {
	return sl.visitChildren(this);
    }

    /**
     * Visits a SwitchStat node.
     * @param st A {@link SwitchStat} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitSwitchStat(SwitchStat st) {
	return st.visitChildren(this);
    }

    /**
     * Visits a Ternary node.
     * @param te A {@link Ternary} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitTernary(Ternary te) {
	return te.visitChildren(this);
    }
    
    /**
     * Visits a This node.
     * @param th A {@link This} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitThis(This th) {
	return null;
    }

    /**
     * Visits a UnaryPostExpr node.
     * @param up A {@link UnaryPostExpr} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitUnaryPostExpr(UnaryPostExpr up) {
	return up.visitChildren(this);
    }

    /**
     * Visits a UnaryPreExpr node.
     * @param up A {@link UnaryPreExpr} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitUnaryPreExpr(UnaryPreExpr up) {
	return up.visitChildren(this);
    }

    /**
     * Visits a Var node.
     * @param va A {@link Var} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitVar(Var va) {
	return va.visitChildren(this);
    }

    /**
     * Visits a WhileStat node.
     * @param ws A {@link WhileStat} node.
     * @return The return value is either null or a oject of a type that subclasses {@link Type}
     */
    public Object visitWhileStat(WhileStat ws) {
	return ws.visitChildren(this);
    }

}
