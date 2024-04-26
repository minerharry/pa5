/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class LocalDecl extends VarDecl {
	
	public LocalDecl(DeclKeywords keywords, TypeDenoter t, Identifier name, Expression il, SourcePosition posn){
		super(keywords,t,name,il,posn);
	}

	@Override
	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitLocalDecl(this,o);
	}

	@Override
	public String repr() {
		return "Local declaration at position " + posn;
	}

}
