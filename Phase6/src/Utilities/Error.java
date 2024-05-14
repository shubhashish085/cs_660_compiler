package Utilities;

import AST.*;

/** 
 * Class with static methods for producing and displaying error messages.
 */
public class Error {

    /**
     * The name of the file being compiled.
     */
    public static String fileName = "";
    public static String trimmedFileName = "";
    
    /**
     * Sets the fileName field.
     * @param name The name of the file being compiled.
     */
    public static void setFileName(String name) {
	fileName = name;
	int index = fileName.lastIndexOf("/");
	trimmedFileName = fileName.substring(index + 1);       
    }

    /**
     * Produces an error message with line numbers and terminates.
     * @param e The node that produced the error.
     * @param msg The message to be printed.
     */
    public static void error(AST e, String msg) {
	System.out.println(trimmedFileName + ":" + e.line + ": " + msg);
	System.exit(1);
    }   

     /**
     * Produces an error message without line numbers and terminates.
     * @param msg The message to be printed.
     */
    public static void error(String msg) {
	System.out.println(trimmedFileName + ": " + msg);
	System.exit(1);
    }
    
     /**
     * Produces an error message with line numbers and terminates if 'terminate' is true.
     * @param e The node that produced the error.
     * @param msg The message to be printed.
     * @param terminate determines if the compiler should terminate.
     */
    public static void error(AST e, String msg, boolean terminate) {
	System.out.println(trimmedFileName + ":" + e.line + ": " + msg);
	if (terminate)
	    System.exit(1);
    }   

    /**
     * Produces an error message without line numbers and terminates if 'terminate' is true.
     * @param msg The message to be printed.
     * @param terminate determines if the compiler should terminate.
     */
    public static void error(String msg, boolean terminate) {
	System.out.println(trimmedFileName + ": " + msg);
	if (terminate)
	    System.exit(1);
    }
}
