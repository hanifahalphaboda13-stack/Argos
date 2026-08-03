package ca.uqac.detection;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Export aligné sur perforce_events_v3.csv (1 enregistrement par bloc événement assemblé).
 */
public final class EnrichissementEvenements {

    private static final Pattern PROJET_NAD =
        Pattern.compile("(20\\d{3}_NAD_[^/\\\\]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern PROJET_NAND =
        Pattern.compile("(20\\d{3}_NAND\\d+_N\\d+_[^/\\\\]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern PROJET_UE =
        Pattern.compile("(UE_\\d+\\.\\d+)", Pattern.CASE_INSENSITIVE);

    private static final String ENTETE_CSV_V3 =
        "timestamp,date,time,pid,user,workspace,ip,client,command,args,file_path,nb_files,project,file_ext,"
        + "duration_s,lapse_s,mem_cmd_mb,mem_proc_mb,rpc_in,rpc_out,db_read_locks,db_write_locks,"
        + "rows_get,rows_pos,rows_scan,rows_put,rows_del";

    private EnrichissementEvenements() {
    }

    public static final class LigneAnalytique {
        private final String timestamp;
        private final String date;
        private final String time;
        private final int pid;
        private final String user;
        private final String workspace;
        private final String ip;
        private final String client;
        private final String command;
        private final String args;
        private final List<String> fichiers;
        private final int nbFiles;
        private final String project;
        private final String fileExt;
        private final Double duration_s;
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

        LigneAnalytique(String timestamp, String date, String time, int pid,
                        String user, String workspace, String ip, String client,
                        String command, String args, List<String> fichiers, String project,
                        String fileExt, Double duration_s, String lapse_s,
                        String mem_cmd_mb, String mem_proc_mb,
                        int rpc_in, int rpc_out,
                        int db_read_locks, int db_write_locks,
                        int rows_get, int rows_pos, int rows_scan, int rows_put, int rows_del) {
            this.timestamp = timestamp;
            this.date = date;
            this.time = time;
            this.pid = pid;
            this.user = user;
            this.workspace = workspace;
            this.ip = ip;
            this.client = client;
            this.command = command;
            this.args = args;
            this.fichiers = Collections.unmodifiableList(new ArrayList<String>(fichiers));
            this.nbFiles = this.fichiers.size();
            this.project = project;
            this.fileExt = fileExt;
            this.duration_s = duration_s;
            this.lapse_s = lapse_s;
            this.mem_cmd_mb = mem_cmd_mb;
            this.mem_proc_mb = mem_proc_mb;
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

        public String getTimestamp() { return timestamp; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public int getPid() { return pid; }
        public String getUser() { return user; }
        public String getWorkspace() { return workspace; }
        public String getIp() { return ip; }
        public String getClient() { return client; }
        public String getCommand() { return command; }
        public String getArgs() { return args; }
        public String getFilePath() { return formaterFilePathArray(fichiers); }
        public List<String> getFichiers() { return fichiers; }
        public int getNbFiles() { return nbFiles; }
        public String getProject() { return project; }
        public String getFileExt() { return fileExt; }
        public Double getDuration_s() { return duration_s; }
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
    }

    public static List<LigneAnalytique> enrichir(List<PerforceEvent> evenements) {
        List<LigneAnalytique> lignes = new ArrayList<LigneAnalytique>();
        for (PerforceEvent evt : evenements) {
            lignes.add(enrichirEvenement(evt));
        }
        return lignes;
    }

    private static final int LARGEUR_ETIQUETTE = 21;
    private static final String PREFIXE_SUITE_LISTE =
        String.format("%" + (2 + LARGEUR_ETIQUETTE + 3) + "s", "");

    public static void afficherDetail(LigneAnalytique l) {
        afficherChamp("timestamp", l.getTimestamp());
        afficherChamp("date", l.getDate());
        afficherChamp("time", l.getTime());
        afficherChamp("pid", String.valueOf(l.getPid()));
        afficherChamp("user", l.getUser());
        afficherChamp("workspace", l.getWorkspace());
        afficherChamp("ip", l.getIp());
        afficherChamp("client", l.getClient());
        afficherChamp("command", l.getCommand());
        afficherChamp("args", l.getArgs().isEmpty() ? "(vide)" : l.getArgs());
        afficherChamp("file_path", l.getFilePath());
        afficherChamp("nb_files", String.valueOf(l.getNbFiles()));
        afficherChamp("project", l.getProject().isEmpty() ? "(aucun)" : l.getProject());
        afficherChamp("file_ext", l.getFileExt().isEmpty() ? "(aucun)" : l.getFileExt());
        afficherChamp("duration_s", l.getDuration_s() == null ? "(vide)" : String.valueOf(l.getDuration_s()));
        afficherChamp("lapse_s", l.getLapse_s().isEmpty() ? "(vide)" : l.getLapse_s());
        afficherChamp("mem_cmd_mb", l.getMem_cmd_mb().isEmpty() ? "(vide)" : l.getMem_cmd_mb());
        afficherChamp("mem_proc_mb", l.getMem_proc_mb().isEmpty() ? "(vide)" : l.getMem_proc_mb());
        afficherChamp("rpc_in", String.valueOf(l.getRpc_in()));
        afficherChamp("rpc_out", String.valueOf(l.getRpc_out()));
        afficherChamp("db_read_locks", String.valueOf(l.getDb_read_locks()));
        afficherChamp("db_write_locks", String.valueOf(l.getDb_write_locks()));
        afficherChamp("rows_get", String.valueOf(l.getRows_get()));
        afficherChamp("rows_scan", String.valueOf(l.getRows_scan()));
    }

    private static void afficherChamp(String etiquette, String valeur) {
        if (valeur.contains("\n")) {
            System.out.println(String.format("  %-" + LARGEUR_ETIQUETTE + "s :", etiquette));
            for (String ligne : valeur.split("\n")) {
                System.out.println(PREFIXE_SUITE_LISTE + ligne);
            }
            return;
        }
        System.out.println(String.format("  %-" + LARGEUR_ETIQUETTE + "s : %s", etiquette, valeur));
    }

    public static void exporter(List<PerforceEvent> evenements, String cheminSortie,
                                UtilEvenements.FormatSortie format) throws IOException {
        exporterLignes(enrichir(evenements), cheminSortie, format);
    }

    public static void exporterLignes(List<LigneAnalytique> lignes, String cheminSortie,
                                      UtilEvenements.FormatSortie format) throws IOException {
        if (format == UtilEvenements.FormatSortie.JSON) {
            ecrireJSON(lignes, cheminSortie);
        } else {
            ecrireCSV(lignes, cheminSortie);
        }
    }

    private static LigneAnalytique enrichirEvenement(PerforceEvent evt) {
        List<String> fichiers = evt.getFichiers();
        String fileExt = extraireExtensions(fichiers);
        MetriquesServeur m = evt.getMetriques();
        String tsIso = formaterTimestampIso(evt.getTimestamp());

        return new LigneAnalytique(
            tsIso,
            extraireDateIso(tsIso),
            extraireHeure(tsIso),
            parserPid(evt.getPid()),
            evt.getUser(),
            evt.getWorkspace(),
            evt.getIp(),
            evt.getClient(),
            evt.getCommande(),
            evt.getArgs(),
            fichiers,
            extraireProjetEvenement(fichiers, evt.getWorkspace()),
            fileExt,
            parserDuree(evt.getDuree_s()),
            m.getLapse_s(),
            m.getMem_cmd_mb(),
            m.getMem_proc_mb(),
            m.getRpc_in(),
            m.getRpc_out(),
            m.getDb_read_locks(),
            m.getDb_write_locks(),
            m.getRows_get(),
            m.getRows_pos(),
            m.getRows_scan(),
            m.getRows_put(),
            m.getRows_del()
        );
    }

    static String extraireProjetEvenement(List<String> fichiersBruts, String workspace) {
        for (String brut : fichiersBruts) {
            String p = extraireProjetChemin(normaliserChemin(brut), workspace);
            if (!estFallbackProjet(p, workspace)) {
                return p;
            }
        }
        for (String brut : fichiersBruts) {
            Matcher ue = PROJET_UE.matcher(normaliserChemin(brut));
            if (ue.find()) {
                return ue.group(1);
            }
        }
        for (String brut : fichiersBruts) {
            String premier = extrairePremierDossierApresLecteur(normaliserChemin(brut));
            if (premier != null) {
                return premier;
            }
        }
        for (String brut : fichiersBruts) {
            String p = extraireDossierAvantContenu(normaliserChemin(brut));
            if (p != null) {
                return p;
            }
        }
        return "";
    }

    private static boolean estFallbackProjet(String projet, String workspace) {
        if (projet == null || projet.isEmpty() || "inconnu".equals(projet)) {
            return true;
        }
        return workspace != null && projet.equals(workspace.trim());
    }

    static String extraireProjetChemin(String cheminNormalise, String workspace) {
        if (cheminNormalise != null && !cheminNormalise.isEmpty()) {
            Matcher nad = PROJET_NAD.matcher(cheminNormalise);
            if (nad.find()) {
                return nad.group(1);
            }
            Matcher nand = PROJET_NAND.matcher(cheminNormalise);
            if (nand.find()) {
                return nand.group(1);
            }
            if (cheminNormalise.startsWith("//")) {
                String depot = extrairePremierSegmentDepot(cheminNormalise);
                if (!depot.isEmpty()) {
                    return depot;
                }
            }
        }
        if (workspace != null && !workspace.trim().isEmpty()) {
            return workspace.trim();
        }
        return "inconnu";
    }

    private static String extraireDossierAvantContenu(String chemin) {
        String[] marqueurs = {"/Content/", "/Engine/", "/Plugins/"};
        for (String marqueur : marqueurs) {
            int idx = chemin.indexOf(marqueur);
            if (idx > 0) {
                String avant = chemin.substring(0, idx);
                int slash = avant.lastIndexOf('/');
                return slash >= 0 ? avant.substring(slash + 1) : avant;
            }
        }
        return null;
    }

    private static String extrairePremierDossierApresLecteur(String chemin) {
        if (chemin.length() < 3 || chemin.charAt(1) != ':') {
            return null;
        }
        String reste = chemin.substring(3);
        if (reste.startsWith("Program Files/Epic Games/") && reste.length() > 25) {
            reste = reste.substring(25);
        }
        int slash = reste.indexOf('/');
        String segment = slash >= 0 ? reste.substring(0, slash) : reste;
        return segment.isEmpty() ? null : segment;
    }

    private static String extrairePremierSegmentDepot(String chemin) {
        if (chemin.length() <= 2) {
            return "";
        }
        String reste = chemin.substring(2);
        int slash = reste.indexOf('/');
        String segment = slash >= 0 ? reste.substring(0, slash) : reste;
        return segment.trim();
    }

    static String normaliserChemin(String chemin) {
        if (chemin == null || chemin.trim().isEmpty()) {
            return "";
        }
        String n = chemin.trim().replace('\\', '/');
        int dieze = n.indexOf('#');
        if (dieze >= 0) {
            n = n.substring(0, dieze);
        }
        int paren = n.indexOf(" (");
        if (paren >= 0) {
            n = n.substring(0, paren).trim();
        }
        if (n.endsWith("/...")) {
            n = n.substring(0, n.length() - 4);
        }
        return n;
    }

    static String extraireNomFichier(String cheminNormalise) {
        if (cheminNormalise == null || cheminNormalise.isEmpty()) {
            return "";
        }
        int slash = cheminNormalise.lastIndexOf('/');
        return slash >= 0 ? cheminNormalise.substring(slash + 1) : cheminNormalise;
    }

    static String extraireExtension(String fileName) {
        if (fileName == null || fileName.isEmpty() || "...".equals(fileName)) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }

    /** Extensions distinctes de tous les fichiers (ordre de première apparition). */
    static String extraireExtensions(List<String> fichiers) {
        LinkedHashSet<String> extensions = new LinkedHashSet<String>();
        for (String brut : fichiers) {
            String nom = extraireNomFichier(normaliserChemin(brut));
            String ext = extraireExtension(nom);
            if (!ext.isEmpty()) {
                extensions.add(ext);
            }
        }
        if (extensions.isEmpty()) {
            return "";
        }
        if (extensions.size() == 1) {
            return extensions.iterator().next();
        }
        return formaterExtensionsArray(new ArrayList<String>(extensions));
    }

    static String formaterExtensionsArray(List<String> extensions) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < extensions.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(extensions.get(i)).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    static String formaterTimestampIso(String timestamp) {
        if (timestamp == null || timestamp.length() < 19) {
            return timestamp == null ? "" : timestamp;
        }
        return timestamp.substring(0, 10).replace('/', '-')
            + timestamp.substring(10);
    }

    static String extraireDateIso(String timestampIso) {
        if (timestampIso == null || timestampIso.length() < 10) {
            return timestampIso == null ? "" : timestampIso;
        }
        return timestampIso.substring(0, 10);
    }

    static String extraireHeure(String timestamp) {
        if (timestamp == null || timestamp.length() < 19) {
            return "";
        }
        return timestamp.substring(11, 19);
    }

    static int parserPid(String pid) {
        if (pid == null || pid.trim().isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(pid.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static Double parserDuree(String duree) {
        if (duree == null || duree.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(duree.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String formaterFilePathArray(List<String> chemins) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < chemins.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(echapperJSON(chemins.get(i))).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    private static void ecrireCSV(List<LigneAnalytique> lignes, String cheminCSV) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(cheminCSV), 1 << 20);
        try {
            bw.write(ENTETE_CSV_V3);
            bw.newLine();
            for (LigneAnalytique l : lignes) {
                bw.write(String.join(",",
                    echapperCSV(l.getTimestamp()),
                    echapperCSV(l.getDate()),
                    echapperCSV(l.getTime()),
                    String.valueOf(l.getPid()),
                    echapperCSV(l.getUser()),
                    echapperCSV(l.getWorkspace()),
                    echapperCSV(l.getIp()),
                    echapperCSV(l.getClient()),
                    echapperCSV(l.getCommand()),
                    echapperCSV(l.getArgs()),
                    echapperCSV(l.getFilePath()),
                    String.valueOf(l.getNbFiles()),
                    echapperCSV(l.getProject()),
                    echapperCSV(l.getFileExt()),
                    formaterNombre(l.getDuration_s()),
                    echapperCSV(l.getLapse_s()),
                    echapperCSV(l.getMem_cmd_mb()),
                    echapperCSV(l.getMem_proc_mb()),
                    String.valueOf(l.getRpc_in()),
                    String.valueOf(l.getRpc_out()),
                    String.valueOf(l.getDb_read_locks()),
                    String.valueOf(l.getDb_write_locks()),
                    String.valueOf(l.getRows_get()),
                    String.valueOf(l.getRows_pos()),
                    String.valueOf(l.getRows_scan()),
                    String.valueOf(l.getRows_put()),
                    String.valueOf(l.getRows_del())
                ));
                bw.newLine();
            }
        } finally {
            bw.close();
        }
    }

    private static void ecrireJSON(List<LigneAnalytique> lignes, String cheminJSON) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(cheminJSON), 1 << 20);
        try {
            bw.write("{\n");
            bw.write("  \"nombre_evenements\": " + lignes.size() + ",\n");
            bw.write("  \"evenements\": [\n");
            for (int i = 0; i < lignes.size(); i++) {
                ecrireLigneJSON(bw, lignes.get(i), "    ");
                bw.write(i < lignes.size() - 1 ? ",\n" : "\n");
            }
            bw.write("  ]\n");
            bw.write("}\n");
        } finally {
            bw.close();
        }
    }

    private static void ecrireLigneJSON(BufferedWriter bw, LigneAnalytique l, String indent)
            throws IOException {
        bw.write(indent + "{\n");
        ecrireChampTexte(bw, indent + "  ", "timestamp", l.getTimestamp(), false);
        ecrireChampTexte(bw, indent + "  ", "date", l.getDate(), false);
        ecrireChampTexte(bw, indent + "  ", "time", l.getTime(), false);
        ecrireChampEntier(bw, indent + "  ", "pid", l.getPid(), false);
        ecrireChampTexte(bw, indent + "  ", "user", l.getUser(), false);
        ecrireChampTexte(bw, indent + "  ", "workspace", l.getWorkspace(), false);
        ecrireChampTexte(bw, indent + "  ", "ip", l.getIp(), false);
        ecrireChampTexte(bw, indent + "  ", "client", l.getClient(), false);
        ecrireChampTexte(bw, indent + "  ", "command", l.getCommand(), false);
        ecrireChampTexte(bw, indent + "  ", "args", l.getArgs(), false);
        ecrireChampTableauTexte(bw, indent + "  ", "file_path", l.getFichiers(), false);
        ecrireChampEntier(bw, indent + "  ", "nb_files", l.getNbFiles(), false);
        ecrireChampTexte(bw, indent + "  ", "project", l.getProject(), false);
        ecrireChampTexte(bw, indent + "  ", "file_ext", l.getFileExt(), false);
        ecrireChampNombre(bw, indent + "  ", "duration_s", l.getDuration_s(), false);
        ecrireChampTexte(bw, indent + "  ", "lapse_s", l.getLapse_s(), false);
        ecrireChampTexte(bw, indent + "  ", "mem_cmd_mb", l.getMem_cmd_mb(), false);
        ecrireChampTexte(bw, indent + "  ", "mem_proc_mb", l.getMem_proc_mb(), false);
        ecrireChampEntier(bw, indent + "  ", "rpc_in", l.getRpc_in(), false);
        ecrireChampEntier(bw, indent + "  ", "rpc_out", l.getRpc_out(), false);
        ecrireChampEntier(bw, indent + "  ", "db_read_locks", l.getDb_read_locks(), false);
        ecrireChampEntier(bw, indent + "  ", "db_write_locks", l.getDb_write_locks(), false);
        ecrireChampEntier(bw, indent + "  ", "rows_get", l.getRows_get(), false);
        ecrireChampEntier(bw, indent + "  ", "rows_pos", l.getRows_pos(), false);
        ecrireChampEntier(bw, indent + "  ", "rows_scan", l.getRows_scan(), false);
        ecrireChampEntier(bw, indent + "  ", "rows_put", l.getRows_put(), false);
        ecrireChampEntier(bw, indent + "  ", "rows_del", l.getRows_del(), true);
        bw.write(indent + "}");
    }

    private static void ecrireChampTableauTexte(BufferedWriter bw, String indent, String nom,
            List<String> chemins, boolean dernier) throws IOException {
        bw.write(indent + "\"" + nom + "\": [");
        for (int i = 0; i < chemins.size(); i++) {
            if (i > 0) {
                bw.write(", ");
            }
            bw.write("\"" + echapperJSON(chemins.get(i)) + "\"");
        }
        bw.write("]");
        bw.write(dernier ? "\n" : ",\n");
    }

    private static void ecrireChampTexte(BufferedWriter bw, String indent, String nom,
            String valeur, boolean dernier) throws IOException {
        bw.write(indent + "\"" + nom + "\": \"" + echapperJSON(valeur) + "\"");
        bw.write(dernier ? "\n" : ",\n");
    }

    private static void ecrireChampEntier(BufferedWriter bw, String indent, String nom,
            int valeur, boolean dernier) throws IOException {
        bw.write(indent + "\"" + nom + "\": " + valeur);
        bw.write(dernier ? "\n" : ",\n");
    }

    private static void ecrireChampNombre(BufferedWriter bw, String indent, String nom,
            Double valeur, boolean dernier) throws IOException {
        if (valeur == null) {
            bw.write(indent + "\"" + nom + "\": null");
        } else {
            bw.write(indent + "\"" + nom + "\": " + valeur);
        }
        bw.write(dernier ? "\n" : ",\n");
    }

    private static String formaterNombre(Double valeur) {
        return valeur == null ? "" : String.valueOf(valeur);
    }

    private static String echapperCSV(String valeur) {
        if (valeur.contains(",") || valeur.contains("\"") || valeur.contains(";")
                || valeur.contains("\n")) {
            return "\"" + valeur.replace("\"", "\"\"") + "\"";
        }
        return valeur;
    }

    private static String echapperJSON(String valeur) {
        StringBuilder sb = new StringBuilder();
        for (char c : valeur.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
