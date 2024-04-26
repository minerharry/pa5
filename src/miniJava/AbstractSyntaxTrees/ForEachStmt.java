package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ForEachStmt extends Statement {

    public ForEachStmt(ForEachDummy dummy, Statement body, SourcePosition posn) {
        super(posn);
        iterDecl = dummy.iterDecl;
        iterExpression = dummy.iterExpression;
        this.body = body;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitForEachStmt(this,o);
    }

    public LocalDecl iterDecl;
    public Expression iterExpression;
    public Statement body;
    @Override
    public String repr() {
        return "For-Each statement at position "  + posn;
    }
}
