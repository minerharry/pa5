package miniJava.AbstractSyntaxTrees;

import java.util.ArrayList;
import java.util.List;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class EnumDecl extends ClassMemberDecl {

    public List<EnumElement> elements;

    public EnumDecl(DeclKeywords keywords, Identifier cn, List<EnumElement> elements, List<FieldDecl> fdl, List<MethodDecl> mdl,
            List<ConstructorDecl> cdl, List<ClassMemberDecl> cmdl, List<TypeDenoter> interfaces, 
            SourcePosition posn) {
        super(keywords, cn, null, fdl, mdl, cdl, cmdl, null, interfaces, posn);
        this.elements = elements;
    }

    public EnumDecl(DeclKeywords keywords, Identifier cn, List<EnumElement> elements, List<TypeDenoter> interfaces, SourcePosition posn){
        super(keywords, cn, null, new ArrayList<FieldDecl>(), new ArrayList<MethodDecl>(), new ArrayList<ConstructorDecl>(), 
        new ArrayList<ClassMemberDecl>(), null, interfaces, posn);
        this.elements = elements;
    }

    @Override
    public ClassType getClassType() {
        return ClassType.ENUM;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        return v.visitEnumDecl(this,arg);
    }

    @Override
    public MemberDecl findMember(String name, boolean allow_type){
        if (!allow_type) {
            for (EnumElement en : elements){
                if (en.name.spelling.equals(name)){
                    return en;
                }
            }
        }
        return super.findMember(name,allow_type);
    }
    
}
