package miniJava.AbstractSyntaxTrees;

public class AnnotatedType extends TypeDenoter {

    public Annotation annotation;
    public TypeDenoter type;
    
    public AnnotatedType(Annotation annotation, TypeDenoter type) {
        super(TypeKind.ANNOTATED, annotation.posn);
        this.annotation = annotation;
        this.type = type;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public boolean isEqual(TypeDenoter other) {
        // I honestly don't know how annotated types work for type association
        throw new UnsupportedOperationException("Unimplemented method 'isEqual'");
    }

    @Override
    public String repr() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'repr'");
    }

    @Override
    protected MemberDecl findMember(Identifier name, boolean allow_type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMember'");
    }
    
}
