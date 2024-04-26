package miniJava.ContextualAnalysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import miniJava.AbstractSyntaxTrees.ClassMemberDecl;
import miniJava.AbstractSyntaxTrees.DeclKeywords;
import miniJava.AbstractSyntaxTrees.MemberDecl;
import miniJava.AbstractSyntaxTrees.TypeDenoter;

public class TypeContext { //for methods, etc
    public Stack<ClassMemberDecl> scopes;
    public TypeResult returnType;
    public List<TypeResult> throwTypes;
    public TypeResult switchType = null;
    public TypeResult hintType = null;
    public DeclKeywords memberKeywords;
    
    public TypeContext(TypeResult ret, List<TypeResult> thrower,Stack<ClassMemberDecl> scope) {
        returnType = ret;
        throwTypes = thrower;
        scopes = scope;
    }

    public void addScope(ClassMemberDecl decl){
        scopes.push(decl);
    }

    public ClassMemberDecl popScope(){
        return scopes.pop();
    }

    public TypeContext clone(){
        TypeContext cl = new TypeContext(returnType, throwTypes, (Stack<ClassMemberDecl>)scopes.clone());
        cl.switchType = switchType;
        cl.hintType = hintType;
        return cl;
    }

    public boolean within(MemberDecl other){
        if (other.enclosingDecl() == null){
            throw new UnsupportedOperationException("Member's enclosing class not defined, can't check nested hierarchy");
        }
        for (ClassMemberDecl scope : scopes){
            if (other.enclosingDecl().equals(scope)){
                return true;
            }
        }
        return false;
    }

}
