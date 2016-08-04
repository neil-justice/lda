/* tracks which files are present in the output directory */
import java.io.*;
import java.nio.file.*;

public class FileTracker {
  private final File dir;
  private final File clean;
  private final File processed;
  private final File database;
  private final File graph;
  private final File infomap;
  private final File lseed;
  
  public FileTracker(String dir) {
    this.dir = new File(dir);
    if (!this.dir.isDirectory()) throw new Error("Not a directory.");
    
    clean = new File(dir + CTUT.CLEANEDFILE);
    processed = new File(dir + CTUT.PROCESSEDFILE);
    database = new File(dir + CTUT.DATABASE);
    graph = new File(dir + CTUT.GRAPH);
    infomap = new File(dir + CTUT.INFOMAP);
    lseed = new File(dir + CTUT.LOUVAIN_SEED);
  }
  
  public boolean isClean() { return clean.exists(); }
  public boolean isProcessed() { return processed.exists(); }
  public boolean isInDB() { return database.exists(); }
  public boolean hasGraph() { return graph.exists(); }
  public boolean hasInfomap() { return infomap.exists(); }
  public boolean hasLouvainSeed() { return lseed.exists(); }
}