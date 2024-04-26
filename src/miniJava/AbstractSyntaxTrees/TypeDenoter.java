/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.AbstractSyntaxTrees.MemberDecl.MemberType;
import miniJava.ContextualAnalysis.TypeChecker.TypeError;
import miniJava.SyntacticAnalyzer.SourcePosition;

//Base class for all Type objects. I'd rename it to Type but that breaks the ToString behavior >:(
abstract public class TypeDenoter extends AST implements TypeOrExpression {

    public ClassMemberDecl typeDeclaration = null;
    
    public TypeDenoter(TypeKind type, SourcePosition posn){
        super(posn);
        typeKind = type;
    }

    public TypeClassification getClassification(){
        return TypeClassification.TYPE;
    }

    public TypeDenoter asType(){
        return this;
    }
    public Expression asExpression() throws InvalidCastError{
        throw new InvalidCastError("Cannot convert TypeDenoter to Expression");
    }

    public TypeKind typeKind;

    public boolean isIdentifier() {
        return false;
    }

    public Identifier asIdentifier() {
        throw new ASTError("Type " + repr() + " cannot be converted to Identifier");
    }

    public boolean isArrayType() {
        return false;
    }

    public ArrayType asArrayType(){
        throw new ASTError("Type " + repr() + " cannot be converted to ArrayType");
    }

    public boolean equals(TypeDenoter other){
        //NOW: recursive equality using the declarations to ensure identical meaning
        if ((typeDeclaration != null) != (other.typeDeclaration != null)){
            //one has declaration but the other doesn't - bad
            throw new TypeError("type declaration populated for typedenoter " + this.repr() + " but not " + other.repr());
        } else if (typeDeclaration != null){ 
            //both have declaraitons - are they the same?
            if (!other.typeDeclaration.equals(typeDeclaration)){
                return false;
            }
        }

        return this.isEqual(other); //subclass-specific equality
    }

    public abstract boolean isEqual(TypeDenoter other);

    public boolean canBecome(TypeDenoter other) {
        if (this.typeKind == TypeKind.NULL){
            return true;
        }
        if (this.typeKind == TypeKind.ERROR){
            return true;
        }
        if (this.typeKind == TypeKind.UNSUPPORTED){
            return false;
        }

        return this.equals(other);
    
        
    }

    public abstract String repr();


    //ok, so what's the dealio with these two methods??? They seem the same!
    //Well, look at the protection. getMember and getMemberThrow are public-facing methods, and are meant
    //to get members that are *visible to the caller*. findMember, on the other hand, is meant to be 
    //much more analagous to ClassMemberDecl.findMember(), and are used for recursive type finding.
    //The most important aspect is that for the outermost type, allow_type should be set; fields, methods, etc
    //are all properties of an instance type like IdType or QualType, including static methods, but *not* classes.
    //A StaticType, however, does have nested classes as members, so must set allow_type to true when looking into the
    //class it is wrapping!
    protected abstract MemberDecl findMember(Identifier name, boolean allow_type);

    public MemberDecl getMember(Identifier name){
        return findMember(name, false); //most types are instance types, but staticType will override this
    }

    public MemberDecl getMemberThrow(Identifier name) {
        MemberDecl res = getMember(name);
        if (res == null){
            throw new TypeError("Cannot find member " + name.repr() + " of type " + repr());
        }
        return res;
    }
 
    //CODE GENERATION DECORATION
    /**
     * Return size of this object **ON THE STACK** (or inside of another class). This means that for objects, this is the size of the *pointer*.
     */
    public int getTypeSize(){
        // switch (typeKind){
        //     case VOID:
        //     case STATIC:
        //         throw new UnsupportedOperationException("Type " + typeKind + " cannot be allocated");
        //     case INT:
        //         return 4;
        //     case BOOLEAN:
        //         return 1;
        //     case CLASS:
        //         return 8;
        //     case ARRAY:
        //         return 8; //pointer as well
        //     default:
        //         throw new UnsupportedOperationException("Type " + typeKind + " not supported in miniJava!");
        // }
        return 8; //TODO: fix load reasons
    }
    
}

        