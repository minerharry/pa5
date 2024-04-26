package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;

/* used to be called initializer, now only used for annotation calls */
public class Kwarg extends AST {

    public Kwarg(Token nameToken){
        super(nameToken.getTokenPosition());
        initExpression = null;
        this.name = new Identifier(nameToken);
    }

    public Kwarg(Identifier name, SourcePosition posn){
        super(posn);
        initExpression = null;
        this.name = name;
    }

    public Kwarg(Identifier name, Expression init, SourcePosition posn){
        super(posn);
        initExpression = init;
        this.name = name;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        return v.visitInitializer(this,arg);
    }

    public Identifier name;
    public Expression initExpression;
    
    @Override
    public String repr() {
        return "Keyword argument of variable " + name.spelling + " at position " + posn;
    }

}
