package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.Compiler;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class GenericType extends TypeDenoter {

    public TypeDenoter baseType;
    public List<TypeDenoter> genericTypes;
    
    private boolean empty = false;
    public GenericType(TypeDenoter baseType, SourcePosition posn) {
        super(TypeKind.GENERIC,posn);
        this.baseType = baseType;
        genericTypes = null;
        empty = true; //these generics must be inferred!! Type identification check
        if (Compiler.IS_MINI){
            throw new ASTError("Generic types not allowed in minijava. This is a parse error.");
        }
    }

    public GenericType(TypeDenoter baseType, List<TypeDenoter> inner, SourcePosition posn) {
        super(TypeKind.GENERIC,posn);
        this.baseType = baseType;
        genericTypes = inner;
        if (Compiler.IS_MINI){
            throw new ASTError("Generic types not allowed in minijava. This is a parse error.");
        }
    }

    public boolean isEmpty(){
        return empty;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitGenericType(this,o);
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
        return this.baseType.findMember(name, allow_type);
    }
    
}
