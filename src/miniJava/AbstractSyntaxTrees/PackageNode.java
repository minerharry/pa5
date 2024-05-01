package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class PackageNode extends AST {

    public PackageNode(int depth, SourcePosition posn) {
        super(posn);
        this.depth = depth;
        //TODO Auto-generated constructor stub
    }

    int depth;

    abstract boolean isSubTree();
    abstract String getName();
    
}
