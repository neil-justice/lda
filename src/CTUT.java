/* CTUT - Community Topic Usage Tracker.  
 * Uses LDA (Latent dirichlet allocation) to determine the topic of discussion
 * of a set of documents, and the Louvain method of graph community detection to
 * find graph communities. */
import java.util.*;

class CTUT {
  
  public static final String FREQFILE = "wordfreqs.txt";
  public static final String CLEANEDFILE = "clean.txt";
  public static final String PROCESSEDFILE = "processed.txt";
  public static final String DATABASE = "data.db";
  public static final String STOPWORDS = "lists/stopwords.txt";
  public static final String SEARCHTERMS = "lists/searchterms.txt";
  private Interface UI;
  
  public static void main(String[] args) {
    CTUT ct = new CTUT();
    if (args.length > 0 && "-t".equals(args[0])) ct.test();
    ct.run(args);
  }
   
  // use cases: test, clean, load dir
  private void run(String[] args) {
    if (args.length > 0) {
      switch(args[0]) {
        case "-c": 
        case "--clean":
          if (args.length < 2) throw new Error("No file specified.");
          FileManager fm = new FileManager(args[1]);
          clean(fm.inputFile(), fm.outputDirectory());
          break;
        case "-o":
        case "--open":
          if (args.length < 2) throw new Error("No dir specified.");
          DirectoryLoader dl = new DirectoryLoader();
          dl.setDirectory(args[1]);
          UI = new Interface(dl.dir());
          UI.loop();
          break;
        default:
          noArgs();        
      }
    }
    else noArgs();
  }
  
  private void noArgs() {
    UI = new Interface();
    UI.loop();
  }
  
  private void clean(String in, String dir) {
    TextCleaner tc = new TextCleaner();
    tc.clean(in, dir);
  }
  
  private void test() {
    TextCleaner.main(null);
    DirectoryLoader.main(null);
    Tokens.main(null);
    Graph.main(null);
    SparseIntMatrix.main(null);
    SparseDoubleMatrix.main(null);
    System.exit(0);   
  }
}