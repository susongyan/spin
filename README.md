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
