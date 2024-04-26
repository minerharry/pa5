package miniJava.AbstractSyntaxTrees;

import miniJava.Compiler;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class DoWhileStmt extends Statement {

    public DoWhileStmt(Statement doStmt, Expression whileExp, SourcePosition posn) {
        super(posn);
        this.body = doStmt;
        this.cond = whileExp;
        if (Compiler.IS_MINI){
            throw new ASTError("Do-while statements not allowed in miniJava. This is a parse error.");
        }
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitDoWhileStmt(this,o);
    }

    public Statement body;
    public Expression cond;
    
    @Override
    public String repr() {
        return "Do-While statement at position " + posn;
    }
    
}
