package miniJava.AbstractSyntaxTrees;
import miniJava.Compiler;

/* like DotExpr, but for :: */
public class MethodRefExpr extends Expression {

    public Expression exp;
    public Identifier name;

    public MethodRefExpr(Expression exp, Identifier name) {
        super(exp.posn);
        this.exp = exp;
        this.name = name;
        if (Compiler.IS_MINI){
            throw new ASTError("Cannot instantiate MethodRefExpr in minijava. This is a parse error.");
        }
    }

    @Override
    public boolean isAssignable() {
        return false;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitMethodRefExpr(this,o);
    }

    @Override
    public String repr() {
        return "Method reference of method ::" + name.spelling + " at position "  + posn;
    }
    
}
