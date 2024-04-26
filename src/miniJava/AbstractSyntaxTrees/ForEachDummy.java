package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ForEachDummy extends Statement {

    public ForEachDummy(LocalDecl var, Expression iterExpression, SourcePosition posn) {
        super(posn);
        iterDecl = var;
        this.iterExpression = iterExpression;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        throw new ASTError("Dummy AST element <ForEachDummy> found in AST");
    }

    public LocalDecl iterDecl;
    public Expression iterExpression;
    
    
    @Override
    public ForEachDummy asForEachDummy() {
        return this;
    }

    @Override
    public String repr() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'repr'");
    }
    
}
