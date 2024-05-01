package miniJava;

public class CompilerError extends Error {
    public String message;

    public CompilerError(String message){
        this.message = message;
    }

    public String toString(){
        return this.message;
    }
}