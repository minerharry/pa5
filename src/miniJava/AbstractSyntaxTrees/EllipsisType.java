package miniJava.AbstractSyntaxTrees;


public class EllipsisType extends TypeDenoter {

    public ArrayType base;
    public EllipsisType(TypeDenoter baseType) {
        //represents <Type>... param in method arguments
        //This is actually just an alias for an array type!
        super(TypeKind.ELLIPSIS, baseType.posn);
        base = new ArrayType(baseType, posn);
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitEllipsisType(this,o);
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
    protected MemberDecl findMember(Identifier name, boolean allow_type) {
        return base.findMember(name, allow_type);
    }
    
    
}
