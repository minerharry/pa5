package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;

public class IncExpr extends Expression {

    public Operator incOp;
    public Expression incExp;
    public boolean post;

    public IncExpr(Operator inc, Expression exp, boolean post, SourcePosition posn) {
        super(posn);
        incExp = exp;
        if (!incExp.isAssignable()){
            throw new ASTError("Cannot increment non-assignable expression " + incExp);
        }
        this.post = post;
        incOp = inc;
    }

    @Override
    public boolean isAssignable() {
        return false;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitIncExpr(this,o);
    }

    @Override
    public boolean isStateable() {
        return true;
    }

    @Override
    public String repr() {
        return "Increment expression at position " + posn;
    }
    
}
