package miniJava.AbstractSyntaxTrees;

import java.util.ArrayList;
import java.util.List;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;
import miniJava.SyntacticAnalyzer.TokenType;

public class OverloadedMethod<MethodType extends MethodDecl> extends MethodDecl {

    //IMPORTANT TO KEEP IN MIND: OverloadedMethod can store both public and private, static and instance.
    //which ones are available should be checked on retrieval
    //the overloaded method itself is given public and static so that it is visible everywhere

    public List<MethodType> methods;
    public OverloadedMethod(MethodType meth, SourcePosition posn) {
        super(null,null,null, meth.name,null,null,null, posn);
        ModifierList mods = new ModifierList();
        mods.add(new Modifier(new Token(TokenType.modifier,"static", posn)));
        keywords = new DeclKeywords(new Protection(new Token(TokenType.protection,"public", posn)),mods,new ArrayList<Annotation>(), posn);
        methods = new ArrayList<MethodType>();
        methods.add(meth);
    }
    public OverloadedMethod(List<MethodType> meths, SourcePosition posn) {
        super(null,null,null, meths.get(0).name,null,null,null, posn);
        ModifierList mods = new ModifierList();
        mods.add(new Modifier(new Token(TokenType.modifier,"static", posn)));
        keywords = new DeclKeywords(new Protection(new Token(TokenType.protection,"public", posn)),mods,new ArrayList<Annotation>(), posn);
        methods = new ArrayList<MethodType>();
        methods = meths;
    }

    public void add(MethodType other){
        if (other instanceof OverloadedMethod) {
            this.methods.addAll(((OverloadedMethod<MethodType>)other).methods);
        } else {
            this.methods.add(other);
        }
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A arg) {
        return v.visitOverloadedMethod(this,arg);
    }

    public OverloadedMethod<MethodType> staticMethods() {
        List<MethodType> statics = new ArrayList<MethodType>();
        for (MethodType m : methods){
            if (m.isStatic()){
                statics.add(m);
            }
        }
        if (statics.size() == 0){
            return null;
        }
        return new OverloadedMethod<MethodType>(statics, posn);
    }
    
}
