package Utilities;

/**
 * A class for minimal version information.
 */
public class Version {
    /**
     * The current version of the Espresso compiler.
     */
    public static String version = "%1.4%";

    /**
     * Array of strings representing the changes between versions.
     */
    public static String changes[] = new String[]{
	"1.0: 2012 November 28th Version",
        "1.1: Fri Nov 30 00:19:36 PST 2012: Versioning added",
        "1.2: Tue Dec  4 21:18:23 PST 2012: Fixed issues with invokestatic",
	"1.3: Sat Apr 18 20:34:00 PST 2015: Fixed issues with arrays of strings, proper check for implementation of abstracts.",
	"1.4: January 2023: Moved all abstract checking to the ModifierChecker.",
        "@"
    };

    /**
     * returns the version number.
     * @return The version number as a stirng.
     */
    public static String getVersion() {
	return version.substring(1,version.length()-1);
    }

    /**
     * returns the version history.
     * @return The version history as a string.
     */
    public static String versionHistory() {
	String s = "Version: " + getVersion() + "\n";
	for (int i=0;i<changes.length-1; i++)
	    s += changes[i] + "\n";
	return s;
    }
}
