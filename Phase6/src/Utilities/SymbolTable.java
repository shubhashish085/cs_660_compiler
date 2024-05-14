package Utilities;

import java.util.*;

/** 
 * A symbol table class. Each symbol table contains a Vector that
 * contains the symbols defined in the scope that it corresponds to, and a
 * reference to the symbol table for its enclosing scope (if any).
 */
public class SymbolTable {

    /**
     * A reference to the parent symbol table.
     */
    private SymbolTable parent;

    /**
     * The entries of the symbol table.
     */
    public Hashtable<String, Object> entries;

    /**
     * Creates a new symbol table with no parent.
     */
    public SymbolTable() {
	parent = null;
	entries = new Hashtable<String, Object>();
    }

    /**
     * Constructs a new symbol table with 'parent' as the parent.
     * @param parent The parent symbol table.
     */
    public SymbolTable(SymbolTable parent) {
	this();
	this.parent = parent;
    }

    /**
     * Removes an entry from the symbol table.
     * @param name Removed the entry in the table of name 'name'.
     */    
    public void remove(String name) {
	entries.remove(name);
    }
    
    /**
     * Enteres a new entry into the symbol table.
     * @param name The name of the entry object.
     * @param entry The entry.
     */
    public void put(String name,Object entry) {
	Object lookup = entries.get(name);
	if (lookup != null) {
	    System.out.println("Symbol '" + name + "' already defined in this scope.");
	    System.exit(1);
	}
	entries.put(name,entry);
    }
    
    /**
     * Check if a symbol table (or its parents) contains an entry by the name.
     * @param name The name of the entry for which we are looking.
     * @return The associated object - null if no entry is found by that name.
     */
    public Object get(String name) {
	Object result = entries.get(name);
	if (result!=null)
	    return result;
	if (parent==null) {
	    return null;
	}
	return parent.get(name);
    }

    /**
     * Returns the contents of the symbol table (and its parents) as a string.
     * @return The table's contents.
     */
    public String toString() {
	String s = "";
	if (parent != null)
	    s = "\n" + parent.toString();
	return entries.toString() + s;
    }
    
    /**
     * Opens a new scope and returns it.
     * @return The new scope.
     */
    public SymbolTable newScope() {
	return new SymbolTable(this);
    }
    
    /** 
     * Closes the current scope and returns its parent scope
     * @return the current scope's parent scope.
     */
    public SymbolTable closeScope() {
	return parent;
    }
}




