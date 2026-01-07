package com.zuomagai.spin.sqlir.druid;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DruidPerfBenchmark {
    private record SqlCase(String name, DbType dbType, String sql) {
    }

    public static void main(String[] args) throws InterruptedException {
        int warmup = getIntArg(args, "--warmup", 2000);
        int iterations = getIntArg(args, "--iterations", 10000);
        int threads = getIntArg(args, "--threads", 1);

        List<SqlCase> cases = buildCases();
        System.out.println("druid perf benchmark");
        System.out.println("cases=" + cases.size()
                + " warmup=" + warmup
                + " iterations=" + iterations
                + " threads=" + threads);

        if (threads <= 1) {
            runSingleThread(cases, warmup, iterations);
        } else {
            runConcurrent(cases, warmup, iterations, threads);
        }
    }

    private static void runSingleThread(List<SqlCase> cases, int warmup, int iterations) {
        long totalOps = 0;
        long totalNs = 0;
        for (SqlCase sqlCase : cases) {
            warmup(sqlCase, warmup);
            long elapsedNs = runCase(sqlCase, iterations);
            totalOps += iterations;
            totalNs += elapsedNs;
            printCaseResult(sqlCase, iterations, elapsedNs);
        }
        printTotal(totalOps, totalNs);
    }

    private static void runConcurrent(List<SqlCase> cases, int warmup, int iterations, int threads)
            throws InterruptedException {
        warmupMixed(cases, warmup);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            executor.execute(() -> {
                ready.countDown();
                try {
                    start.await();
                    SQLStatement last = null;
                    for (int loop = 0; loop < iterations; loop++) {
                        for (SqlCase sqlCase : cases) {
                            last = parse(sqlCase);
                        }
                    }
                    if (last == null) {
                        throw new IllegalStateException("No statements parsed");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    e.printStackTrace(System.err);
                } finally {
                    done.countDown();
                }
            });
        }
        ready.await();
        long startNs = System.nanoTime();
        start.countDown();
        done.await();
        long elapsedNs = System.nanoTime() - startNs;
        executor.shutdownNow();

        long totalOps = (long) iterations * cases.size() * threads;
        System.out.println("mixed concurrency run");
        printTotal(totalOps, elapsedNs);
    }

    private static void warmup(SqlCase sqlCase, int warmup) {
        for (int i = 0; i < warmup; i++) {
            parse(sqlCase);
        }
    }

    private static void warmupMixed(List<SqlCase> cases, int warmup) {
        for (int i = 0; i < warmup; i++) {
            for (SqlCase sqlCase : cases) {
                parse(sqlCase);
            }
        }
    }

    private static long runCase(SqlCase sqlCase, int iterations) {
        SQLStatement last = null;
        long startNs = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            last = parse(sqlCase);
        }
        long elapsedNs = System.nanoTime() - startNs;
        if (last == null) {
            throw new IllegalStateException("No statements parsed");
        }
        return elapsedNs;
    }

    private static SQLStatement parse(SqlCase sqlCase) {
        return SQLUtils.parseSingleStatement(sqlCase.sql(), sqlCase.dbType(), true);
    }

    private static void printCaseResult(SqlCase sqlCase, int iterations, long elapsedNs) {
        double avgUs = elapsedNs / 1000.0 / iterations;
        double ops = iterations / (elapsedNs / 1_000_000_000.0);
        System.out.printf(Locale.US,
                "case=%s db=%s count=%d timeMs=%.3f avgUs=%.3f ops/s=%.1f%n",
                sqlCase.name(), sqlCase.dbType(), iterations, elapsedNs / 1_000_000.0, avgUs, ops);
    }

    private static void printTotal(long totalOps, long elapsedNs) {
        double avgUs = elapsedNs / 1000.0 / totalOps;
        double ops = totalOps / (elapsedNs / 1_000_000_000.0);
        System.out.printf(Locale.US,
                "total count=%d timeMs=%.3f avgUs=%.3f ops/s=%.1f%n",
                totalOps, elapsedNs / 1_000_000.0, avgUs, ops);
    }

    private static int getIntArg(String[] args, String key, int defaultValue) {
        for (String arg : args) {
            if (arg.startsWith(key + "=")) {
                String value = arg.substring(key.length() + 1);
                return Integer.parseInt(value);
            }
        }
        return defaultValue;
    }

    private static List<SqlCase> buildCases() {
        List<SqlCase> cases = new ArrayList<>();
        cases.add(new SqlCase("mysql_select_literal", DbType.mysql, "SELECT 1"));
        cases.add(new SqlCase("mysql_select_simple", DbType.mysql, "SELECT * FROM users"));
        cases.add(new SqlCase("mysql_select_where_params", DbType.mysql,
                "SELECT u.id, u.name FROM users u WHERE u.status = 1 AND u.tenant_id = ?"));
        cases.add(new SqlCase("mysql_select_join_order_limit", DbType.mysql,
                "SELECT u.id, o.total FROM users u JOIN orders o ON u.id = o.user_id "
                        + "WHERE o.total > 100 ORDER BY o.created_at DESC LIMIT 20 OFFSET 40"));
        cases.add(new SqlCase("mysql_select_group_having", DbType.mysql,
                "SELECT u.id, COUNT(*) AS cnt FROM users u LEFT JOIN orders o ON u.id = o.user_id "
                        + "GROUP BY u.id HAVING COUNT(*) > 1"));
        cases.add(new SqlCase("mysql_select_in_list", DbType.mysql,
                "SELECT u.id FROM users u WHERE u.id IN (1, 2, 3, 4)"));
        cases.add(new SqlCase("mysql_select_exists", DbType.mysql,
                "SELECT u.id FROM users u WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id)"));
        cases.add(new SqlCase("mysql_select_case", DbType.mysql,
                "SELECT CASE WHEN u.status = 1 THEN 'A' ELSE 'B' END AS s FROM users u"));
        cases.add(new SqlCase("mysql_select_union_all", DbType.mysql,
                "SELECT u.id FROM users u UNION ALL SELECT a.id FROM users_archive a"));
        cases.add(new SqlCase("mysql_select_derived", DbType.mysql,
                "SELECT t.id FROM (SELECT id FROM users WHERE status = 1) t WHERE t.id > 100"));
        cases.add(new SqlCase("mysql_insert_values", DbType.mysql,
                "INSERT INTO users(id, name, tenant_id) VALUES (1, 'alice', 10)"));
        cases.add(new SqlCase("mysql_insert_values_multi", DbType.mysql,
                "INSERT INTO users(id, name) VALUES (1, 'alice'), (2, 'bob')"));
        cases.add(new SqlCase("mysql_insert_select", DbType.mysql,
                "INSERT INTO users(id, name) SELECT id, name FROM users_archive WHERE tenant_id = 10"));
        cases.add(new SqlCase("mysql_update_where", DbType.mysql,
                "UPDATE users SET name = 'bob', updated_at = NOW() WHERE id = 1"));
        cases.add(new SqlCase("mysql_delete_where", DbType.mysql,
                "DELETE FROM users WHERE id = 1"));

        cases.add(new SqlCase("oracle_select_dual", DbType.oracle, "SELECT 1 FROM dual"));
        cases.add(new SqlCase("oracle_select_simple", DbType.oracle, "SELECT * FROM users"));
        cases.add(new SqlCase("oracle_select_where_params", DbType.oracle,
                "SELECT u.id, u.name FROM users u WHERE u.status = 1 AND u.tenant_id = :tenantId"));
        cases.add(new SqlCase("oracle_select_join_fetch", DbType.oracle,
                "SELECT u.id, o.total FROM users u JOIN orders o ON u.id = o.user_id "
                        + "WHERE o.total > 100 ORDER BY o.created_at DESC FETCH FIRST 20 ROWS ONLY"));
        cases.add(new SqlCase("oracle_select_group_having", DbType.oracle,
                "SELECT u.id, COUNT(*) cnt FROM users u LEFT JOIN orders o ON u.id = o.user_id "
                        + "GROUP BY u.id HAVING COUNT(*) > 1"));
        cases.add(new SqlCase("oracle_select_in_list", DbType.oracle,
                "SELECT u.id FROM users u WHERE u.id IN (1, 2, 3, 4)"));
        cases.add(new SqlCase("oracle_select_exists", DbType.oracle,
                "SELECT u.id FROM users u WHERE EXISTS (SELECT 1 FROM orders o WHERE o.user_id = u.id)"));
        cases.add(new SqlCase("oracle_select_case", DbType.oracle,
                "SELECT CASE WHEN u.status = 1 THEN 'A' ELSE 'B' END AS s FROM users u"));
        cases.add(new SqlCase("oracle_select_union_all", DbType.oracle,
                "SELECT u.id FROM users u UNION ALL SELECT a.id FROM users_archive a"));
        cases.add(new SqlCase("oracle_select_derived", DbType.oracle,
                "SELECT t.id FROM (SELECT id FROM users WHERE status = 1) t WHERE t.id > 100"));
        cases.add(new SqlCase("oracle_select_offset_fetch", DbType.oracle,
                "SELECT u.id FROM users u ORDER BY u.id OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY"));
        cases.add(new SqlCase("oracle_insert_values", DbType.oracle,
                "INSERT INTO users(id, name, tenant_id) VALUES (1, 'alice', 10)"));
        cases.add(new SqlCase("oracle_update_where", DbType.oracle,
                "UPDATE users SET name = 'bob' WHERE id = 1"));
        cases.add(new SqlCase("oracle_delete_where", DbType.oracle,
                "DELETE FROM users WHERE id = 1"));
        return cases;
    }
}
