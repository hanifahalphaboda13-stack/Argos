package ca.uqac.detection;

/**
 * Noms de métriques alignés sur le client Java Prometheus / OpenMetrics.
 * @see <a href="https://prometheus.github.io/client_java/">Prometheus Java client</a>
 */
public final class MetriquesPrometheus {

    public static final String UNIT_SECONDS = "seconds";

    // Counters (_total)
    public static final String EVENTS_TOTAL = "perforce_events_total";
    public static final String USER_SUBMIT_TOTAL = "perforce_user_submit_total";
    public static final String USER_EDIT_TOTAL = "perforce_user_edit_total";

    // Gauges
    public static final String OPEN_EDIT_FILES = "perforce_open_edit_files";
    public static final String OPEN_EDIT_USERS = "perforce_open_edit_users";

    // Histogram & Summary (unit = seconds)
    public static final String COMMAND_DURATION_SECONDS = "perforce_command_duration_seconds";

    public static final String HELP_EVENTS_TOTAL =
        "Total number of Perforce user commands processed";
    public static final String HELP_USER_SUBMIT_TOTAL =
        "Total number of user-submit commands";
    public static final String HELP_USER_EDIT_TOTAL =
        "Total number of user-edit commands";
    public static final String HELP_OPEN_EDIT_FILES =
        "Number of files currently open for edit (checkout active)";
    public static final String HELP_OPEN_EDIT_USERS =
        "Number of users with at least one open edit";
    public static final String HELP_COMMAND_DURATION =
        "Perforce server command duration from completed line";

    private MetriquesPrometheus() {
    }

    /** perforce_user_sync_total à partir de user-sync */
    static String counterPourCommande(String commande) {
        return "perforce_" + commande.replace('-', '_') + "_total";
    }
}
