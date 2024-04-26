/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class FieldDecl extends VarDecl implements MemberDecl {
	public FieldDecl(DeclKeywords keywords, TypeDenoter t, Identifier name, Expression initializer, SourcePosition posn) {
		super(keywords, t, name, initializer, posn);
	}

	public <A,R> R visit(Visitor<A,R> v, A o) {
		return v.visitFieldDecl(this, o);
	}

	@Override
	public MemberType getMemberType() {
		return MemberType.FIELD;
	}

	@Override
	public Declaration asDeclaration() {
		return this;
	}
	
	@Override
    public MemberDecl asMemberDecl(){
		return this;
	}

	@Override
	public FieldDecl asField() {
		return this;
	}

	@Override
	public String repr() {
		return "Field declaration at position " + posn;
	}

	
	private ClassMemberDecl enclosing;
	@Override
	public ClassMemberDecl enclosingDecl() {
		return enclosing;
	}

	@Override
	public void setEnclosingDecl(ClassMemberDecl cmd) {
		enclosing = cmd;
	}
}

