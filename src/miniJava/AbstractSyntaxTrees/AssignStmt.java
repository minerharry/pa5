/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class AssignStmt extends Statement
{

    public AssignStmt(Expression refExp, Operator assignOp, Expression assignExp, SourcePosition posn){
        super(posn);
        expRef = refExp;
        op = assignOp;
        val = assignExp;
    }
    
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitAssignStmt(this, o);
    }
    
    public Expression expRef;
    public Expression val;
    public Operator op;
    
    @Override
    public String repr() {
        return "Assign Statement at position " + this.posn;
    }
}