package miniJava.SyntacticAnalyzer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public enum ProtectionType {
    Public("public"),
    Private("private"),
    Protected("protected");

    public String word;

    ProtectionType(String word){
        this.word = word;
    }

    public String getWord(){
        return word;
    }

    public String toString(){
        return getWord();
    }

    public static ProtectionType fromToken(Token t){
        if (t.getTokenType() != TokenType.protection){
            throw new Parser.SyntaxError("Token " + t + " is not a protection",t);
        }
        for (ProtectionType p : ProtectionType.values()){
            if (p.word.equals(t.getTokenText())){
                return p;
            }
        }
        throw new Parser.SyntaxError("Invalid protection level: " + t.getTokenText(),t);
    }

    public static boolean isProtection(String text){
        for (ProtectionType t : values()){
            if (t.getWord().equals(text)){
                return true;
            }
        }
        return false;
    }
}
