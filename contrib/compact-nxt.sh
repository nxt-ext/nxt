#!/bin/sh
echo ***********************************************************************
echo * This shell script will compact and reorganize the Nxt NRS database. *
echo * This process can take a long time.  Do not interrupt the script     *
echo * or shutdown the computer until it finishes.                         *
echo ***********************************************************************

# HDIR is the Nxt NRS install directory and defaults to the current home directory
HDIR=$HOME

# DBDIR is the Nxt NRS database directory and defaults to the nxt_db subdirectory
DBDIR=$HDIR/nxt_db

cd $HDIR

if [ ! -f $DBDIR/nxt.mv.db -a ! -f $DBDIR/nxt.h2.db ] ; then
  echo "The Nxt NRS database does not exist"
  exit 1
fi

if [ -f $DBDIR/backup.sql.gz ] ; then
  rm $DBDIR/backup.sql.gz
fi

echo "Creating the database SQL script"
java -Xmx768m -cp "lib/*" org.h2.tools.Script -script $DBDIR/backup.sql.gz -url "jdbc:h2:$DBDIR/nxt" -user sa -password 'sa' -options COMPRESSION GZIP
if [ ! -f $DBDIR/backup.sql.gz ] ; then
  echo "Unable to create the database SQL script"
  exit 1
fi

echo "Recreating the Nxt NRS database"
if [ -f $DBDIR/nxt.mv.db ] ; then
  rm $DBDIR/nxt.mv.db
fi
if [ -f $DBDIR/nxt.h2.db ] ; then
  rm $DBDIR/nxt.h2.db
fi
java -Xmx768m -cp "lib/*" org.h2.tools.RunScript -script $DBDIR/backup.sql.gz -url "jdbc:h2:$DBDIR/nxt" -user sa -password 'sa' -options COMPRESSION GZIP
if [ ! -f $DBDIR/nxt.mv.db -a ! -f $DBDIR/nxt.h2.db ] ; then
  echo "Unable to recreate the Nxt NRS database, SQL script is $DBDIR/backup.sql.gz"
  exit 1
fi

echo "Nxt database recreated, SQL script is $DBDIR/backup.sql.gz"
exit 0

