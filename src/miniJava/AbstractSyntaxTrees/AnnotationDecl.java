package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class AnnotationDecl extends ClassMemberDecl {

    public AnnotationDecl(DeclKeywords keywords, Identifier cn, List<FieldDecl> fdl, List<MethodDecl> mdl,
            List<ConstructorDecl> cdl, List<ClassMemberDecl> cmdl, TypeDenoter superclass, List<TypeDenoter> interfaces,
            SourcePosition posn) {
        super(keywords, cn, null, fdl, mdl, cdl, cmdl, superclass, interfaces, posn);
    }

    @Override
    public ClassType getClassType() {
        return ClassType.ANNOTATION;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        return v.visitAnnotationDecl(this,arg);
    }

}
