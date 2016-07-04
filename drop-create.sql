DROP TABLE IF EXISTS Info;
DROP TABLE IF EXISTS Token;
DROP TABLE IF EXISTS Doc;
DROP TABLE IF EXISTS Word;

CREATE TABLE Word (
  id INTEGER PRIMARY KEY,
  word VARCHAR(20) NOT NULL
);

CREATE TABLE Doc (
  id INTEGER PRIMARY KEY,
  doc INTEGER NOT NULL
);

CREATE TABLE Token (
  id INTEGER PRIMARY KEY,
  word INTEGER NOT NULL REFERENCES Word(id),
  doc INTEGER NOT NULL REFERENCES Doc(id),
  topic INTEGER NOT NULL
);

CREATE TABLE Info (
  id INTEGER PRIMARY KEY,
  topics INTEGER NOT NULL,
  cycles INTEGER NOT NULL
);

explain query plan
SELECT Word.word AS word, COUNT(word.word) AS cnt
FROM Token INNER JOIN Word ON Word.id = Token.word 
WHERE topic = 1
GROUP BY Word.word 
ORDER BY cnt ASC;

SELECT COUNT(Token.topic) from Token WHERE Token.doc = 1 GROUP BY Token.topic;