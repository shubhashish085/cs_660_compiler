package AST;
import Utilities.Visitor;


/** AST is the over-all super class of all parse tree nodes.
 *  @author Matt Pedersen
 *  @version 1.0
 */
public abstract class AST {

    /** The line in the source program that this node is associated with. */
    public int line;

    /** The row in line {@link #line line} of the source program where this node starts. */
    public int charBegin;

    /** The number of children this node has. */
    public int nchildren = 0;

    /** The array of children of this parse-tree node. */
    public AST[] children;

    /** Constructs an AST based on line and row.
     * @param p_line The line number.
     * @param p_charBegin The row number.
     */
    public AST(int p_line, int p_charBegin) {
	line = p_line;
	charBegin = p_charBegin;
    }

    /** Constructs an AST based on the line/row information of a Token object.
     * @param t A token of type {@link Token}.
     */
    public AST(Token t) {
	line = t.getLine();
	charBegin = t.getCharBegin();;
    }

    /** Constructs an AST based on the line/row information of another AST node object (or subclass).
     * @param n An AST object.
     */
    public AST(AST n) {
	if (n == null) {
	    line = 0;
	    charBegin = 0;
	} else {
	    line = n.line;
	    charBegin = n.charBegin;
	}
    }
    
    /** Always returns the empty string ""
     */
    public String toString () {
	return "";
    }

    /** Creates spaces for printing the parse tree in a nice indented manner.
     * @param out A print stream.
     * @param amount The number of spaces.
     */
    private void tab(java.io.PrintStream out, int amount) {
	int i;
	for (i = 0; i < amount; i++)
	    out.print(" ");
    }

    /** Returns an integer as a string with padding on the right.
     * @param i The integer to be converted.
     * @param w The size of the entire returned string.
     * @return Returns the parameter i as a string padded on the right.
     */
    private String intToString(int i, int w) {
	String s = "                    " + Integer.toString(i);
	int length = s.length();
	return s.substring(length - w);
    }

    /** Prints this node
     * @param out A print stream.
     * @param depth The indentation.
     */
    public void print(java.io.PrintStream out, int depth) {
	out.print("line " + this.intToString(line, 3) + ": ");
	tab(out, depth * 2);
	out.println(this.getClass().getName() + " " + this.toString());
	for (int c = 0; c < nchildren; c++) {
	    if (children[c] == null) {
		out.print("line " + this.intToString(line, 3) + ": ");
		tab(out, depth * 2 + 2);
		out.println("empty");
	    } else {
		children[c].print(out, depth + 1);
	    }
	}
    }

    /** 
     * This should never be called.
     * @return "This is getname() in AST - you should never see this."
     */    
    public String getname() {
	return "This is getname() in AST - you should never see this.";
    }

    /**
     */
    public void print(java.io.PrintStream out) {
	this.print(out, 0);
    }

    /**
     */
    public void print() {
	this.print(System.out);
    }

    /** Generic visit method.
     * @param v An object of type {@link Utilities.Visitor}. 
     * @return Any object reference.
     */
    public abstract Object visit(Visitor v);
    
    /** Visit all children of this node from left to right.  Usually called from within a visitor.
     * @param v An object of type {@link Utilities.Visitor}. 
     * @return Always returns null.
     */
    public Object visitChildren(Visitor v) {
	for (int c = 0; c < nchildren; c++) 
	    if (children[c] != null) 
		children[c].visit(v);
	return null;
    }
}
