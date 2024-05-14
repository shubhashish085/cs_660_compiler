package AST;

/**
 * This class does not appear in the syntax tree. It is used only to
 * transport a {@link CInvocation} and a {@link Sequence} of
 * statements to the grammar rule for a Constructor
 * Declaration.<br><br> The two parts that are needed as parameters
 * for the {@link ConstructorDecl} can be obtained as <code>.ci</code> for
 * the {@link CInvocation}, and as <code>.st</code> for the {@link
 * Sequence} of statements. Note, The explicit constructor invocation
 * may be null, but the sequence of statements is always a sequence,
 * though perhaps empty.
 */

public class ConstructorBody {
    /**
     * The explicit constructor invocation -- may be null.
     */
    public CInvocation ci;
    /**
     * The sequence of statements -- never null but may be empty.
     */
    public Sequence st;

    /**
     * Constructs a constructor body node.
     * @param ci An explcit constructor invocation node (may be null).
     * @param st A sequence of statements (mey never be null, but may be empty).
     */
    public ConstructorBody(CInvocation ci, Sequence /* of Statements */ st) {
	this.ci = ci;
	this.st = st;
    }   
}
