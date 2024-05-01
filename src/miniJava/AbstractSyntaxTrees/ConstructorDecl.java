package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.ContextualAnalysis.TypeResult;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class ConstructorDecl extends MethodDecl {

    public ConstructorDecl(DeclKeywords keywords, Identifier name, List<ParameterDecl> pl, List<Statement> sl,
            List<TypeDenoter> throwables, SourcePosition posn) {
        super(keywords, null, TypeResult.VOID.getType(), name, pl, sl, throwables, posn);
        // parameters = pl;
        // statementList = sl;
        // throwsList = throwables;
    }

    @Override
    public <A,R> R visit(Visitor<A,R> v, A o){
        return v.visitConstructorDecl(this,o);
    }

	// public List<ParameterDecl> parameters;
	// public List<Statement> statementList;
    // public List<TypeDenoter> throwsList;
    @Override
    public MemberType getMemberType() {
        return MemberType.CONSTRUCTOR;
    }

    @Override
    public Declaration asDeclaration() {
        return this;
    }

    @Override
    public ConstructorDecl asConstuctor() {
        return this;
    }

    @Override
    public MemberDecl asMemberDecl(){
		return this;
	}

    @Override
    public String repr() {
        return "Constructor Declaration for class " + name.spelling + "; " + parameters.size() + " parameters, " + statementList.size() + " statements, at position" + posn;
    }    
}
