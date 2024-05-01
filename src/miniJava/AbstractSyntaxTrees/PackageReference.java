package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class PackageReference extends AST {

    public List<Identifier> packRef;
    public PackageReference(List<Identifier> packRef, SourcePosition posn) {
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

    public String getFullName() {
        String res = packRef.get(0).spelling;
        for (int i = 1; i < packRef.size(); i++){
            res += ".";
            res += packRef.get(i).spelling;
        }
        return res;
    }

    @Override
    public int hashCode(){
        return this.getFullName().hashCode();
    }

}
