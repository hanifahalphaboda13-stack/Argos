package ca.uqac.detection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Métriques serveur Perforce (lignes --- après completed).
 */
public final class MetriquesServeur {

    private static final Pattern LIGNE_LAPSE =
        Pattern.compile("^---\\s+lapse\\s+([\\d.]+)s\\s*$");
    private static final Pattern LIGNE_MEMORY =
        Pattern.compile("^---\\s+memory\\s+cmd/proc\\s+(\\d+)mb/(\\d+)mb\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LIGNE_RPC =
        Pattern.compile("^---\\s+rpc\\s+msgs/size\\s+in\\+out\\s+(\\d+)\\+(\\d+)/");
    private static final Pattern LIGNE_LOCKS_ROWS =
        Pattern.compile("locks\\s+read/write\\s+(\\d+)/(\\d+)\\s+rows\\s+get\\+pos\\+scan\\s+put\\+del\\s+"
            + "(\\d+)\\+(\\d+)\\+(\\d+)\\s+(\\d+)\\+(\\d+)");

    private final String lapse_s;
    private final String mem_cmd_mb;
    private final String mem_proc_mb;
    private final int rpc_in;
    private final int rpc_out;
    private final int db_read_locks;
    private final int db_write_locks;
    private final int rows_get;
    private final int rows_pos;
    private final int rows_scan;
    private final int rows_put;
    private final int rows_del;

    public MetriquesServeur() {
        this("", "", "", 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private MetriquesServeur(String lapse_s, String mem_cmd_mb, String mem_proc_mb,
                             int rpc_in, int rpc_out,
                             int db_read_locks, int db_write_locks,
                             int rows_get, int rows_pos, int rows_scan, int rows_put, int rows_del) {
        this.lapse_s = lapse_s == null ? "" : lapse_s;
        this.mem_cmd_mb = mem_cmd_mb == null ? "" : mem_cmd_mb;
        this.mem_proc_mb = mem_proc_mb == null ? "" : mem_proc_mb;
        this.rpc_in = rpc_in;
        this.rpc_out = rpc_out;
        this.db_read_locks = db_read_locks;
        this.db_write_locks = db_write_locks;
        this.rows_get = rows_get;
        this.rows_pos = rows_pos;
        this.rows_scan = rows_scan;
        this.rows_put = rows_put;
        this.rows_del = rows_del;
    }

    public String getLapse_s() { return lapse_s; }
    public String getMem_cmd_mb() { return mem_cmd_mb; }
    public String getMem_proc_mb() { return mem_proc_mb; }
    public int getRpc_in() { return rpc_in; }
    public int getRpc_out() { return rpc_out; }
    public int getDb_read_locks() { return db_read_locks; }
    public int getDb_write_locks() { return db_write_locks; }
    public int getRows_get() { return rows_get; }
    public int getRows_pos() { return rows_pos; }
    public int getRows_scan() { return rows_scan; }
    public int getRows_put() { return rows_put; }
    public int getRows_del() { return rows_del; }

    public static final class Accumulateur {
        private String lapse_s = "";
        private String mem_cmd_mb = "";
        private String mem_proc_mb = "";
        private int rpc_in;
        private int rpc_out;
        private int db_read_locks;
        private int db_write_locks;
        private int rows_get;
        private int rows_pos;
        private int rows_scan;
        private int rows_put;
        private int rows_del;

        public void traiterLigne(String ligne) {
            if (ligne == null || !ligne.startsWith("---")) {
                return;
            }
            Matcher lapse = LIGNE_LAPSE.matcher(ligne);
            if (lapse.matches()) {
                lapse_s = lapse.group(1);
                return;
            }
            Matcher mem = LIGNE_MEMORY.matcher(ligne);
            if (mem.matches()) {
                mem_cmd_mb = mem.group(1);
                mem_proc_mb = mem.group(2);
                return;
            }
            Matcher rpc = LIGNE_RPC.matcher(ligne);
            if (rpc.matches()) {
                rpc_in = Integer.parseInt(rpc.group(1));
                rpc_out = Integer.parseInt(rpc.group(2));
                return;
            }
            Matcher lr = LIGNE_LOCKS_ROWS.matcher(ligne);
            if (lr.find()) {
                db_read_locks += Integer.parseInt(lr.group(1));
                db_write_locks += Integer.parseInt(lr.group(2));
                rows_get += Integer.parseInt(lr.group(3));
                rows_pos += Integer.parseInt(lr.group(4));
                rows_scan += Integer.parseInt(lr.group(5));
                rows_put += Integer.parseInt(lr.group(6));
                rows_del += Integer.parseInt(lr.group(7));
            }
        }

        public MetriquesServeur construire() {
            return new MetriquesServeur(
                lapse_s, mem_cmd_mb, mem_proc_mb,
                rpc_in, rpc_out,
                db_read_locks, db_write_locks,
                rows_get, rows_pos, rows_scan, rows_put, rows_del
            );
        }
    }
}
