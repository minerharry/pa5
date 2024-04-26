package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.Compiler;

public class ThrowStmt extends Statement {

    public ThrowStmt(Expression errExp, SourcePosition posn) {
        super(posn);
        exp = errExp;
        if (Compiler.IS_MINI){
            throw new ASTError("Throw statements not allowed in miniJava. This is a parse error.");
        }
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitThrowStmt(this,o);
    }

    public Expression exp;

    @Override
    public String repr() {
        return "Throw statement at position " + posn;
    }
    
}
