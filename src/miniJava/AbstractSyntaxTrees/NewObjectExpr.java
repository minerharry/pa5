/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class NewObjectExpr extends NewExpr
{

    public NewObjectExpr(TypeDenoter ct, ExprList args, SourcePosition posn){
        super(posn);
        type = ct;
        this.args = args;
    }
        
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitNewObjectExpr(this, o);
    }
    
    public TypeDenoter type;
    public ExprList args;

    @Override
    public boolean isAssignable() {
        return false;
    }

    @Override
    public boolean isStateable() {
        return true;
    }

    @Override
    public String repr() {
        return "New object expression at position " + posn;
    }
}
