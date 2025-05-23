import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.zuomagai.spin.parser.SQLParser;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

public class SqlParserTest {

    public static void main(String[] args) throws JSQLParserException, InterruptedException, ClassNotFoundException {


        //  Class.forName(MySqlLexer.class.getName());
        String sql = "select COUNT(*) from t_user";
        long begin = System.currentTimeMillis();
        SQLParser.parse(sql);
        System.out.println("antlr4 parse: " + (System.currentTimeMillis() - begin) + "ms");

        begin = System.currentTimeMillis();
        CCJSqlParserUtil.parse(sql);
        System.out.println("jsql parser: " + (System.currentTimeMillis() - begin) + "ms");

        begin = System.currentTimeMillis();
        SQLStatement statement = SQLUtils.parseSingleStatement(sql, DbType.oracle, true);
        System.out.println("druid parser: " + (System.currentTimeMillis() - begin) + "ms");
    }
}
