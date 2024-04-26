package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class AmbiguousTypeOrExpression extends AST implements TypeOrExpression{

    public TypeDenoter potType;
    public Expression potExpr;

    public AmbiguousTypeOrExpression(TypeDenoter type, Expression exp, SourcePosition posn) {
        super(posn);
        potType = type;
        potExpr = exp;
    }

    @Override
    public TypeClassification getClassification() {
        return TypeClassification.AMBIGUOUS;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        throw new ASTError("Cannot visit dummy class " + this.getClass());
    }

    @Override
    public TypeDenoter asType() {
        return potType;
    }

    @Override
    public Expression asExpression() {
        return potExpr;
    }

    @Override
    public String repr() {
        return "Ambiguous between type {" + potType.repr() + "} and expression {" + potExpr.repr() + "}";
    }
    
}
