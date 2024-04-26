/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.TokenType;

public enum TypeKind {
        VOID,
		INT,
        BOOLEAN,
        CLASS,
        ARRAY,
        FLOAT, //should only be used if !Compiler.IS_MINI !!!
        DOUBLE, //should only be used if !Compiler.IS_MINI !!!
        CHAR, //should only be used if !Compiler.IS_MINI !!!
        ELLIPSIS, //should only be used if !Compiler.IS_MINI !!!
        GENERIC, //should only be used if !Compiler.IS_MINI !!!
        ANNOTATED, //should only be used if !Compiler.IS_MINI !!!
        ENUM_TYPE, //weird
        STATIC, //weird
        UNSUPPORTED,
        NULL,
        ERROR;

        public static TypeKind fromTokenType(TokenType t){
            
            switch (t) {
                case voidKeyword:
                    return VOID;
                case intPrimitive:
                    return INT;
                case boolPrimitive:
                    return BOOLEAN;
                case floatPrimitive:
                    return FLOAT;
                case doublePrimitive:
                    return DOUBLE;
                case charPrimitive:
                    return CHAR;
                default:
                    throw new ASTError("Invalid TokenType " + t + "; not a primitive type");
            }
        }
}
