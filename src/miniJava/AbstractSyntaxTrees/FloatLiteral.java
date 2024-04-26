package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

public class FloatLiteral extends Terminal {

    public FloatLiteral(Token t) {
        super(t);
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitFloatLiteral(this,o);
    }

    @Override
    public String repr() {
        return "Float literal: " + spelling + " at position " + posn;
    }
    
}
