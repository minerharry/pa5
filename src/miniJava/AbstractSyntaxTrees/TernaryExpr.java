package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.Compiler;

public class TernaryExpr extends Expression {

    public Expression conditional;
    public Expression value1;
    public Expression value2;

    public TernaryExpr(Expression cond, Expression v1, Expression v2, SourcePosition posn) {
        super(posn);
        conditional = cond;
        value1 = v1;
        value2 = v2;
        if (Compiler.IS_MINI){
            throw new ASTError("Ternary expressions not allowed in miniJava; ");
        }
    }

    @Override
    public boolean isAssignable() {
        return false;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitTernaryExpr(this,o);
    }

    @Override
    public String repr() {
        return "Ternary Expression at position " + posn;
    }
    
}
