package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.ProtectionType;
import miniJava.SyntacticAnalyzer.Token;

public class Protection extends AST {

    public ProtectionType type;

    public Protection(Token t) {
        super(t.getTokenPosition());
        type = ProtectionType.fromToken(t);
    }

    @Override
    public String toString(){
        return type.toString();
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        return v.visitProtection(this,arg);
    }

    @Override
    public String repr() {
        return "Protection: " + type.name() + " at position " + posn;
    }
    
}
