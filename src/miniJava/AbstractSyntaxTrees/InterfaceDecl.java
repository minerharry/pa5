package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class InterfaceDecl extends ClassMemberDecl {

    public InterfaceDecl(DeclKeywords keywords, 
            Identifier cn, 
            List<GenericVar> genericVars,
            List<FieldDecl> fdl, 
            List<MethodDecl> mdl, 
            List<ConstructorDecl> cdl,
            List<ClassMemberDecl> cmdl,
            List<TypeDenoter> interfaces, 
            SourcePosition posn) {
        super(keywords, cn, genericVars, fdl, mdl, cdl, cmdl, null, interfaces, posn);
    }


    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        return v.visitInterfaceDecl(this,arg);
    }

    @Override
    public ClassType getClassType() {
        return ClassType.INTERFACE;
    }
    
}
