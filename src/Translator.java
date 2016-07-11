import java.util.*;
import gnu.trove.map.hash.*;
import gnu.trove.iterator.*;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.list.array.TIntArrayList;

public class Translator {
  private final SQLConnector c;
  private ArrayList<String> words;
  private TLongArrayList docs;
  private TLongIntHashMap docIndexes;
  
  public Translator(SQLConnector c) {
    this.c = c;
  }
  
  public ArrayList<String> getWords() {
    if (words == null) words = c.getWords();
    return words;
  }
  
  public TLongArrayList getDocs() {
    if(docs == null) docs = c.getDocs();
    return docs;
  }
  
  public TLongIntHashMap getDocIndexes() {
    if (docIndexes == null) docIndexes = c.getDocIndexes();
    return docIndexes;
  }
  
  public String getWord(int i) {
    if (words == null) words = c.getWords();
    return words.get(i);
  }
  
  public int getDocIndex(long doc) {
    if (docIndexes == null) docIndexes = c.getDocIndexes();
    return docIndexes.get(doc);
  }
  
  public long getDoc(int i) {
    if (docs == null) docs = c.getDocs();
    return docs.get(i);
  }  
}