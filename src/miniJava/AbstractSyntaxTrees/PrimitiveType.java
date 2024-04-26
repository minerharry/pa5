/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;
import miniJava.ContextualAnalysis.TypeChecker.TypeError;
import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
 /*Primitive Types (int,string,boolean,etc) which don't have any oher values associated with the class */
public class PrimitiveType extends TypeDenoter
{
    public PrimitiveType(TypeKind t, SourcePosition posn){
        super(t, posn);
    }
    
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitPrimitiveType(this, o);
    }

    public static PrimitiveType fromToken(Token t){
        TypeKind kind = TypeKind.fromTokenType(t.getTokenType());
        return new PrimitiveType(kind, t.getTokenPosition());
    }

    @Override
    public boolean isEqual(TypeDenoter other) {
        return this.typeKind == other.typeKind; //checked in equals but w/e
    }

    @Override
    public String repr() {
        return "Primitive type: " + this.typeKind;
    }

    @Override
    protected MemberDecl findMember(Identifier name, boolean allow_type) {
        throw new TypeError(repr() + " has no members");
    }

    @Override
    public int getTypeSize() {
        switch (typeKind) {
            default:
                return 8; //all types are same size for now
                //TODO: type-specific sizing
        }
    }
}
