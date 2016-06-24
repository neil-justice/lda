import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import gnu.trove.map.hash.*;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.iterator.*;

public class SQLConnector implements AutoCloseable {
  static final String CONNECTION = "jdbc:sqlite:db/dict.db";
  private Connection c;

  public void open() {
    try {
      c = DriverManager.getConnection(CONNECTION);
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

  public void buildDocumentDictionary(TLongArrayList documents) {
    if (c == null) { throw new IllegalStateException(); }
    final String cmd = "INSERT INTO Doc VALUES( ?, ? );";

    try (PreparedStatement s = c.prepareStatement(cmd)) {
      for (int i = 0; i < documents.size(); i++) {
        s.setInt(1, i);
        s.setLong(2, documents.get(i));
        s.addBatch();
        if ( i > 0 && i % 1000 == 0) {
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
        if ( i > 0 && i % 1000 == 0) {
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

  public void setCacheSize(int pages) {
    try {
      c.prepareStatement("PRAGMA cache_size=" + pages + ";").executeUpdate();
    }
    catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
}
