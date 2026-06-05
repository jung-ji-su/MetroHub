package com.metrohub.api.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaUpdater {

    private final DataSource dataSource;

    @EventListener(ApplicationReadyEvent.class)
    public void applyPendingMigrations() {
        try (Connection conn = dataSource.getConnection()) {
            migrateComplaintsTrainInfo(conn);
        } catch (SQLException e) {
            log.error("SchemaUpdater: DB 연결 실패 — {}", e.getMessage());
        }
    }

    private void migrateComplaintsTrainInfo(Connection conn) throws SQLException {
        if (columnExists(conn, "complaints", "train_no")) return;

        String sql =
            "ALTER TABLE complaints " +
            "ADD COLUMN train_no     VARCHAR(50) NULL AFTER station_name, " +
            "ADD COLUMN line_code    VARCHAR(20) NULL AFTER train_no, " +
            "ADD COLUMN line_name    VARCHAR(50) NULL AFTER line_code, " +
            "ADD COLUMN direction    VARCHAR(50) NULL AFTER line_name, " +
            "ADD COLUMN destination  VARCHAR(50) NULL AFTER direction";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("SchemaUpdater: complaints 테이블에 열차정보 컬럼 5개 추가 완료");
        } catch (SQLException e) {
            log.error("SchemaUpdater: ALTER TABLE 실패 — {}", e.getMessage());
        }
    }

    private boolean columnExists(Connection conn, String table, String column) throws SQLException {
        String sql =
            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}
