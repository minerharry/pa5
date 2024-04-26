package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.Compiler;

public class ContinueStmt extends Statement{

    public ContinueStmt(SourcePosition posn) {
        super(posn);
        if (Compiler.IS_MINI){
            throw new ASTError("Continue statements not allowed in miniJava. This is a parse error.");
        }
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitContinueStmt(this,o);
    }

    @Override
    public String repr() {
        return "Continue statement at position " + posn;
    }

    
}
