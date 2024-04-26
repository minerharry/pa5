package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.ContextualAnalysis.TypeChecker.TypeError;

public class QualType extends TypeDenoter {

    public Identifier id;
    public TypeDenoter baseType;
    public QualType(TypeDenoter base, Identifier name) {
        super(TypeKind.CLASS, base.posn);
        id = name;
        baseType = base;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitQualType(this,o);
    }

    @Override
    public boolean isEqual(TypeDenoter o) {
        if (o instanceof QualType){
            QualType other = (QualType)o;
            //this should be populated with typedeclaration (handled in equals), but just in case, the name should be the same
            if (!other.id.spelling.equals(this.id.spelling)){
                return false;
            }
            return baseType.equals(other.baseType);
        } else if (o instanceof IdentifierType) {
            //hear me out
            //we know the declarations are referring to the same place
            //so if I'm in class X and receive an instance of X.C, that will be the same as C - they both point to the same declaration
            //TODO: are generic instance classes an exception to this?
            //for sanity check, let's check the name:
            IdentifierType other = (IdentifierType)o;
            return other.className.spelling.equals(this.id.spelling);
        } else {
            return false;
        }
    }

    @Override
    public String repr() {
        return "Qualtype {" + baseType.repr() + "} . {" + id.repr() + "}";
    }

    @Override
    protected MemberDecl findMember(Identifier name, boolean allow_type) {
        if (this.typeDeclaration == null){
            throw new UnsupportedOperationException("Cannot get declaration of type " + repr());
        } else {
            return this.typeDeclaration.findMember(name.spelling, allow_type);
        }
    }
    
}
