import java.io.*;
import java.nio.file.*;

public class FileTracker {
  private final File dir;
  private final File clean;
  private final File processed;
  private final File database;
  
  public FileTracker(String dir) {
    this.dir = new File(dir);
    if (!this.dir.isDirectory()) throw new Error("Not a directory.");
    
    clean = new File(dir + CTUT.CLEANEDFILE);
    processed = new File(dir + CTUT.PROCESSEDFILE);
    database = new File(dir + CTUT.DATABASE);
  }
  
  public boolean isClean() { return clean.exists(); }
  public boolean isProcessed() { return processed.exists(); }
  public boolean isInDB() { return database.exists(); }
}