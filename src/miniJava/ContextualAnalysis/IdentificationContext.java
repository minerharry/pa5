package miniJava.ContextualAnalysis;

import miniJava.AbstractSyntaxTrees.ClassMemberDecl;
import miniJava.AbstractSyntaxTrees.Identifier;

public class IdentificationContext {
    public ClassMemberDecl thisDecl; //for thisref
    public Identifier currentName;
    public boolean isStatic;
    public int staticLevel;
    public boolean allow_type;
    public IdentificationContext(ClassMemberDecl cdl){
        thisDecl = cdl;
    }
}