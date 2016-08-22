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
  private final File lpart;
  private final File jspart;
  
  public FileTracker(String dir) {
    this.dir = new File(dir);
    if (!this.dir.isDirectory()) throw new Error("Not a directory.");
    
    clean = new File(dir + CTUT.CLEANEDFILE);
    processed = new File(dir + CTUT.PROCESSEDFILE);
    database = new File(dir + CTUT.DATABASE);
    graph = new File(dir + CTUT.GRAPH);
    infomap = new File(dir + CTUT.INFOMAP);
    lpart = new File(dir + CTUT.LOUVAIN_PARTITION_SET);
    jspart = new File(dir + CTUT.JS_PARTITION_SET);
  }
  
  public boolean isClean() { return clean.exists(); }
  public boolean isProcessed() { return processed.exists(); }
  public boolean isInDB() { return database.exists(); }
  public boolean hasGraph() { return graph.exists(); }
  public boolean hasInfomap() { return infomap.exists(); }
  public boolean hasLouvainPartInfo() { return lpart.exists(); }
  public boolean hasJSPartInfo() { return jspart.exists(); }
}