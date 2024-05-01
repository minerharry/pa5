package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class InstanceOfExpression extends Expression {

    public Expression expr;
    public TypeDenoter type;

    public InstanceOfExpression(Expression expr, TypeDenoter type, SourcePosition posn) {
        super(posn);
        this.expr = expr;
        this.type = type;
    }

    @Override
    public String repr() {
        return "Instanceof Expression";
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        return v.visitInstanceOf(this,arg);
    }

    @Override
    public boolean isAssignable() {
        return false;
    }

}
