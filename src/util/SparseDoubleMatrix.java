/* Sparse matrix using hashmap. */
import gnu.trove.map.hash.TLongDoubleHashMap;
import gnu.trove.iterator.TLongDoubleIterator;
import tester.Tester;

public class SparseDoubleMatrix {
  private final TLongDoubleHashMap map = new TLongDoubleHashMap();
  private final long xmax;
  private final long ymax;
  
  public SparseDoubleMatrix(int xmax, int ymax) {
    this.xmax = (long) xmax;
    this.ymax = (long) ymax;
  }
  
  public double get (int x, int y) { 
    return map.get((long) x + xmax * (long) y); 
  }
  
  public void set (int x, int y, double val) {
    if (x > xmax) throw new Error("x overflow");
    if (y > ymax) throw new Error("y overflow");
    map.put((long) x + xmax * (long) y, val); 
  }
  
  public void add (int x, int y, double val) {
    set(x, y, get(x, y) + val);
  }
  
  public void div (int x, int y, double val) {
    set(x, y, get(x, y) / val);
  }
  
  public int xmax() { return (int) xmax; }
  
  public int ymax() { return (int) ymax; }

  public SparseDoubleMatrix.Iterator iterator() { return new Iterator(); }
  
  public class Iterator {
    private final TLongDoubleIterator iterator = map.iterator();

    public void advance() { iterator.advance(); }
    public boolean hasNext() { return iterator.hasNext(); }
    public double value() { return iterator.value(); }
    public int x() { return (int) (iterator.key() % xmax); }
    public int y() { return (int) (iterator.key() / xmax); }
  }  
  
  public static void main(String[] args) {
    Tester t = new Tester();
    double[][] m1 = new double[3][4];
    SparseDoubleMatrix m2 = new SparseDoubleMatrix(3, 4);
    
    m1[1][2] = 3.0;
    m1[1][2] /= 1.343;
    m1[1][1] = 3.141;
    m1[2][2] = 100203.5;
    m2.set(1, 2, 3.0);
    m2.div(1, 2, 1.343);
    m2.set(1, 1, 3.141);
    m2.set(2, 2, 100203.5);
    
    t.is(m1[1][2], m2.get(1, 2));
    t.is(m1[1][1], m2.get(1, 1));
    t.is(m1[2][2], m2.get(2, 2));
    t.is(m1[0][3], m2.get(0, 3));
    
    t.results();
  }
}