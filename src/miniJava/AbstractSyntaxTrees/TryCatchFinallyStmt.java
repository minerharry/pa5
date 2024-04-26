package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class TryCatchFinallyStmt extends Statement {

    public TryCatchFinallyStmt(BlockStmt tryBlock, List<CatchBlock> catchBlocks, BlockStmt finallyBlock, SourcePosition posn) {
        super(posn);
        this.tryBlock = tryBlock;
        this.catchBlocks = catchBlocks;
        this.finallyBlock = finallyBlock;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitTryCatchFinallyStmt(this,o);
    }

    public BlockStmt tryBlock,finallyBlock;
    public List<CatchBlock> catchBlocks;
    
    @Override
    public String repr() {
        return "Try catch finally statement at position " + posn;
    }


    
}
