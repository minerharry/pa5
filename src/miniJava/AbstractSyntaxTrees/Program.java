package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class Program extends AST {

    // public PackageTree packages;

    public Program(SourcePosition posn, List<Package> packages) {
        super(posn);
        // this.packages = PackageTree.makeTree(packages);
    }

    @Override
    public String repr() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'repr'");
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        return v.visitProgram(this,arg);
    }
    
}
