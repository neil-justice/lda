#!/bin/bash
FILE='data/largest-subgraph-statuses.csv'
DB='out/largest-subgraph-30/data.db'

function TOPDOCS {
  ARRAY=($('sqlite3.exe' "$DB" "select doc.doc from doc inner join theta on doc.id = theta.doc where topic = $1 order by val desc limit 10;"))
  echo $1
  for DOCID in "${ARRAY[@]}"
  do
    echo $DOCID
    sed -n '/^'$DOCID'\t/p' $FILE | cut -c 1-500
  done
}
if [ "$1" == "" ]; then
  for TOPIC in `seq 0 29`;
  do
    TOPDOCS $TOPIC
  done  
else
  TOPDOCS $1
fi