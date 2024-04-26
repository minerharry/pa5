package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class CaseBlock extends AST {

    public Terminal literal; //identifier or literal
    public List<Statement> caseBody;
    public CaseBlock nextBlock; //potentially null!

    /**
     * Unusually, the specific order of the case blocks matters; execution will fall into 
     * the next case block. Uses a linked list structure to represent this.
     * 
     * @param matcher Terminal match target; literal or identifier (enum expression)
     * @param body Statements in the case block
     * @param next Next case block in the sequence
     * @param posn
     * 
     */
    public CaseBlock(Terminal matcher, List<Statement> body, CaseBlock next, SourcePosition posn) {
        super(posn);
        literal = matcher;
        caseBody = body;
        nextBlock = next;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        return v.visitCaseBlock(this,arg);
    }

    @Override
    public String repr() {
        return "Case block: " + literal.spelling + " at position " + posn;
    }

}
