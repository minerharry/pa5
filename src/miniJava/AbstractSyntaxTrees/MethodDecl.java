/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import java.util.List;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.SyntacticAnalyzer.SourcePosition;

public class MethodDecl extends Declaration implements MemberDecl {

    public MethodDecl(DeclKeywords keywords, List<GenericVar> generics, TypeDenoter rt, Identifier name, List<ParameterDecl> pl, List<Statement> sl, List<TypeDenoter> throwables, SourcePosition posn){
        super(keywords,rt,name,posn);
        this.genericTypes = generics;
        parameters = pl;
        statementList = sl;
        throwsList = throwables;
    }
    	
	public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitMethodDecl(this, o);
    }

    public List<GenericVar> genericTypes;
	public List<ParameterDecl> parameters;
	public List<Statement> statementList;
    public List<TypeDenoter> throwsList;

    @Override
    public MemberType getMemberType() {
        return MemberType.METHOD;
    }

    public boolean isEmpty(){
        return false;
    }

    public boolean isGeneric(){
        return genericTypes != null;
    }

    @Override
    public Declaration asDeclaration() {
        return this;
    }

    @Override
    public MethodDecl asMethod(){
        return this;
    }

    @Override
    public MemberDecl asMemberDecl(){
		return this;
	}

    @Override
    public String repr() {
        return "Method Declaration with name " + name.spelling + "; " + parameters.size() + " parameters, " + statementList.size() + " statements, at position" + posn;
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


    //CODEGEN DECORATION
    public Instruction start;
}
