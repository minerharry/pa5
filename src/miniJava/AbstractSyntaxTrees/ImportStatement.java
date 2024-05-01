package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ImportStatement extends Declaration {

    public PackageReference packRef; //package this is referring to 
    public ImportStatement(List<Identifier> importReference, SourcePosition posn) {
        super(new DeclKeywords(), null, importReference.get(importReference.size()-1), posn);
        packRef = new PackageReference(importReference, posn);
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitImportStatement(this,o);
    }

    @Override
    public String repr() {
        return "Import statement at position " + posn;
    }

    //DECORATED DURING IDENTIFICATION
    public Package referredPackage;
    
}
