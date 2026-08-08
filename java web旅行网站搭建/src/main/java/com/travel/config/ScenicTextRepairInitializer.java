package com.travel.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ScenicTextRepairInitializer {

    private static final Logger logger = LoggerFactory.getLogger(ScenicTextRepairInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;

    public ScenicTextRepairInitializer(
            JdbcTemplate jdbcTemplate,
            @Value("${app.scenic.auto-repair-corrupted-text:true}") boolean enabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void maybeRepairCorruptedScenicText() {
        if (!enabled) {
            return;
        }

        try {
            Integer corruptedCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) "
                            + "FROM scenic "
                            + "WHERE id IN (9,11,12,13,14,15,16,17,18,20,21,22,23,24,25,26,27,28,29,30) "
                            + "AND (name IS NULL OR TRIM(name) = '' "
                            + "OR REPLACE(REPLACE(TRIM(name), '?', ''), '？', '') = '')",
                    Integer.class);

            if (corruptedCount == null || corruptedCount < 10) {
                return;
            }

            int repaired = 0;
            repaired += updateScenic(9, "布达拉宫", "拉萨", "西藏", "布达拉宫是西藏拉萨地标性建筑，集宫殿、宗教与历史文化价值于一体。",
                    "西藏自治区拉萨市城关区北京中路35号", "5A", "人文", "夏秋两季", "09:00-16:00");
            repaired += updateScenic(11, "故宫博物院", "北京", "北京", "故宫博物院是明清两代皇家宫殿遗址，拥有丰富的古建筑与文物馆藏。",
                    "北京市东城区景山前街4号", "5A", "人文", "春秋两季", "08:30-17:00");
            repaired += updateScenic(12, "西湖", "杭州", "浙江", "西湖以湖光山色和深厚人文底蕴著称，是杭州最具代表性的景区。",
                    "浙江省杭州市西湖区龙井路1号", "5A", "自然", "春季三四月", "全天开放");
            repaired += updateScenic(13, "兵马俑", "西安", "陕西", "兵马俑是秦始皇陵的重要组成部分，被誉为世界第八大奇迹。",
                    "陕西省西安市临潼区秦陵北路", "5A", "人文", "春秋两季", "08:30-18:00");
            repaired += updateScenic(14, "张家界国家森林公园", "张家界", "湖南", "张家界国家森林公园以石英砂岩峰林地貌著称，景观独特壮丽。",
                    "湖南省张家界市武陵源区", "5A", "自然", "春秋两季", "07:00-18:00");
            repaired += updateScenic(15, "九寨沟", "阿坝", "四川", "九寨沟以高山海子、彩林和瀑布群闻名，是热门自然景观目的地。",
                    "四川省阿坝藏族羌族自治州九寨沟县漳扎镇", "5A", "自然", "秋季九十月", "07:30-17:00");
            repaired += updateScenic(16, "黄山", "黄山", "安徽", "黄山以奇松、怪石、云海、温泉闻名，被誉为天下第一奇山。",
                    "安徽省黄山市黄山区汤口镇", "5A", "自然", "春夏秋三季", "06:00-17:30");
            repaired += updateScenic(17, "丽江古城", "丽江", "云南", "丽江古城是世界文化遗产，兼具纳西文化与古城水系风貌。",
                    "云南省丽江市古城区", "5A", "人文", "春秋两季", "全天开放");
            repaired += updateScenic(18, "鼓浪屿", "厦门", "福建", "鼓浪屿以万国建筑、海岛风光和音乐文化著称。",
                    "福建省厦门市思明区鼓浪屿", "5A", "人文", "春秋两季", "全天开放");
            repaired += updateScenic(20, "桂林漓江", "桂林", "广西", "漓江山水甲天下，以喀斯特峰丛与江景游览线路闻名。",
                    "广西壮族自治区桂林市灵川县附近", "5A", "自然", "四季皆宜", "08:00-17:00");
            repaired += updateScenic(21, "泰山", "泰安", "山东", "泰山是五岳之首，兼具自然景观与深厚历史文化。",
                    "山东省泰安市泰山区红门路", "5A", "自然", "春秋两季", "06:00-17:30");
            repaired += updateScenic(22, "峨眉山", "乐山", "四川", "峨眉山是著名佛教名山，以山景与寺院文化闻名。",
                    "四川省乐山市峨眉山市黄湾镇", "5A", "自然", "春秋两季", "06:00-17:00");
            repaired += updateScenic(23, "华山", "渭南", "陕西", "华山以险峻山势和经典登山线路著称。",
                    "陕西省渭南市华阴市华山景区", "5A", "自然", "春秋两季", "07:00-19:00");
            repaired += updateScenic(24, "婺源篁岭", "上饶", "江西", "婺源篁岭以晒秋民俗、古村落与梯田花海著称。",
                    "江西省上饶市婺源县江湾镇篁岭村", "4A", "人文", "春秋两季", "07:30-17:30");
            repaired += updateScenic(25, "乌镇", "嘉兴", "浙江", "乌镇是典型江南水乡古镇，白墙黛瓦，小桥流水。",
                    "浙江省嘉兴市桐乡市乌镇镇", "5A", "人文", "四季皆宜", "09:00-22:00");
            repaired += updateScenic(26, "都江堰景区", "成都", "四川", "都江堰是世界著名古代水利工程，兼具人文与自然景观。",
                    "四川省成都市都江堰市公园路", "5A", "人文", "春秋两季", "08:00-17:30");
            repaired += updateScenic(27, "云冈石窟", "大同", "山西", "云冈石窟是中国四大石窟之一，石刻艺术价值极高。",
                    "山西省大同市云冈区云冈镇", "5A", "人文", "春秋两季", "08:30-17:00");
            repaired += updateScenic(28, "青海湖", "海北", "青海", "青海湖是中国最大的内陆咸水湖，以高原湖景和环湖线路闻名。",
                    "青海省海北藏族自治州刚察县附近", "5A", "自然", "夏季", "08:00-18:00");
            repaired += updateScenic(29, "稻城亚丁", "甘孜", "四川", "稻城亚丁以雪山、草甸和海子构成高原景观，被称为香格里拉之魂。",
                    "四川省甘孜藏族自治州稻城县香格里拉镇", "5A", "自然", "秋季", "07:00-17:30");
            repaired += updateScenic(30, "喀纳斯景区", "阿勒泰", "新疆", "喀纳斯以高山湖泊、原始森林和彩色秋景闻名。",
                    "新疆维吾尔自治区阿勒泰地区布尔津县喀纳斯", "5A", "自然", "夏秋两季", "08:00-20:00");

            logger.warn("Detected {} potentially corrupted scenic rows. Repaired {} rows.", corruptedCount, repaired);
        } catch (Exception ex) {
            logger.debug("Skip scenic text repair check: {}", ex.getMessage());
        }
    }

    private int updateScenic(
            int id,
            String name,
            String city,
            String province,
            String content,
            String address,
            String scenicLevel,
            String scenicType,
            String bestSeason,
            String openTime) {
        return jdbcTemplate.update(
                "UPDATE scenic SET name = ?, city = ?, province = ?, content = ?, address = ?, "
                        + "scenic_level = ?, scenic_type = ?, best_season = ?, open_time = ? WHERE id = ?",
                name, city, province, content, address, scenicLevel, scenicType, bestSeason, openTime, id);
    }
}
