import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TableManager {
  private final Connection c;
  private final String dropi = "DROP TABLE IF EXISTS Info;";
  private final String dropt = "DROP TABLE IF EXISTS Token;";
  private final String dropd = "DROP TABLE IF EXISTS Doc;";
  private final String dropw = "DROP TABLE IF EXISTS Word;";
  private final String word  = "CREATE TABLE Word (" + 
                               "id INTEGER PRIMARY KEY, " +
                               "word VARCHAR(20) NOT NULL);";
  private final String doc   = "CREATE TABLE Doc (" +
                               "id INTEGER PRIMARY KEY, " +
                               "doc INTEGER NOT NULL);";
  private final String token = "CREATE TABLE Token (" +
                               "id INTEGER PRIMARY KEY, " +
                               "word INTEGER NOT NULL REFERENCES Word(id), " +
                               "doc INTEGER NOT NULL REFERENCES Doc(id), " +
                               "topic INTEGER NOT NULL);";
  private final String info  = "CREATE TABLE Info (" +
                               "id INTEGER PRIMARY KEY, " +
                               "topics INTEGER NOT NULL, " +
                               "cycles INTEGER NOT NULL);";
  private final String iinit = "INSERT INTO Info VALUES( 0, 0, 0)";
  
  public TableManager(Connection c) { this.c = c; }
  
  public void createTables() {
    if (c == null) { throw new IllegalStateException(); }

    try (Statement s = c.createStatement()) {
      s.addBatch(word);
      s.addBatch(doc);
      s.addBatch(token);
      s.addBatch(info);
      s.addBatch(iinit);
      s.executeBatch();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
  
  public void dropTables() {
    if (c == null) { throw new IllegalStateException(); }

    try (Statement s = c.createStatement()) {
      s.addBatch(dropi);
      s.addBatch(dropt);
      s.addBatch(dropd);
      s.addBatch(dropw);
      s.executeBatch();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }    
  }
  
  public void dropTokenTable() {
    if (c == null) { throw new IllegalStateException(); }

    try (PreparedStatement s = c.prepareStatement(dropt)) {
      s.executeUpdate();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }       
  }
  
  public void createTokenTable() {
    if (c == null) { throw new IllegalStateException(); }

    try (PreparedStatement s = c.prepareStatement(token)) {
      s.executeUpdate();
      c.commit();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }
}