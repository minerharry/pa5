package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.Token;

public class CharLiteral extends Terminal {

    public CharLiteral(Token t) {
        super(t);
    }

    @Override
    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitCharLiteral(this,o);
    }

    @Override
    public String repr() {
        return "Char literal: " + spelling + " at position " + posn;
    }

    //public char[] escapeChars = {'t','b','n','r','f','\'','\"','\\'};

    public char value(){
        String val = spelling.substring(1,spelling.length()-1);
        if (val.length() > 1){
            //escape sequence
            switch (val.charAt(1)){
                case 't':
                    return '\t';
                case 'b':
                    return '\b';
                case 'n':
                    return '\n';
                case 'r':
                    return '\r';
                case 'f':
                    return '\f';
                case '\\':
                    return '\\';
                case '\"':
                    return '\"';
                case '\'':
                    return '\'';
            }
        }
        return val.charAt(0);
    }
}
