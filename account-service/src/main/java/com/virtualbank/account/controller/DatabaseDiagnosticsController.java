package com.virtualbank.account.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system")
public class DatabaseDiagnosticsController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DatabaseDiagnosticsController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/db-schema-info")
    public ResponseEntity<Map<String, Object>> getSchemaInfo() {
        Map<String, Object> info = new HashMap<>();

        try {
            info.put("url", jdbcTemplate.getDataSource().getConnection().getMetaData().getURL());

            List<Map<String, Object>> schemas = jdbcTemplate.queryForList(
                    "SELECT schema_name FROM information_schema.schemata");
            info.put("schemas", schemas);

            List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'");
            info.put("tables", tables);

            try {
                List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                        "SELECT column_name, data_type FROM information_schema.columns " +
                                "WHERE table_name = 'accounts' AND table_schema = 'public'");
                info.put("accountsTableColumns", columns);
            } catch (Exception e) {
                info.put("accountsTableError", e.getMessage());
            }

            return ResponseEntity.ok(info);
        } catch (Exception e) {
            info.put("error", e.getMessage());
            return ResponseEntity.status(500).body(info);
        }
    }
    @GetMapping("/account-check")
    public ResponseEntity<Map<String, Object>> checkAccounts() {
        Map<String, Object> info = new HashMap<>();

        try {
            Integer accountCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM accounts", Integer.class);
            info.put("accountCount", accountCount);

            if (accountCount > 0) {
                List<Map<String, Object>> accounts = jdbcTemplate.queryForList(
                        "SELECT * FROM accounts LIMIT 10");
                info.put("accounts", accounts);
            }

            String specificId = "4958f017-b452-41b8-be6b-6568e118db25";
            Integer specificCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM accounts WHERE account_id::text = ?",
                    Integer.class, specificId);
            info.put("specificAccountExists", specificCount > 0);

            return ResponseEntity.ok(info);
        } catch (Exception e) {
            info.put("error", e.getMessage());
            return ResponseEntity.status(500).body(info);
        }
    }

}