package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class EmptyMethodDecl extends MethodDecl {

    public EmptyMethodDecl(DeclKeywords keywords, List<GenericVar> generics, TypeDenoter rt, Identifier name, List<ParameterDecl> pl,
            List<TypeDenoter> throwables, SourcePosition posn) {
        super(keywords, generics, rt, name, pl, null, throwables, posn);
    }

    @Override
    public boolean isEmpty(){
        return true;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        return v.visitEmptyMethodDecl(this,arg);
    }

}
