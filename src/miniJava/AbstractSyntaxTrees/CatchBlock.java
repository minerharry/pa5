package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class CatchBlock extends CustomAST {
    public CatchBlock(ParameterDecl excDecl, BlockStmt catchStmt, SourcePosition posn) {
        super(posn);
        exception = excDecl;
        statement = catchStmt;
        
    }
    public ParameterDecl exception;
    public BlockStmt statement;
    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitCatchBlock(this,o);
    }

    @Override
    public String repr() {
        return "Catch Block at position " + posn;
    }
}
