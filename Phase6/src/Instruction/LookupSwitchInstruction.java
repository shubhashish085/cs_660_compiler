package Instruction;

import AST.*;
import java.util.*;

/**
 * Used for the following instructions:
 * 
 * lookupswitch
 * 
 */
public class LookupSwitchInstruction extends Instruction {
    private SortedMap<Object, String> sm; 
    private String defaultLabel;

    public LookupSwitchInstruction(int opCode, SortedMap<Object, String> sm, String defaultLabel) {
	super(opCode);
	this.sm = sm;
	this.defaultLabel = defaultLabel;
    }

    public SortedMap<Object, String> getValues() {
	return sm;
    }
    
    public String getDefaultLabel() {
	return defaultLabel;
    }
    
    public String toString() {
	String result = "lookupswitch\n";
	Object caseValue = null;	
	for (Iterator<Object> ii=sm.keySet().iterator(); ii.hasNext();) {
	    caseValue = ii.next();
	    String label = sm.get(caseValue);
	    
	    result += "\t" + caseValue + "\t: L" + label + "\n";
	}
	result += "\tdefault\t: " + defaultLabel;
	
	return result;
    }
}
