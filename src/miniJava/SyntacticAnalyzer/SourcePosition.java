package miniJava.SyntacticAnalyzer;

public class SourcePosition {
    public String file;
    public int line;
    public int col;

    public SourcePosition(String filepath, int lineno, int colno){
        file = filepath;
        line = lineno;
        col = colno;
    }

    public String toString(){
        return shortString(); //potentially to be changed, if you def want just path:line:col, used 
    }

    public String shortString() {
        return "\""+file+"\":"+line+":"+col;
    }
}
