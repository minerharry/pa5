package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class SwitchStmt extends Statement {
    

    public SwitchStmt(Expression switchTarget, CaseBlock firstCase, int numCases, SourcePosition posn) {
        super(posn);
        this.target = switchTarget;
        this.firstCase = firstCase;
        this.numCases = numCases;
    }

    public Expression target;
    public CaseBlock firstCase;
    public int numCases;

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        return v.visitSwitchStmt(this,arg);
    }

    @Override
    public String repr() {
        return "Switch statement with " + numCases + " cases at position " + posn;
    }

    //decoration after type checking -
    public static enum SwitchType {
        STRING,
        ENUM,
        INT,
        CHAR
    }

    public SwitchType switchType;

    
}
