/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.Compiler;
public class IxExpr extends Expression {


public IxExpr(Expression r, Expression e, SourcePosition posn){
    super(posn);
    exRef = r;
    ixExpr = e;
}
    
public <A,R> R visit(Visitor<A,R> v, A o) {
    return v.visitIxExpr(this, o);
}


public Expression exRef;
public Expression ixExpr;

@Override
public boolean isAssignable() {
    return true;
}

@Override
public String repr() {
    return "Index expression at position " + posn;
}

}
