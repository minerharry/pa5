package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.Compiler;

public class BreakStmt extends Statement {

    public BreakStmt(SourcePosition posn) {
        super(posn);
        if (Compiler.IS_MINI){
            throw new ASTError("Break statements not allowed in miniJava. This is a parse error.");
        }
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitBreakStmt(this,o);
    }

    @Override
    public String repr() {
        return "Break Statement at position " + posn;
    }
    
}
