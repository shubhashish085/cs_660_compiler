package Phases;

import AST.*;
import Utilities.SymbolTable;

/**
 * Super class for all the PhaseX subclasses that are used for running
 * the various phases of the compiler.
 */
public abstract class Phase {
    /**
     * The phase the compiler was invoked to run.
     */
    public static int phase;
    /**
     * The root of the parse tree. This is set if phase 2 suceeds.
     */
    public static AST root;
    /**
     * The global class table.
     */
    public static SymbolTable classTable = new SymbolTable();
    /*
     * Each subclass of this class must implement this method to call
     * whatever visitor re-implementations are needed.
     */
    public abstract void execute(Object arg, int debuglevel, int runLevel) ;	
}
