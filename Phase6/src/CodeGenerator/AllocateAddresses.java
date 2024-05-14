package CodeGenerator;

import AST.*;
import Utilities.Visitor;

class AllocateAddresses extends Visitor {

	private Generator gen;
	private ClassDecl currentClass;
	private ClassBodyDecl currentBodyDecl;

	AllocateAddresses(Generator g, ClassDecl currentClass, boolean debug) {
		this.debug = debug;
		gen = g;
		this.currentClass = currentClass;
	}

	// EXPERIMENTAL: the block should correctly use the next address now.
	// BLOCK
	public Object visitBlock(Block bl) {
		// YOUR CODE HERE
		int address = gen.getAddress();
		bl.visitChildren(this);
		gen.setAddress(address);
		return null;
	}


	// LOCAL VARIABLE DECLARATION
	public Object visitLocalDecl(LocalDecl ld) {
		// YOUR CODE HERE

		ld.address = gen.getAddress();

		if(ld.type().isLongType() || ld.type().isDoubleType()){
			gen.inc2Address();
		}else {
			gen.incAddress();
		}

		println(ld.line + ": LocalDecl:\tAssigning address:  " + ld.address + " to local variable '" + ld.var().name().getname() + "'.");
		return null;
	}

	// FOR STATEMENT
	public Object visitForStat(ForStat fs) {
		int tempAddress = gen.getAddress();
		fs.visitChildren(this);
		gen.setAddress(tempAddress);
		return null;
	}

	// PARAMETER DECLARATION
	public Object visitParamDecl(ParamDecl pd) {
		// YOUR CODE HERE

		pd.address = gen.getAddress();

		if(pd.type().isLongType() || pd.type().isDoubleType()){
			gen.inc2Address();
		}else {
			gen.incAddress();
		}
		println(pd.line + ": ParamDecl:\tAssigning address:  " + pd.address + " to parameter '" + pd.paramName().getname() + "'.");
		return null;
	}

	// METHOD DECLARATION
	public Object visitMethodDecl(MethodDecl md) {
		println(md.line + ": MethodDecl:\tResetting address counter for method '" + md.name().getname() + "'.");

		// YOUR CODE HERE
		gen.resetAddress();

		if(!md.isStatic()){
			gen.setAddress(1);
		}else{
			gen.setAddress(0);
		}

		if(md.params() != null){
			md.params().visit(this);
		}

		if(md.block() != null) {
			md.block().visit(this);
		}

		currentBodyDecl = md;
		currentBodyDecl.localsUsed = gen.getLocalsUsed();

		println(md.line + ": End MethodDecl");
		gen.resetAddress();
		return null;
	}

	// CONSTRUCTOR DECLARATION
	public Object visitConstructorDecl(ConstructorDecl cd) {
		println(cd.line + ": ConstructorDecl:\tResetting address counter for constructor '" + cd.name().getname() + "'.");
		gen.resetAddress();
		gen.setAddress(1);
		currentBodyDecl = cd;

		// EXPERIMENTAL: we need to visit the  common constructor here
		// but we can't possibly know what the addresses are gonna be
		// for every visit it completely depends on the number of parameters.
		// This is a problem... Possible Solutions: clone ??

		super.visitConstructorDecl(cd);
		cd.localsUsed = gen.getLocalsUsed();
		//System.out.println("Locals Used: " + cd.localsUsed);
		gen.resetAddress();
		println(cd.line + ": End ConstructorDecl");
		return null;
	}

	// STATIC INITIALIZER
	public Object visitStaticInitDecl(StaticInitDecl si) {
		println(si.line + ": StaticInit:\tResetting address counter for static initializer for class '" + currentClass.name() + "'.");
		// YOUR CODE HERE
		gen.resetAddress();
		gen.setAddress(0);
		currentBodyDecl = si;

		super.visitStaticInitDecl(si);
		si.localsUsed = gen.getLocalsUsed();

		gen.resetAddress();
		println(si.line + ": End StaticInit");
		return null;
	}
}

