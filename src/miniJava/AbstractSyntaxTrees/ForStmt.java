package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ForStmt extends Statement {

    public ForStmt(Statement initStatement, Expression compExpression, Statement incStatement, Statement body, SourcePosition posn) {
        super(posn);
        initStmt = initStatement;
        compExp = compExpression;
        incStmt = incStatement;
        this.body = body;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitForStmt(this,o);
    }

    public Statement initStmt;
    public Expression compExp;
    public Statement incStmt;
    public Statement body;
    
    @Override
    public String repr() {
        return "For statement at position " + posn;
    }
    
}
