package miniJava.SyntacticAnalyzer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public enum ModifierType {
    Static("static"), //valid on all members except constructurs, invalid in global, local, or parameter scopes
    Final("final"), //valid on fields and local variables only
    Default("default"), //valid only in an interface before methods (interface methods)
    //allows interfaces to have default method implementations. Otherwise the methods must be abstract.
    Abstract("abstract"); //allowed before all methods and class-type declarations except enum, invalid elsewhere

    //modifiers not included here:
    // - synchronized - don't care about multithreading
    // - native - don't care about JNI
    // - transient - weird, could be implemented just as easily with an annotation
    // - volatile - more multithreading 

    //When are modifiers valid?


    public String word;

    ModifierType(String word){
        this.word = word;
    }

    public String getWord(){
        return word;
    }

    public String toString(){
        return getWord();
    }

    public static ModifierType fromToken(Token t){
        if (t.getTokenType() != TokenType.modifier){
            throw new Parser.SyntaxError("Token " + t + " is not a modifier",t);
        }
        for (ModifierType p : ModifierType.values()){
            if (p.word.equals(t.getTokenText())){
                return p;
            }
        }
        throw new Parser.SyntaxError("Invalid modifier: " + t.getTokenText(),t);
    }
    
    public static boolean isModifier(String text){
        for (ModifierType t : values()){
            if (t.getWord().equals(text)){
                return true;
            }
        }
        return false;
    }
    
}