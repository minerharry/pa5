package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ExprStmt extends Statement {

    public Expression baseExpr;

    public ExprStmt(Expression exp, SourcePosition posn) {
        super(posn);
        baseExpr = exp;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitExprStatement(this,o);
    }

    @Override
    public String repr() {
        return "Expression statement at position " + posn;
    }
    
}
