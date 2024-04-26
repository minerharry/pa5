package miniJava.AbstractSyntaxTrees;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class KwargList implements Iterable<Kwarg> {
    public KwargList() {
    	initializerList = new ArrayList<Kwarg>();
    }
    public KwargList(Kwarg... initial) {
    	initializerList = new ArrayList<Kwarg>();
        for (Kwarg i : initial){
            initializerList.add(i);
        }
    }
    
    public void add(Kwarg s){
    	initializerList.add(s);
    }
    
    public Kwarg get(int i){
        return initializerList.get(i);
    }
    
    public int size() {
        return initializerList.size();
    }
    
    public Iterator<Kwarg> iterator() {
    	return initializerList.iterator();
    }
    
    private List<Kwarg> initializerList;

}
