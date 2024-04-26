/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

import java.util.Stack;

import miniJava.Compiler;

public class CallExpr extends Expression
{
    
    public CallExpr(Identifier name, ExprList el, SourcePosition posn){
        super(posn);
        baseExp = null;
        methodName = name;
        argList = el;
    }

    public CallExpr(Expression exp, Identifier name, ExprList el, SourcePosition posn) {
        super(posn);
        baseExp = exp;
        methodName = name;
        argList = el;
    }

    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitCallExpr(this, o);
    }

    public Expression baseExp;
    public Identifier methodName;
    public ExprList argList;
    
    @Override
    public boolean isAssignable() {
        return false;
    }

    @Override
    public boolean isStateable() {
        return true; //can be its own expression
    }

    @Override
    public String repr() {
        return "Method call of " + methodName.spelling + " with " + argList.size() + " args at position " + posn;
    }

    //DECORATION populated during type checking, necessary for unqualified expression
    public ClassMemberDecl context;
}