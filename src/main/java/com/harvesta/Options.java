package com.harvesta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixed option sets stored as strings in the database (PRD 6: string column + validation).
 * JSP reads them as beans: ${o.value}, ${o.label}.
 */
public final class Options {

    private Options() {
    }

    public interface Option {
        String getValue();

        String getLabel();
    }

    public enum TreeStatus implements Option {
        SEEDLING("Seedling"), GROWING("Growing"), PRODUCTIVE("Productive"),
        NEEDS_ATTENTION("Needs attention"), DEAD("Dead");

        private final String label;

        TreeStatus(String label) {
            this.label = label;
        }

        public String getValue() {
            return name().toLowerCase();
        }

        public String getLabel() {
            return label;
        }
    }

    public enum ActivityType implements Option {
        WATERING("Watering"), FERTILIZING("Fertilizing"), PRUNING("Pruning"), PEST_CONTROL("Pest control"),
        DISEASE_TREATMENT("Disease treatment"), WEEDING("Weeding"), INSPECTION("Inspection"), OTHER("Other");

        private final String label;

        ActivityType(String label) {
            this.label = label;
        }

        public String getValue() {
            return name().toLowerCase();
        }

        public String getLabel() {
            return label;
        }
    }

    public enum HarvestUnit implements Option {
        KG("kg"), PCS("pcs");

        private final String label;

        HarvestUnit(String label) {
            this.label = label;
        }

        public String getValue() {
            return name().toLowerCase();
        }

        public String getLabel() {
            return label;
        }
    }

    public enum HarvestGrade implements Option {
        PREMIUM("Premium"), GOOD("Good"), FAIR("Fair"), POOR("Poor");

        private final String label;

        HarvestGrade(String label) {
            this.label = label;
        }

        public String getValue() {
            return name().toLowerCase();
        }

        public String getLabel() {
            return label;
        }
    }

    public enum ExpenseCategory implements Option {
        FERTILIZER("Fertilizer"), PESTICIDE("Pesticide"), LABOR("Labor"), EQUIPMENT("Equipment"),
        WATER("Water"), TRANSPORT("Transport"), OTHER("Other");

        private final String label;

        ExpenseCategory(String label) {
            this.label = label;
        }

        public String getValue() {
            return name().toLowerCase();
        }

        public String getLabel() {
            return label;
        }
    }

    public enum UserRole implements Option {
        ADMIN("Admin"), STAFF("Staff"), VIEWER("Viewer");

        private final String label;

        UserRole(String label) {
            this.label = label;
        }

        public String getValue() {
            return name().toLowerCase();
        }

        public String getLabel() {
            return label;
        }
    }

    /** Every option set, exposed to JSP as application attributes (e.g. ${treeStatuses}). */
    public static final Map<String, Option[]> ALL = Map.of(
            "treeStatuses", TreeStatus.values(),
            "activityTypes", ActivityType.values(),
            "harvestUnits", HarvestUnit.values(),
            "harvestGrades", HarvestGrade.values(),
            "expenseCategories", ExpenseCategory.values(),
            "userRoles", UserRole.values());

    private static final Map<String, String> LABELS = new HashMap<>();

    static {
        // Shared values (e.g. "other") have the same label in every set.
        ALL.values().forEach(set -> List.of(set).forEach(o -> LABELS.put(o.getValue(), o.getLabel())));
    }

    public static String label(Object value) {
        return value == null ? "" : LABELS.getOrDefault(value.toString(), value.toString());
    }

    public static boolean valid(Option[] set, String value) {
        for (Option option : set) {
            if (option.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }
}
