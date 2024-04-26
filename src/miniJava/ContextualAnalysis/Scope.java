package miniJava.ContextualAnalysis;

import java.util.HashMap;

import miniJava.AbstractSyntaxTrees.ClassMemberDecl;
import miniJava.AbstractSyntaxTrees.Declaration;
import miniJava.AbstractSyntaxTrees.MemberDecl.MemberType;

public class Scope {

    public int wow;
    public boolean global;

    private HashMap<String,ClassMemberDecl> typeMap;
    private HashMap<String,Declaration> varMap;
    public Scope(boolean isGlobal){
        super();
        global = isGlobal;
        varMap = new HashMap<>();
        typeMap = new HashMap<>();
    }

    public Declaration put(String name, Declaration decl){
        if (decl.isTypeMember()){ //it's a type
            return typeMap.put(name, decl.asMemberDecl().asClassLike());
        } else {
            return varMap.put(name,decl);
        }

    }

    public Declaration get(String spelling, boolean allow_type) {
        Declaration result;
        if (!allow_type){
            //always search variables first
            result = varMap.get(spelling);
            if (result != null){
                return result;
            }
        }

        return typeMap.get(spelling);
    }

}
