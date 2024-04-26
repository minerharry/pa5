/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class ThisRef extends Expression{

	public ClassMemberDecl thisDecl;
	
	public ThisRef(SourcePosition posn) {
		super(posn);
	}

	@Override
	public <A, R> R visit(Visitor<A, R> v, A o) {
		return v.visitThisRef(this, o);
	}

	@Override
	public boolean isAssignable() {
		return false;
	}

	@Override
	public String repr() {
		return "This Expression/Reference at position " + posn;
	}

	@Override
	public boolean isReferrable() {
		return true;
	}
	
}
