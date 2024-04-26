package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.SyntacticAnalyzer.SourcePosition;

//specifically for public <A,R> R blah(); or public class wow<A,R>
// also known as a "Formal Type Parameter"
public class GenericVar extends Declaration {

    public List<TypeDenoter> supertypes;

    public GenericVar(DeclKeywords keywords, Identifier base, List<TypeDenoter> supers, SourcePosition posn) {
        super(keywords,null,base,posn);
        //TODO: maybe genericvars should have a more interesting type?
        supertypes = supers;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        return v.visitGenericVar(this,arg);
    }

    @Override
    public String repr() {
        return "Generic Var " + name.spelling + " at position " + posn;
    }
    
}
