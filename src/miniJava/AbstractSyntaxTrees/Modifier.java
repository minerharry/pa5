package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.ModifierType;
import miniJava.SyntacticAnalyzer.Token;

public class Modifier extends AST {

    public ModifierType type;

    public Modifier(Token t) {
        super(t.getTokenPosition());
        type = ModifierType.fromToken(t);
    }

    @Override
    public String toString(){
        return type.toString();
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        return v.visitModifier(this,arg);
    }

    @Override
    public String repr() {
        return "Modifier " + type.name() + " at position " + posn;
    }
    
}
