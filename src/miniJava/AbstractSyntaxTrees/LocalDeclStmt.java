/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.SyntacticAnalyzer.SourcePosition;
 
public class LocalDeclStmt extends Statement
{

    public LocalDeclStmt(List<LocalDecl> lds){
        super(lds.get(0).keywords.posn);
        decls = lds;
    }
    public LocalDeclStmt(List<LocalDecl> lds, SourcePosition posn){
        super(posn);
        decls = lds;
    }
        
    public <A,R> R visit(Visitor<A,R> v, A o) {
        return v.visitLocalDeclStmt(this, o);
    }

    public List<LocalDecl> decls;

    @Override
    public String repr() {
        return "Local declaration statement at position " + posn;
    }
}
