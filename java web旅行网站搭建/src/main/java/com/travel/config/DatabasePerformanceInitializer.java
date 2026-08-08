package com.travel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabasePerformanceInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabasePerformanceInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabasePerformanceInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexes() {
        ensureIndex("scenic", "idx_scenic_rating_view",
                "CREATE INDEX idx_scenic_rating_view ON scenic (rating, view_count)");
        ensureIndex("scenic", "idx_scenic_view_count",
                "CREATE INDEX idx_scenic_view_count ON scenic (view_count)");
        ensureIndex("scenic", "idx_scenic_city_rating",
                "CREATE INDEX idx_scenic_city_rating ON scenic (city, rating, view_count)");
        ensureIndex("scenic", "idx_scenic_type_rating",
                "CREATE INDEX idx_scenic_type_rating ON scenic (scenic_type, rating, view_count)");
        ensureIndex("scenic", "idx_scenic_api_id",
                "CREATE INDEX idx_scenic_api_id ON scenic (api_id)");
        ensureIndex("scenic", "idx_scenic_name_city",
                "CREATE INDEX idx_scenic_name_city ON scenic (name, city)");

        ensureIndex("comment", "idx_comment_scenic_status_time",
                "CREATE INDEX idx_comment_scenic_status_time ON comment (scenic_id, status, create_time)");

        ensureIndex("favorite", "idx_favorite_user_time",
                "CREATE INDEX idx_favorite_user_time ON favorite (user_id, create_time)");

        ensureIndex("browse_history", "idx_browse_user_scenic_time",
                "CREATE INDEX idx_browse_user_scenic_time ON browse_history (user_id, scenic_id, browse_time)");
    }

    private void ensureIndex(String tableName, String indexName, String createSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                Integer.class,
                tableName,
                indexName);

        if (count != null && count > 0) {
            return;
        }

        try {
            jdbcTemplate.execute(createSql);
            logger.info("Created database index {} on {}", indexName, tableName);
        } catch (Exception ex) {
            logger.warn("Failed to create database index {} on {}", indexName, tableName, ex);
        }
    }
}
