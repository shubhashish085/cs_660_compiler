package Instruction;

import AST.*;
import java.util.*;

/**
 * Used for the following instructions:
 * 
 * tableswitch
 * 
 */
public class TableSwitchInstruction extends Instruction {
    private int low;
    private String[] labels;
    private String defaultLabel;

    public TableSwitchInstruction(int opCode, int low, String[] labels, String defaultLabel) {
	super(opCode);
	this.low = low;
	this.labels = labels;
	this.defaultLabel = defaultLabel;
    }
    

    public String toString() {
	String result = "tableswitch " + low + "\n";
	for (int i=0; i<labels.length; i++) {
	    result += "\tL" + labels[i] + "\n";
	}
	result += "\tdefault\t: " + defaultLabel;
	
	return result;
    }
}
