package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

public class CharLiteral extends Terminal {

    public CharLiteral(Token t) {
        super(t);
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitCharLiteral(this,o);
    }

    @Override
    public String repr() {
        return "Char literal: " + spelling + " at position " + posn;
    }
    
}
