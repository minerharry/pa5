/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class NewArrayExpr extends NewExpr
{
    public NewArrayExpr(TypeDenoter et, Expression e, SourcePosition posn){
        super(posn);
        eltType = et;
        sizeExprs = new ExprList();
        sizeExprs.add(e);
    }

    public NewArrayExpr(TypeDenoter et, ExprList sizes, SourcePosition posn){
        super(posn);
        eltType = et;
        sizeExprs = sizes;
    }
    
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitNewArrayExpr(this, o);
    }

    public TypeDenoter eltType;
    public ExprList sizeExprs;
    
    @Override
    public boolean isAssignable() {
        return false;
    }

    @Override
    public String repr() {
        return "New array expression at position " + posn;
    }

    
}