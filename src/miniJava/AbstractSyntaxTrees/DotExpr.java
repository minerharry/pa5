package miniJava.AbstractSyntaxTrees;

import miniJava.Compiler;

/* Ok So
 * This class is weird for a number of reasons.
 * Primarily, it is redundant with QualRef. The reason is simple: in miniJava, dot notation is reserved for identifiers
 * and sub-idenitifiers, whereas in real java (which this is coded for) dot notation is an operation that can be 
 * applied to the result of any expression.
 * The companion class, MethodRefExpr, doesn't have this issue; :: syntax is unique to real java.
 */
public class DotExpr extends Expression {

    public Expression exp;
    public Identifier name;

    public DotExpr(Expression exp, Identifier name) {
        super(exp.posn);
        this.exp = exp;
        this.name = name;
    }

    @Override
    public boolean isAssignable() {
        return true;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitDotExpr(this,o);
    }

    @Override
    public boolean isCallable() {
        return true;
    }

    @Override
    public boolean isReferrable(){
        return exp.isReferrable();
    }

    @Override
    public CallExpr toMethodCall(ExprList el){
        return new CallExpr(exp,name,el,posn);
    }

    @Override
    public String repr() {
        return "Dot Expression {Exp}." +  name.spelling + " at position " + posn;
    }
}
