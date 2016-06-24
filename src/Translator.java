import java.util.*;
import gnu.trove.map.hash.*;
import gnu.trove.iterator.*;
import gnu.trove.list.array.TLongArrayList;

public class Translator {
  
  // is the other way round from the input map:
  private final TIntObjectHashMap<String> words = new TIntObjectHashMap<>();
  private final TLongArrayList documents;

  public Translator(TObjectIntHashMap<String> inmap, 
                    TLongArrayList documents) {
    for ( TObjectIntIterator<String> it = inmap.iterator(); it.hasNext(); ) {
      it.advance();
      words.put(it.value(),it.key());
    }
    this.documents = documents;
  }
  
  public String getWord(int i) { return words.get(i); }
  
  public Long getDoc(int i) { return documents.get(i); }
}