#!/bin/bash
FILE='data/abstracts-v8.txt'
DB='out/abstracts-v8/data.db'
function TOPDOCS {
  TOPIC=$1
  echo $TOPIC
  ARRAY=($('sqlite3.exe' "$DB" "select doc.doc from doc inner join theta on doc.id = theta.doc where topic = $TOPIC order by val desc limit 10;"))
  for i in "${ARRAY[@]}"
  do
     sed -n '/^'$i'\t/p' $FILE | rev | cut -d "!" -f1 | rev | cut -c 1-160
  done
}
if [ "$1" == "" ]; then
  for TOPIC in `seq 0 19`;
  do
    TOPDOCS $TOPIC
  done  
else
  TOPDOCS $1
fi