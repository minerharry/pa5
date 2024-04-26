package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class FileHeader extends AST {

    public PackageDecl packageDec;
    public List<ImportStatement> imports;
    
    public FileHeader(PackageDecl packName, List<ImportStatement> imports) {
        super(packName.posn);
        packageDec = packName;
        this.imports = imports;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitFileHeader(this, o);
    }

    @Override
    public String repr() {
        return "File Header at position " + posn;
    }
    
}
