package miniJava.AbstractSyntaxTrees;

import java.util.ArrayList;

import miniJava.SyntacticAnalyzer.ModifierType;

public class ModifierList extends ArrayList<Modifier> {
    public boolean containsType(ModifierType type){
        for(Modifier m : this){
            if (m.type == type){
                return true;
            }
        }
        return false;
    }

    public int indexOfType(ModifierType type){
        for(int i = 0; i < this.size(); i++){
            if (this.get(i).type == type){
                return i;
            }
        }
        return -1;
    }

    public Modifier findType(ModifierType type){
        for(Modifier m : this){
            if (m.type == type){
                return m;
            }
        }
        return null;
    }
}
