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
  private Connection c;
  
  public SQLConnector(String dir) {
    connection = "jdbc:sqlite:" + dir + LDA.database;
  }

  public void open() {
    try {
      c = DriverManager.getConnection(connection);
      c.setAutoCommit(false);  // Allow transactions
    } catch (SQLException e) {
      c = null;
      System.out.println(e.getMessage());
    }
  }

  @Override
  public void close() {
    try {
      if (c == null) { return; }
      c.close();
      c = null;
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
  
  public void createDrop() {
    if (c == null) { throw new IllegalStateException(); }
    final String dropt = "DROP TABLE IF EXISTS Token;";
    final String dropd = "DROP TABLE IF EXISTS Doc;";
    final String dropw = "DROP TABLE IF EXISTS Word;";
    final String word  = "CREATE TABLE Word (" + 
                         "id INTEGER PRIMARY KEY, " +
                         "word VARCHAR(20) NOT NULL);";
    final String doc   = "CREATE TABLE Doc (" +
                         "id INTEGER PRIMARY KEY, " +
                         "doc INTEGER NOT NULL);";
    final String token = "CREATE TABLE Token (" +
                         "id INTEGER PRIMARY KEY, " +
                         "word INTEGER NOT NULL REFERENCES Word(id), " +
                         "doc INTEGER NOT NULL REFERENCES Doc(id), " +
                         "topic INTEGER NOT NULL);";

    try (Statement s = c.createStatement()) {
      s.addBatch(dropt);
      s.addBatch(dropd);
      s.addBatch(dropw);
      s.addBatch(word);
      s.addBatch(doc);
      s.addBatch(token);
      s.executeBatch();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }

  public void buildDocumentDictionary(TLongArrayList documents) {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "INSERT INTO Doc VALUES( ?, ? );";

    try (PreparedStatement s = c.prepareStatement(cmd)) {
      for (int i = 0; i < documents.size(); i++) {
        s.setInt(1, i);
        s.setLong(2, documents.get(i));
        s.addBatch();
        if ( i % 1000 == 0 && i > 0) {
          int[] res = s.executeBatch();
          c.commit();
        }
      }
      int[] res = s.executeBatch();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
  
  public void buildWordDictionary(TObjectIntHashMap<String> words) {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "INSERT INTO Word VALUES( ?, ? );";

    try (PreparedStatement s = c.prepareStatement(cmd)) {
      int i = 0;
      for ( TObjectIntIterator<String> it = words.iterator(); it.hasNext(); i++) {
        it.advance();
        s.setInt(1, it.value());
        s.setString(2, it.key());
        s.addBatch();
        if ( i % 1000 == 0 && i > 0) {
          int[] res = s.executeBatch();
          c.commit();
        }
      }
      int[] res = s.executeBatch();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
  
  public void buildTokenList(Tokens tokens) {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "INSERT INTO Token VALUES( ?, ?, ?, ? );";
    
    try (PreparedStatement s = c.prepareStatement(cmd)) {
      for (int i = 0; i < tokens.size(); i++) {
        s.setInt(1, i);
        s.setInt(2, tokens.word(i));
        s.setInt(3, tokens.doc(i));
        s.setInt(4, tokens.topic(i));
        s.addBatch();
        if ( i % 10000 == 0 && i > 0) {
          int[] res = s.executeBatch();
          c.commit();
        }
      }
      int[] res = s.executeBatch();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
  
  public void updateTokens(Tokens tokens) {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "UPDATE Token SET topic = ? WHERE Token.id = ? ;";
    
    try (PreparedStatement s = c.prepareStatement(cmd)) {
      for (int i = 0; i < tokens.size(); i++) {
        s.setInt(1, tokens.topic(i));
        s.setInt(2, i);
        s.addBatch();
        if ( i % 10000 == 0 && i > 0) {
          int[] res = s.executeBatch();
          c.commit();
        }
      }
      int[] res = s.executeBatch();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
  
  public Tokens loadTokens() {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "SELECT * FROM Topic";
    Tokens tokens = new Tokens();

    try (PreparedStatement s = c.prepareStatement(cmd)) {
      try (ResultSet r = s.executeQuery()) {
        while (r.next()) {
          tokens.add(r.getInt("word"), r.getInt("doc"), r.getInt("topic"));
        }
        return 
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
  
  // converts the list into a string of format "(val1, val2, val3 ... valn)"
  // private String buildStringList(String start, TIntArrayList list) {
  //   int i = 0;
  //   StringBuilder sb = new StringBuilder(start);
  //   sb.append("(");
  //   for (int val: list) {
  //     if (i != 0) sb.append(",");
  //     sb.append(val);
  //     i++;
  //   }
  //   sb.append(");");
  //   
  //   return sb.toString();
  // }

  public void setCacheSize(int pages) {
    try {
      c.prepareStatement("PRAGMA cache_size=" + pages + ";").executeUpdate();
    }
    catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
}
