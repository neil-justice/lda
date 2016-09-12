#!/bin/bash
FILE='data/largest-subgraph-statuses.csv'
DB='out/largest-subgraph-15/data.db'

function TOPDOCS {
  ARRAY=($('sqlite3.exe' "$DB" "select doc from doc where doc.id = $1 ;"))
  echo $1
  for DOCID in "${ARRAY[@]}"
  do
    sed -n '/^'$DOCID'\t/p' $FILE | cut -c 1-500
  done
}
if [ "$1" == "" ]; then
  for TOPIC in `seq 0 14`;
  do
    TOPDOCS $TOPIC
  done  
else
  TOPDOCS $1
fi