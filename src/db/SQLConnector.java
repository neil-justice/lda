import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import gnu.trove.map.hash.*;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.iterator.*;

public class SQLConnector implements AutoCloseable {
  private final String connection;
  private TableManager tm;
  private Connection c;
  private boolean open; // will only catch intentional closure.
  
  public SQLConnector(String dir) {
    connection = "jdbc:sqlite:" + dir + CTUT.DATABASE;
  }

  public void open() {
    try {
      if (isOpen()) return;
      c = DriverManager.getConnection(connection);
      c.setAutoCommit(false);  // Allow transactions
      open = true;
      tm = new TableManager(c);
      setTempStore(2);
      setSynchronous(1);
    } catch (SQLException e) {
      c = null;
      System.out.println(e.getMessage());
    }
  }
  
  public boolean isOpen() {return open; }

  @Override
  public void close() {
    try {
      if (c == null) { return; }
      c.close();
      c = null;
      open = false;
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
  
  public void createDrop() {
    tm.dropTables();
    initialiseParameters();
    tm.createTables();
  }
  
  // only PRAGMAs which persist should be initialised here.
  private void initialiseParameters() {
    setDefaultCacheSize(100000);
    setPageSize(4096);
  }

  public void buildDocumentDictionary(TLongArrayList documents) {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "INSERT INTO Doc VALUES( ?, ? );";
    final int batchSize = 10000;
    
    try (PreparedStatement s = c.prepareStatement(cmd)) {
      for (int i = 0; i < documents.size(); i++) {
        s.setInt(1, i);
        s.setLong(2, documents.get(i));
        s.addBatch();
        if ( i % batchSize == 0 && i > 0) {
          s.executeBatch();
          c.commit();
        }
      }
      s.executeBatch();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
  
  public void buildWordDictionary(TObjectIntHashMap<String> words) {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "INSERT INTO Word VALUES( ?, ? );";
    final int batchSize = 10000;
    
    try (PreparedStatement s = c.prepareStatement(cmd)) {
      int i = 0;
      for ( TObjectIntIterator<String> it = words.iterator(); it.hasNext(); i++) {
        it.advance();
        s.setInt(1, it.value());
        s.setString(2, it.key());
        s.addBatch();
        if ( i % batchSize == 0 && i > 0) {
          s.executeBatch();
          c.commit();
        }
      }
      s.executeBatch();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
  
  public void buildTokenList(Tokens tokens) {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "INSERT INTO Token VALUES( ?, ?, ? );";
    final int batchSize = 100000;
    try (PreparedStatement s = c.prepareStatement(cmd)) {
      long b = System.nanoTime();
      for (int i = 0; i < tokens.size(); i++) {
        s.setInt(1, i);
        s.setInt(2, tokens.word(i));
        s.setInt(3, tokens.doc(i));
        s.addBatch();
        if ( i % batchSize == 0 && i > 0) {
          s.executeBatch();
          c.commit();
        }
      }
      s.executeBatch();
      c.commit();
      long e = System.nanoTime();
      System.out.printf("DB written: seconds taken: %.01f%n", (e - b) / 1000000000d );
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
  
  public void writePhi(double[][] phi) {
    if (c == null) { throw new IllegalStateException(); }
    tm.dropPhiTable();
    tm.createPhiTable();
    final String cmd = "INSERT INTO Phi VALUES( null, ?, ?, ? );";
    final int height = phi.length;
    final int width = phi[0].length;
    
    try (PreparedStatement s = c.prepareStatement(cmd)) {
      for (int word = 0; word < height; word++) {
        for (int topic = 0; topic < width; topic++) {
          s.setInt(1, word);
          s.setInt(2, topic);
          s.setDouble(3, phi[word][topic]);
          s.addBatch();
        }
      }
      s.executeBatch();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }    
  }
  
  public void writeTheta(double[][] theta) {
    if (c == null) { throw new IllegalStateException(); }
    tm.dropThetaTable();
    tm.createThetaTable();
    final String cmd = "INSERT INTO Theta VALUES( null, ?, ?, ? );";
    final int height = theta.length;
    final int width = theta[0].length;
    
    try (PreparedStatement s = c.prepareStatement(cmd)) {
      for (int topic = 0; topic < height; topic++) {
        for (int doc = 0; doc < width; doc++) {
          s.setInt(1, topic);
          s.setInt(2, doc);
          s.setDouble(3, theta[topic][doc]);
          s.addBatch();
        }
      }
      s.executeBatch();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }    
  }
  
  public double[][] getTheta() {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "SELECT * FROM Theta";
    int topicCount = getTopics();
    int docCount = getCount("Doc");
    double[][] theta = new double[topicCount][docCount];
    
    try (PreparedStatement s = c.prepareStatement(cmd)) {
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) {
          int topic = r.getInt("topic");
          int doc = r.getInt("doc");
          theta[topic][doc] = r.getDouble("val");
        }
        return theta;
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      throw new RuntimeException(e);
    }
  }
  
  public double[][] getPhi() {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "SELECT * FROM Phi";
    int wordCount = getCount("Word");
    int topicCount = getTopics();
    double[][] phi = new double[wordCount][topicCount];
    
    try (PreparedStatement s = c.prepareStatement(cmd)) {
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) {
          int word = r.getInt("word");
          int topic = r.getInt("topic");
          phi[word][topic] = r.getDouble("val");
        }
        return phi;
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      throw new RuntimeException(e);
    }
  }  
  
  public Tokens getTokens() {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "SELECT * FROM Token";
    int cnt = 0;
    Tokens tokens = new Tokens(getCount("Token"));
    
    try (PreparedStatement s = c.prepareStatement(cmd)) {
      try (ResultSet r = s.executeQuery()) {
        long b = System.nanoTime();
        while (r.next()) {
          tokens.add(r.getInt("word"), r.getInt("doc"));
          cnt++;
        }
        long e = System.nanoTime();
        System.out.printf("DB loaded: seconds taken: %.01f%n", (e - b) / 1000000000d );
        return tokens;
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      throw new RuntimeException(e);
    }
  }
  
  public ArrayList<String> getWords() {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "SELECT * FROM Word";
    ArrayList<String> words = new ArrayList<String>();

    try (PreparedStatement s = c.prepareStatement(cmd)) {
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) {
          words.add(r.getString("word"));
        }
        return words;
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      throw new RuntimeException(e);
    }
  }
  
  public TLongArrayList getDocs() {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "SELECT * FROM Doc";
    TLongArrayList docs = new TLongArrayList();

    try (PreparedStatement s = c.prepareStatement(cmd)) {
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) {
          docs.add(r.getLong("doc"));
        }
        return docs;
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      throw new RuntimeException(e);
    }
  }
  
  public TLongIntHashMap getDocIndexes() {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "SELECT * FROM Doc";
    TLongIntHashMap docs = new TLongIntHashMap();

    try (PreparedStatement s = c.prepareStatement(cmd)) {
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) {
          docs.put(r.getLong("doc"), r.getInt("id"));
        }
        return docs;
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      throw new RuntimeException(e);
    }
  }  
  
  public int getCount(String table) {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "SELECT MAX(id) AS cnt FROM " + table;

    try ( PreparedStatement s = c.prepareStatement(cmd)) {
      try(ResultSet r = s.executeQuery()) {
        return r.getInt("cnt") + 1;
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      throw new RuntimeException(e);
    }
  }
  
  public void setCycles(int val) { setInfo("cycles", val); }
  public void setTopics(int val) { setInfo("topics", val); }
  
  private void setInfo(String col, int val) {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "UPDATE Info SET " + col + " = ? WHERE Info.id = 0 ;";
    try (PreparedStatement s = c.prepareStatement(cmd)) {
      s.setInt(1, val);
      s.executeUpdate();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
  
  public int getCycles() { return getInfo("cycles"); }
  public int getTopics() { return getInfo("topics"); }
  
  private int getInfo(String col) {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "SELECT " + col + " from Info";

    try ( PreparedStatement s = c.prepareStatement(cmd)) {
      try(ResultSet r = s.executeQuery()) {
        return r.getInt(col);
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      throw new RuntimeException(e);
    }
  }
  
  public void setCacheSize(int pages) {
    if (pages < 0) throw new Error("-ve values not allowed here");
    setPragmaValue("cache_size", pages);
  }
  
  public void setDefaultCacheSize(int pages) {
    if (pages < 0) throw new Error("-ve values not allowed here");
    setPragmaValue("default_cache_size", pages);
  }
  
  public void setPageSize(int size) {
    if (size < 0) throw new Error("-ve values not allowed here");
    setPragmaValue("page_size", size);
    vacuum();
  }  
  
  //DEFAULT (0), FILE (1), and MEMORY (2)
  public void setTempStore(int mode) {
    if (mode > 2 || mode < 0) throw new Error("unsupported mode");
    setPragmaValue("temp_store", mode);
  }
  
  public void setSynchronous(int level) {
    if (level > 3 || level < 0) throw new Error("unsupported mode");
    try {
      c.setAutoCommit(true);
      setPragmaValue("synchronous", level);
      c.setAutoCommit(false);
    }
    catch (SQLException e) {
      System.out.println(e.getMessage());
    }    
  }
  
  public int getCacheSize() {
    return getPragmaValue("cache_size");
  }
  
  public int getPageSize() {
    return getPragmaValue("page_size");
  }
  
  public int getTempStore() {
    return getPragmaValue("temp_store");
  }
  
  public int getDefaultCacheSize() {
    return getPragmaValue("default_cache_size", "cache_size");
  }
  
  public int getSynchronous() {
    return getPragmaValue("synchronous");
  }
  
  public void showPragmas() {
    System.out.println("cache size  : " + getCacheSize());
    System.out.println("page size   : " + getPageSize());
    System.out.println("temp store  : " + getTempStore());
    System.out.println("synchronous : " + getSynchronous());
  }

  private void setPragmaValue(String name, int val) {
    if (c == null) { throw new IllegalStateException(); }
    
    try {
      c.prepareStatement("PRAGMA "+ name + "=" + val + ";").executeUpdate();
      c.commit();
    }
    catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
  
  private int getPragmaValue(String name) {
    return getPragmaValue(name, name);
  }
  
  // some pragma column names are different from the var name, e.g.
  // default_cache_size returns a col of title cache_size
  private int getPragmaValue(String name, String col) {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "PRAGMA " + name + ";";

    try ( PreparedStatement s = c.prepareStatement(cmd)) {
      try(ResultSet r = s.executeQuery()) {
        return r.getInt(col);
      }
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      throw new RuntimeException(e);
    }
  }
  
  public void vacuum() {
    try {
      c.setAutoCommit(true);
      c.prepareStatement("VACUUM;").executeUpdate();
      c.setAutoCommit(false);
    }
    catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }  
}
