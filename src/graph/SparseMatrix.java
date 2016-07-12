/* Sparse square matrix using hashmap. */
import gnu.trove.map.hash.TLongIntHashMap;
import tester.Tester;

public class SparseMatrix {
  private final TLongIntHashMap map = new TLongIntHashMap();
  private final long size;
  
  public SparseMatrix(int size) {
    this.size = (long) size;
  }
  
  public int get (int x, int y) { 
    long lx = (long) x;
    long ly = (long) y;
    return map.get(lx * size + ly); 
  }
  public void set (int x, int y, int val) { 
    long lx = (long) x;
    long ly = (long) y;
    map.put(lx * size + ly, val); 
  }
  public int size() { return (int) size; }
  
  public static void main(String[] args) {
    Tester t = new Tester();
    SparseMatrix m = new SparseMatrix(3);
    
    for (int i = 0; i < m.size(); i++) {
      for (int j = 0; j < m.size(); j++) {
        m.set(i, j, i + j);
      }
    }
    
    for (int i = 0; i < m.size(); i++) {
      for (int j = 0; j < m.size(); j++) {
        t.is(m.get(i, j), i + j);
      }
    }
    
    t.results();
  }
}