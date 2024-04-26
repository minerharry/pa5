package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ImportStatement extends AST {

    public List<Identifier> importRef;
    public boolean importAll;
    public ImportStatement(List<Identifier> importReference, boolean isStar, SourcePosition posn) {
        super(posn);
        importRef = importReference;
        importAll = isStar;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitImportStatement(this,o);
    }

    @Override
    public String repr() {
        return "Import statement at position " + posn;
    }
    
}
