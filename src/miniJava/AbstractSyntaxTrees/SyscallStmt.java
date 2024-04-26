package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

//just a class for handling special case instructions that need to call the os, like i/o
public class SyscallStmt extends Statement {
    public enum SyscallType { 
        PRINTLN
    }

    public SyscallType type;
    public ExprList args = new ExprList();
    
    public SyscallStmt(SyscallType type, SourcePosition posn) {
        super(posn);
        this.type = type;
    }

    public SyscallStmt(SyscallType type, Expression arg, SourcePosition posn) {
        super(posn);
        this.type = type;
        this.args.add(arg);
    }

    public SyscallStmt(SyscallType type, ExprList args, SourcePosition posn) {
        super(posn);
        this.type = type;
        this.args = args;
    }

    @Override
    public String repr() {
        return "System Call of type " + type;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        return v.visitSyscallStmt(this,arg);
    }
}
