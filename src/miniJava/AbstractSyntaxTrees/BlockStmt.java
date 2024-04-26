/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class BlockStmt extends Statement
{
    public BlockStmt(List<Statement> sl, SourcePosition posn){
        super(posn);
        this.sl = sl;
    }
        
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitBlockStmt(this, o);
    }
   
    public List<Statement> sl;

    @Override
    public String repr() {
        return "Block statement of " + sl.size() + " elements at position " + posn;
    }
}