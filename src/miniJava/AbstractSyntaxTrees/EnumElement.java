package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class EnumElement extends Declaration implements MemberDecl {

    public ExprList args;
    public EnumType parent;

    public EnumElement(DeclKeywords keywords, EnumType parent, Identifier name) {
        super(keywords,parent,name,name.posn);
        args = new ExprList();
    }

    public EnumElement(DeclKeywords keywords, EnumType parent, Identifier name, ExprList args){
        super(keywords,parent,name,name.posn);
        this.args = args;
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        return v.visitEnumElement(this,arg);
    }

    @Override
    public MemberType getMemberType() {
        return MemberType.ENUM;
    }

    @Override
    public Declaration asDeclaration() {
        return this;
    }

    @Override
    public EnumElement asEnum(){
        return this;
    }

    @Override
    public String repr() {
        return "Enum Element " + name.spelling + " declaration at position " + posn;
    }

	@Override
	public ClassMemberDecl enclosingDecl() {
		return parent.parent;
	}

	@Override
	public void setEnclosingDecl(ClassMemberDecl cmd) {
		throw new UnsupportedOperationException("nuh uh");
	}
}
