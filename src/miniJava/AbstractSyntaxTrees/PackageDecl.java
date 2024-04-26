package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class PackageDecl extends AST {

    public List<Identifier> packRef;
    public PackageDecl(List<Identifier> packRef, SourcePosition posn) {
        super(posn);
        this.packRef = packRef;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitPackageDecl(this,o);
    }

    @Override
    public String repr() {
        return "Package declaration at position " + posn;
    }

}
