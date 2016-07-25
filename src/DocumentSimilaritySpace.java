/* simplistic trilateration approach to situate docs in space using JS Metric
 * as distance between two points.  Is not incredibly accurate. */
import java.util.*;

public class DocumentSimilaritySpace {
  private final DocumentSimilarityMeasurer simRanker;
  private final CommunityStructure structure;
  private final int docCount;  
  private final Random rnd = new Random();  
  private int b1; // base points with sufficient distance from each other
  private int b2;
  private int b3;
  private double a;  // dist(b1, b2)
  private double b;  // dist(b1, b3)
  private double c;  // dist(b2, b3)
  private double aa; // a*a
  private double bb; // b*b
  private double cc; // c*c
  
  private final double[] x; // coordinates of each document
  private final double[] y; 
  private final double[] z; 
  
  public DocumentSimilaritySpace(CommunityStructure structure) {
    this.structure = structure;
    docCount = structure.docCount();    
    simRanker = new DocumentSimilarityMeasurer(structure);
    x = new double[docCount];
    y = new double[docCount];
    z = new double[docCount];
  }
  
  public void run() {
    findBasePoints();
    setBasePointCoords();
    
    for (int doc = 0; doc < docCount; doc++) {
      if (doc != b1 && doc != b2 && doc != b3) setCoords(doc);
      System.out.println(x[doc] + " " + y[doc] + " " + structure.communities(1)[doc]);
    }   
  }
  
  private void setCoords(int doc) {
    double d = dist(b1, doc);
    double e = dist(b2, doc);
    double f = dist(b3, doc);
    double dd = d*d;
    double ee = e*e;
    double ff = f*f;
    double b3xx = x[b3] * x[b3];
    double b3yy = y[b3] * y[b3];
    
    x[doc] = (dd - ee + aa) / (2d * a);
    y[doc] = ((dd - ff + b3xx + b3yy) / (2d * y[b3])) - ((x[b3]/y[b3]) * x[doc]);
    z[doc] = Math.sqrt(dd - (x[doc] * x[doc]) - (y[doc] * y[doc]));
  }

  // all others are 0 so do not need setting
  private void setBasePointCoords() {
    x[b2] = a;
    x[b3] = (aa + bb - cc) / (2d * a);
    y[b3] = Math.sqrt((a+b+c) * (a+b-c) * (b+c-a) * (c+a-b)) / (2d*a);
  }
  
  // tries to find 3 points with large divergence between all 3 to serve as a 
  // basis for triangularisation of all other points.
  private void findBasePoints() {
    long s1 = System.nanoTime();
    boolean found = false;
    
    while (!found) {
      int p1 = rnd.nextInt(docCount);
      int p2 = rnd.nextInt(docCount);
      int p3 = rnd.nextInt(docCount);
      if (dist(p1, p2) > 0.85
      &&  dist(p2, p3) > 0.85 
      &&  dist(p3, p1) > 0.85) {
        found = true;
        b1 = p1;
        b2 = p2;
        b3 = p3;
        a = dist(b1, b2);
        b = dist(b1, b3);
        c = dist(b2, b3);
        aa = a*a;
        bb = b*b;
        cc = c*c;
      }
    }
    long e1 = System.nanoTime();
    double time = (e1 - s1) / 1000000000d;
    System.out.println("Found base points in: " + time + "s");
  }
  
  // the sqrt is to convert the JSdivergence to the JS metric.
  private double dist(int d1, int d2) { return Math.sqrt(simRanker.JSDistance(d1, d2)); } 
  
  // checks the euclidean distances between the generated coords with the 
  // JS distances
  private double checkDist(int d1, int d2) {
    double x1 = x[d1];
    double x2 = x[d2];
    double y1 = y[d1];
    double y2 = y[d2];
    double z1 = z[d2];
    double z2 = z[d2];
    
    return Math.sqrt((x1-x2)*(x1-x2) + (y1-y2)*(y1-y2) + (z1-z2)*(z1-z2));
  }
  
  public double[] x() { return x; }
  public double[] y() { return y; }
  public double[] z() { return z; }  
}