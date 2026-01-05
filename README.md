# spin
sql parser &amp; db route &amp; rewrite for table sharding

轻量级

## 目的
- 熟悉 sql ast 解析工具
- 熟悉 sql92 标准
- 实践分库分表工具开发， 填充工具箱

- 改写层可扩展：一次解析， 多次改写（分区表/影子表/加密字段）


## sql 支持
[antlr4 mysql语法定义](https://github.com/susongyan/grammars-v4/tree/master/sql/mysql/Positive-Technologies)
mysql 官方文档的语法定义 5.6/5.7/8.0 
- [http://dev.mysql.com/doc/refman/5.6/en/](http://dev.mysql.com/doc/refman/5.6/en/).
- [http://dev.mysql.com/doc/refman/5.7/en/](http://dev.mysql.com/doc/refman/5.7/en/).
- [http://dev.mysql.com/doc/refman/8.0/en/](http://dev.mysql.com/doc/refman/8.0/en/).


antlr4 代码生成：
jdk17
```shell
 cd spin-parser
 mvn antlr4:antlr4
 ```

用 antlr4 写自定义分库分表规则表达式 

benchmark

mvn -pl spin-parser-druid -DskipTests -Dexec.mainClass=com.zuomagai.spin.parser.DruidPerfBenchmark -Dexec.args="--warmup=2000 --iterations=10000 --threads=1" org.codehaus.mojo:exec-maven-plugin:3.1.0:java

```
druid perf benchmark
cases=29 warmup=2000 iterations=10000 threads=1
case=mysql_select_literal db=mysql count=10000 timeMs=56.037 avgUs=5.604 ops/s=178453.2
case=mysql_select_simple db=mysql count=10000 timeMs=46.381 avgUs=4.638 ops/s=215607.7
case=mysql_select_where_params db=mysql count=10000 timeMs=49.533 avgUs=4.953 ops/s=201884.6
case=mysql_select_join_order_limit db=mysql count=10000 timeMs=113.667 avgUs=11.367 ops/s=87976.2
case=mysql_select_group_having db=mysql count=10000 timeMs=67.277 avgUs=6.728 ops/s=148639.7
case=mysql_select_in_list db=mysql count=10000 timeMs=32.012 avgUs=3.201 ops/s=312381.9
case=mysql_select_exists db=mysql count=10000 timeMs=64.314 avgUs=6.431 ops/s=155486.8
case=mysql_select_case db=mysql count=10000 timeMs=46.628 avgUs=4.663 ops/s=214462.2
case=mysql_select_union_all db=mysql count=10000 timeMs=72.527 avgUs=7.253 ops/s=137879.8
case=mysql_select_derived db=mysql count=10000 timeMs=65.235 avgUs=6.524 ops/s=153291.9
case=mysql_insert_values db=mysql count=10000 timeMs=23.966 avgUs=2.397 ops/s=417259.2
case=mysql_insert_values_multi db=mysql count=10000 timeMs=15.318 avgUs=1.532 ops/s=652830.4
case=mysql_insert_select db=mysql count=10000 timeMs=34.860 avgUs=3.486 ops/s=286857.8
case=mysql_update_where db=mysql count=10000 timeMs=41.108 avgUs=4.111 ops/s=243260.3
case=mysql_delete_where db=mysql count=10000 timeMs=16.943 avgUs=1.694 ops/s=590210.6
case=oracle_select_dual db=oracle count=10000 timeMs=79.447 avgUs=7.945 ops/s=125870.5
case=oracle_select_simple db=oracle count=10000 timeMs=44.886 avgUs=4.489 ops/s=222786.3
case=oracle_select_where_params db=oracle count=10000 timeMs=110.928 avgUs=11.093 ops/s=90148.8
case=oracle_select_join_fetch db=oracle count=10000 timeMs=122.335 avgUs=12.234 ops/s=81742.6
case=oracle_select_group_having db=oracle count=10000 timeMs=101.860 avgUs=10.186 ops/s=98173.8
case=oracle_select_in_list db=oracle count=10000 timeMs=30.207 avgUs=3.021 ops/s=331052.4
case=oracle_select_exists db=oracle count=10000 timeMs=65.410 avgUs=6.541 ops/s=152880.9
case=oracle_select_case db=oracle count=10000 timeMs=37.996 avgUs=3.800 ops/s=263186.9
case=oracle_select_union_all db=oracle count=10000 timeMs=39.231 avgUs=3.923 ops/s=254897.9
case=oracle_select_derived db=oracle count=10000 timeMs=38.858 avgUs=3.886 ops/s=257347.6
case=oracle_select_offset_fetch db=oracle count=10000 timeMs=34.973 avgUs=3.497 ops/s=285933.9
case=oracle_insert_values db=oracle count=10000 timeMs=30.736 avgUs=3.074 ops/s=325355.1
case=oracle_update_where db=oracle count=10000 timeMs=26.614 avgUs=2.661 ops/s=375747.1
case=oracle_delete_where db=oracle count=10000 timeMs=8.491 avgUs=0.849 ops/s=1177658.1
total count=290000 timeMs=1517.779 avgUs=5.234 ops/s=191068.7
```