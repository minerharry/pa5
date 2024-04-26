package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class EnumType extends TypeDenoter {

    public EnumDecl parent;

    public EnumType(EnumDecl parentDecl, SourcePosition posn) {
        super(TypeKind.ENUM_TYPE, posn);
        parent = parentDecl;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public boolean isEqual(TypeDenoter other) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isEqual'");
    }

    @Override
    public String repr() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'repr'");
    }

    @Override
    public MemberDecl findMember(Identifier name, boolean allow_type) {
        return parent.findMember(null, allow_type); //TODO: this may end up weird with static shenanigans
    }

}
