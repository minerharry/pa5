package miniJava.AbstractSyntaxTrees;

import miniJava.Compiler;
import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class CustomAST extends AST {

    public CustomAST(SourcePosition posn) {
        super(posn);
        if (Compiler.IS_MINI){
            throw new ASTError("Custom Syntax Element used in minijava; This is an error in parsing.");
        }
    }


}
