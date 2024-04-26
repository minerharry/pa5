package miniJava.AbstractSyntaxTrees;

import javax.xml.transform.Source;

import miniJava.SyntacticAnalyzer.SourcePosition;


//Guess I have to do annotations too because of all the @Overrides...
//ANNOTATION SYNTAX
// annotations can be before:
//  - Declarations - part of DeclKeywords
//  - Types - AnnotatedType
// Annotations have the following syntax
// @AnnotationName(arg1 = val1, arg2 = val2, ...)
// @AnnotationName [equivalent to @AnnotationName()]
//
// INCREDIBLY ANNOYINGLY THERE IS A RESERVED "ANNOTATION", @interface, WHICH *DECLARES* AN ANNOTATION

public class Annotation extends AST {

    public TypeDenoter annotationType;
    public KwargList annotationParams;

    public Annotation(TypeDenoter base, KwargList args, SourcePosition posn) {
        super(posn);
        annotationType = base;
        annotationParams = args;
    }
    
    public Annotation(TypeDenoter base, SourcePosition posn){
        super(posn);
        annotationType = base;
        annotationParams = new KwargList();
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'visit'");
    }

    @Override
    public String repr() {
        return "Annotation @" + annotationType.repr();
    }
    
}
