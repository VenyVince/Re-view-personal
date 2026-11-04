#!/usr/bin/env bash
set -eu
export NLS_LANG=AMERICAN_AMERICA.AL32UTF8

sqlplus -s / as sysdba @/measure-sql/perf-user.sql
sqlplus -s MEASURE/measure_local@//localhost:1521/XEPDB1 @/measure-sql/schema.sql
sqlplus -s MEASURE/measure_local@//localhost:1521/XEPDB1 @/measure-sql/fixture.sql
sqlplus -s MEASURE_PERF/measure_local@//localhost:1521/XEPDB1 @/measure-sql/schema.sql
sqlplus -s MEASURE_PERF/measure_local@//localhost:1521/XEPDB1 @/measure-sql/perf.sql
