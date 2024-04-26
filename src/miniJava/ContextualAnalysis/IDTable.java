package miniJava.ContextualAnalysis;

import java.util.ArrayDeque;

public class IDTable extends ArrayDeque<Scope> {

    public int currentLevel() {
        return size() - 1;
    }

}