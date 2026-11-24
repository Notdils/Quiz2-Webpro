package com.harvesta;

import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Seeds an empty database on startup (PRD 6.4): admin/staff/viewer accounts, master data,
 * and twelve months of deterministic demo data (fixed random seed) so charts look alive.
 * Safe to run repeatedly: each part only runs when its tables are empty.
 */
public final class Seeder {

    private static final Object[][] CATEGORIES = {
            {"Fruit", "Trees grown mainly for their edible fruit.",
                    new String[][]{{"Durian", "Durio zibethinus"}, {"Mango", "Mangifera indica"},
                            {"Rambutan", "Nephelium lappaceum"}, {"Avocado", "Persea americana"}}},
            {"Timber", "Trees grown for wood and long-term investment.",
                    new String[][]{{"Teak", "Tectona grandis"}, {"Mahogany", "Swietenia macrophylla"},
                            {"Albizia", "Falcataria falcata"}}},
            {"Ornamental", "Trees planted for shade and landscaping.",
                    new String[][]{{"Frangipani", "Plumeria rubra"}, {"Flame Tree", "Delonix regia"}}},
            {"Spice", "Trees that produce spices.",
                    new String[][]{{"Clove", "Syzygium aromaticum"}, {"Nutmeg", "Myristica fragrans"}}},
    };

    private static final Object[][] BLOCKS = {
            {"North Block", "12500.00", "Next to the main gate.", "Mostly mature fruit trees."},
            {"East Block", "9800.00", "On the slope behind the storage shed.", "Mixed timber and spice trees."},
            {"River Block", "7200.50", "Along the irrigation canal.", "Young trees and new plantings."},
    };

    /** Unit and price range (IDR per unit) for each harvestable tree type. */
    private static final Map<String, Object[]> HARVESTABLE = Map.of(
            "Durian", new Object[]{"pcs", 45000, 90000},
            "Mango", new Object[]{"kg", 15000, 25000},
            "Rambutan", new Object[]{"kg", 10000, 18000},
            "Avocado", new Object[]{"kg", 20000, 35000},
            "Clove", new Object[]{"kg", 90000, 130000},
            "Nutmeg", new Object[]{"kg", 70000, 110000});

    private static final Object[][] EXPENSES = {
            {"fertilizer", 300000, 2000000, "NPK and organic compost"},
            {"pesticide", 150000, 1000000, "Insecticide spray"},
            {"labor", 1000000, 3500000, "Weekly wages for field workers"},
            {"equipment", 200000, 5000000, "Pruning shears and ladders"},
            {"water", 100000, 600000, "Irrigation pump fuel and maintenance"},
            {"transport", 100000, 800000, "Delivery to the wholesale market"},
            {"other", 50000, 500000, "Miscellaneous supplies"},
    };

    private static final String[] BUYERS = {
            "Fresh Harvest Market", "Green Valley Grocers", "Sunrise Fruit Stall",
            "Nusantara Spice Traders", "City Juice Bar", "Local Cooperative",
    };

    private final Random random = new Random(20261028);
    private final LocalDate today = LocalDate.now();
    private List<Long> userIds;

    private Seeder() {
    }

    public static void run() {
        if (Db.count("select count(*) from users") == 0) {
            Db.tx(() -> {
                account("ADMIN", "Admin", "admin@example.com", "admin");
                account("STAFF", "Field Staff", "staff@example.com", "staff");
                account("VIEWER", "Guest Viewer", "viewer@example.com", "viewer");
                return null;
            });
        }
        if (Db.count("select count(*) from categories") == 0) {
            Db.tx(() -> {
                masterData();
                return null;
            });
        }
        if (Env.flag("SEED_DEMO_DATA", true) && Db.count("select count(*) from trees") == 0) {
            Db.tx(() -> {
                new Seeder().demoData();
                return null;
            });
        }
    }

    private static void account(String key, String name, String email, String role) {
        String password = Env.get("SEED_" + key + "_PASSWORD", null);
        if (password == null) {
            if (Env.production()) {
                throw new IllegalStateException("Set SEED_" + key + "_PASSWORD before seeding in production.");
            }
            password = "password";
        }
        Db.update("insert into users (name, email, password, role) values (?, ?, ?, ?)",
                Env.get("SEED_" + key + "_NAME", name), Env.get("SEED_" + key + "_EMAIL", email).toLowerCase(),
                BCrypt.hashpw(password, BCrypt.gensalt()), role);
    }

    private static void masterData() {
        for (Object[] category : CATEGORIES) {
            long id = Db.insert("insert into categories (name, description) values (?, ?) returning id",
                    category[0], category[1]);
            for (String[] type : (String[][]) category[2]) {
                Db.update("insert into tree_types (category_id, name, scientific_name) values (?, ?, ?)",
                        id, type[0], type[1]);
            }
        }
        for (Object[] block : BLOCKS) {
            Db.update("insert into blocks (name, area_sqm, location_note, description) values (?, ?, ?, ?)",
                    block[0], new BigDecimal((String) block[1]), block[2], block[3]);
        }
    }

    private record SeedTree(long id, String type, String status) {
    }

    private void demoData() {
        userIds = new ArrayList<>();
        Db.list("select id from users where role in ('admin', 'staff') order by id")
                .forEach(u -> userIds.add(((Number) u.get("id")).longValue()));

        List<SeedTree> trees = seedTrees();
        List<SeedTree> living = trees.stream().filter(t -> !t.status.equals("dead")).toList();

        // 88 past activities without a follow-up, then 12 recent ones that feed "Upcoming Care".
        for (int i = 0; i < 88; i++) {
            careLog(pick(living), today.minusDays(between(31, 360)), null);
        }
        for (int due : new int[]{-4, -2, -1, 0, 1, 2, 3, 5, 7, 12, 18, 25}) {
            careLog(pick(living), today.minusDays(between(3, 25)), today.plusDays(due));
        }

        List<SeedTree> productive = trees.stream()
                .filter(t -> t.status.equals("productive") && HARVESTABLE.containsKey(t.type)).toList();
        List<Object[]> harvests = new ArrayList<>();
        String[] grades = {"premium", "good", "good", "good", "fair", "poor"};
        for (int i = 0; i < 40; i++) {
            SeedTree tree = productive.get(i % productive.size());
            String unit = (String) HARVESTABLE.get(tree.type)[0];
            BigDecimal quantity = unit.equals("pcs") ? BigDecimal.valueOf(between(10, 60)) : decimal(20, 150);
            LocalDate harvestedOn = dayInMonth(i % 12);
            long id = Db.insert("insert into harvests (tree_id, harvested_on, quantity, unit, grade, user_id) "
                            + "values (?, ?, ?, ?, ?, ?) returning id",
                    tree.id, harvestedOn, quantity, unit, grades[random.nextInt(grades.length)], pick(userIds));
            harvests.add(new Object[]{id, tree, quantity, harvestedOn});
        }

        // Each sale sells part of a different harvest, so no harvest is oversold.
        Collections.shuffle(harvests, random);
        for (Object[] harvest : harvests.subList(0, 20)) {
            Object[] info = HARVESTABLE.get(((SeedTree) harvest[1]).type);
            BigDecimal share = ((BigDecimal) harvest[2]).multiply(decimal(0.3, 0.9));
            BigDecimal quantity = info[0].equals("pcs")
                    ? share.setScale(0, RoundingMode.FLOOR).max(BigDecimal.ONE)
                    : share.setScale(2, RoundingMode.FLOOR);
            BigDecimal price = BigDecimal.valueOf(Math.round(between((int) info[1], (int) info[2]) / 100.0) * 100);
            LocalDate soldOn = ((LocalDate) harvest[3]).plusDays(between(0, 5));
            Db.update("insert into sales (harvest_id, buyer_name, sold_on, quantity, unit_price, total_amount, user_id)"
                            + " values (?, ?, ?, ?, ?, ?, ?)",
                    harvest[0], BUYERS[random.nextInt(BUYERS.length)], soldOn.isAfter(today) ? today : soldOn,
                    quantity, price, quantity.multiply(price).setScale(2, RoundingMode.HALF_UP), pick(userIds));
        }

        List<Long> blockIds = new ArrayList<>();
        Db.list("select id from blocks order by id").forEach(b -> blockIds.add(((Number) b.get("id")).longValue()));
        for (int i = 0; i < 30; i++) {
            Object[] expense = EXPENSES[random.nextInt(EXPENSES.length)];
            long amount = Math.round(between((int) expense[1], (int) expense[2]) / 1000.0) * 1000;
            Db.update("insert into expenses (block_id, category, amount, spent_on, description, user_id) "
                            + "values (?, ?, ?, ?, ?, ?)",
                    random.nextInt(100) < 70 ? pick(blockIds) : null, expense[0], BigDecimal.valueOf(amount),
                    dayInMonth(i % 12), expense[3], pick(userIds));
        }
    }

    private List<SeedTree> seedTrees() {
        Map<String, List<Object[]>> types = new HashMap<>();
        for (Map<String, Object> row : Db.list("select tt.id, tt.name, c.name as category from tree_types tt "
                + "join categories c on c.id = tt.category_id order by tt.id")) {
            types.computeIfAbsent((String) row.get("category"), k -> new ArrayList<>())
                    .add(new Object[]{row.get("id"), row.get("name")});
        }
        Map<String, Object> blocks = new HashMap<>();
        Db.list("select id, name from blocks").forEach(b -> blocks.put((String) b.get("name"), b.get("id")));

        Object[][] plan = {
                {"Fruit", "productive", 12}, {"Fruit", "growing", 2}, {"Fruit", "needs_attention", 2},
                {"Fruit", "seedling", 1}, {"Fruit", "dead", 1},
                {"Spice", "productive", 4}, {"Spice", "growing", 1}, {"Spice", "needs_attention", 1},
                {"Timber", "growing", 6}, {"Timber", "seedling", 3}, {"Timber", "dead", 1},
                {"Ornamental", "growing", 3}, {"Ornamental", "seedling", 2}, {"Ornamental", "needs_attention", 1},
        };
        Map<String, byte[]> photos = new HashMap<>();
        List<SeedTree> trees = new ArrayList<>();
        int number = 1;
        for (Object[] step : plan) {
            String category = (String) step[0];
            String status = (String) step[1];
            for (int i = 0; i < (int) step[2]; i++) {
                Object[] type = pick(types.get(category));
                String block = status.equals("seedling") ? "River Block"
                        : category.equals("Fruit") ? "North Block" : "East Block";
                String typeName = (String) type[1];
                // Seedlings and dead trees keep the "no photo" placeholder.
                byte[] photo = status.equals("seedling") || status.equals("dead") ? null
                        : photos.computeIfAbsent(typeName, Seeder::photo);
                long id = Db.insert("insert into trees (code, tree_type_id, block_id, planted_on, status, height_cm, "
                                + "notes, photo, photo_type, created_by) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) returning id",
                        String.format("T-%04d", number++), type[0], blocks.get(block), plantedOn(status), status,
                        height(status), status.equals("needs_attention") ? "Leaves are turning yellow; check for root rot." : null,
                        photo, photo == null ? null : "image/jpeg", pick(userIds));
                trees.add(new SeedTree(id, typeName, status));
            }
        }
        return trees;
    }

    private static byte[] photo(String typeName) {
        String file = "/seed-photos/" + typeName.toLowerCase().replace(' ', '-') + ".jpg";
        try (InputStream in = Seeder.class.getResourceAsStream(file)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void careLog(SeedTree tree, LocalDate performedOn, LocalDate nextDueOn) {
        Options.ActivityType activity = Options.ActivityType.values()[random.nextInt(Options.ActivityType.values().length)];
        Db.update("insert into care_logs (tree_id, activity_type, performed_on, next_due_on, notes, user_id) "
                        + "values (?, ?, ?, ?, ?, ?)",
                tree.id, activity.getValue(), performedOn,
                nextDueOn != null && nextDueOn.isBefore(performedOn) ? performedOn : nextDueOn,
                random.nextInt(100) < 40 ? activity.getLabel() + " completed without issues." : null,
                pick(userIds));
    }

    private LocalDate plantedOn(String status) {
        int[] range = switch (status) {
            case "seedling" -> new int[]{30, 360};
            case "growing" -> new int[]{730, 1825};
            case "productive" -> new int[]{2190, 4380};
            default -> new int[]{1095, 3650};
        };
        return today.minusDays(between(range[0], range[1]));
    }

    private BigDecimal height(String status) {
        return switch (status) {
            case "seedling" -> decimal(30, 120);
            case "growing" -> decimal(150, 450);
            default -> decimal(400, 1500);
        };
    }

    /** A random day in the month {@code monthsAgo} months back, never after today. */
    private LocalDate dayInMonth(int monthsAgo) {
        LocalDate month = today.withDayOfMonth(1).minusMonths(monthsAgo);
        LocalDate day = month.plusDays(between(0, month.lengthOfMonth() - 1));
        return day.isAfter(today) ? today : day;
    }

    private int between(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private BigDecimal decimal(double min, double max) {
        return BigDecimal.valueOf(min + random.nextDouble() * (max - min)).setScale(2, RoundingMode.HALF_UP);
    }

    private <T> T pick(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }
}
