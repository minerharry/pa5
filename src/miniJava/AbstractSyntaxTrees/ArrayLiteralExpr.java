package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ArrayLiteralExpr extends Expression {

    public ExprList elements;

    public ArrayLiteralExpr(ExprList arrayElements, SourcePosition posn) {
        super(posn);
        elements = arrayElements;
    }

    @Override
    public boolean isAssignable() {
        return false;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitArrayLiteralExpr(this,o);
    }

    @Override
    public String repr() {
        return "Array Literal with " + elements.size() + " elements at position " + this.posn;
    }
    
}
