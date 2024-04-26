package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.ArrayType;
import miniJava.AbstractSyntaxTrees.ClassMemberDecl;
import miniJava.AbstractSyntaxTrees.Identifier;
import miniJava.AbstractSyntaxTrees.IdentifierType;
import miniJava.AbstractSyntaxTrees.IntLiteral;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.PrimitiveType;
import miniJava.AbstractSyntaxTrees.Terminal;
import miniJava.AbstractSyntaxTrees.TypeDenoter;
import miniJava.AbstractSyntaxTrees.TypeKind;
import miniJava.ContextualAnalysis.TypeChecker.TypeError;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

public class TypeResult {

    //convenience type for conditionals; these names will provide the only map from literal -> primitive
    public static TypeResult BOOLEAN = fromTypeKind(TypeKind.BOOLEAN);
    public static TypeResult INT = fromTypeKind(TypeKind.INT);
    public static TypeResult FLOAT = fromTypeKind(TypeKind.FLOAT);
    public static TypeResult CHAR = fromTypeKind(TypeKind.CHAR);
    public static TypeResult DOUBLE = fromTypeKind(TypeKind.DOUBLE);
    public static TypeResult NULL = fromTypeKind(TypeKind.NULL);
    public static TypeResult VOID = fromTypeKind(TypeKind.VOID);
    
    //TODO: test that this string type actually works! (is there a qualified version of string??) also, does this work for typekind unsupported?
    public static TypeResult STRING = new TypeResult(new IdentifierType(new Identifier(new Token(TokenType.id, "String", null)))); 
    public static TypeResult STRINGARR = new TypeResult(new ArrayType(STRING.getType(), null));

    private TypeDenoter type;
    public ClassMemberDecl origDecl;


    public TypeResult(TypeDenoter t){
        type = t;
        if (t != null){
            origDecl = t.typeDeclaration;
        }
    }

    public boolean equals(TypeResult other){
        return this.type.equals(other.type);
    }

    public static TypeResult fromTypeKind(TypeKind primitiveType) {
        return new TypeResult(new PrimitiveType(primitiveType, null));
    }

    public boolean canBecome(TypeResult expected) {
        return this.type.canBecome(expected.type);
    }

    public MemberDecl getMember(Identifier name) {
        return type.getMemberThrow(name);
    }

    public String repr() {
        return type.repr();
    }

    public TypeDenoter getType(){
        return type;
    }

}
