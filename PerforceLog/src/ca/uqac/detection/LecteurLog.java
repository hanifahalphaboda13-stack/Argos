package ca.uqac.detection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse un log Perforce brut en streaming.
 * Ne conserve que les commandes utilisateur et ignore le bruit serveur.
 */
public class LecteurLog {

    private static final String ENTETE = "Perforce server info:";

    static final Set<String> COMMANDES_UTILISATEUR = creerCommandesUtilisateur();

    private static Set<String> creerCommandesUtilisateur() {
        Set<String> commandes = new HashSet<String>();
        commandes.add("user-edit");
        commandes.add("user-submit");
        commandes.add("user-sync");
        commandes.add("user-add");
        commandes.add("user-revert");
        commandes.add("user-lock");
        commandes.add("user-shelve");
        commandes.add("user-resolve");
        commandes.add("user-delete");
        commandes.add("user-diff");
        commandes.add("user-integrate");
        return Collections.unmodifiableSet(commandes);
    }

    private static final Pattern LIGNE_COMMANDE = Pattern.compile(
        "^(\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}) pid (\\d+) (\\w+)@(\\S+) (\\S+) \\[([^\\]]+)\\] '(.+)'$"
    );

    private static final Pattern LIGNE_COMPLETED = Pattern.compile(
        "^(\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}) pid (\\d+) completed ([\\d.]+)s\\s*$"
    );

    private static final Pattern MARQUEUR_CHEMIN =
        Pattern.compile("(?=//|[A-Za-z]:[/\\\\])");

    @FunctionalInterface
    public interface ConsommateurEvenement {
        void accepter(PerforceEvent evt) throws IOException;
    }

    public static List<PerforceEvent> lire(String cheminLog) throws IOException {
        final List<PerforceEvent> evenements = new ArrayList<PerforceEvent>();
        parcourir(cheminLog, new ConsommateurEvenement() {
            @Override
            public void accepter(PerforceEvent evt) {
                evenements.add(evt);
            }
        });
        return evenements;
    }

    public static void parcourir(String cheminLog, ConsommateurEvenement consommateur) throws IOException {
        Map<String, PerforceEvent> enAttenteParPid = new LinkedHashMap<String, PerforceEvent>();

        try (BufferedReader br = new BufferedReader(new FileReader(cheminLog), 1 << 20)) {
            LecteurAvecRemise lecteur = new LecteurAvecRemise(br);
            String ligne;
            while ((ligne = lecteur.lireLigne()) != null) {
                if (!ENTETE.equals(ligne)) {
                    continue;
                }
                String contenu = lecteur.lireLigne();
                if (contenu == null) {
                    break;
                }
                contenu = contenu.trim();
                traiterContenu(contenu, lecteur, enAttenteParPid, consommateur);
            }
        }

        for (PerforceEvent evt : enAttenteParPid.values()) {
            consommateur.accepter(evt);
        }
    }

    private static void traiterContenu(String contenu, LecteurAvecRemise lecteur,
            Map<String, PerforceEvent> enAttenteParPid,
            ConsommateurEvenement consommateur) throws IOException {
        Matcher cmd = LIGNE_COMMANDE.matcher(contenu);
        if (cmd.matches() && estCommandeUtilisateur(cmd.group(7))) {
            String pid = cmd.group(2);
            libererEnAttente(pid, enAttenteParPid);
            enAttenteParPid.put(pid, creerEvenement(cmd));
            return;
        }

        Matcher fin = LIGNE_COMPLETED.matcher(contenu);
        if (fin.matches()) {
            String pid = fin.group(2);
            PerforceEvent evt = enAttenteParPid.remove(pid);
            if (evt != null) {
                MetriquesServeur.Accumulateur accumulateur = new MetriquesServeur.Accumulateur();
                lireMetriquesApresCompleted(lecteur, accumulateur);
                consommateur.accepter(
                    evt.avecDuree(fin.group(3)).avecMetriques(accumulateur.construire()));
            }
        }
    }

    /**
     * Après "completed", le log peut répéter la commande puis lister les lignes ---.
     */
    private static void lireMetriquesApresCompleted(LecteurAvecRemise lecteur,
            MetriquesServeur.Accumulateur accumulateur) throws IOException {
        boolean dansMetriques = false;
        String ligne;
        while ((ligne = lecteur.lireLigne()) != null) {
            if (ENTETE.equals(ligne)) {
                String contenu = lecteur.lireLigne();
                if (contenu == null) {
                    return;
                }
                contenu = contenu.trim();
                if (!dansMetriques && LIGNE_COMMANDE.matcher(contenu).matches()) {
                    continue;
                }
                lecteur.remettre(contenu);
                lecteur.remettre(ENTETE);
                return;
            }
            if (ligne.startsWith("---")) {
                dansMetriques = true;
                accumulateur.traiterLigne(ligne.trim());
            }
        }
    }

    private static void libererEnAttente(String pid, Map<String, PerforceEvent> enAttente) {
        enAttente.remove(pid);
    }

    private static PerforceEvent creerEvenement(Matcher cmd) {
        String commandeBrute = cmd.group(7);
        String commande = extraireCommande(commandeBrute);
        String args = extraireArguments(commandeBrute);
        List<String> fichiers = extraireFichiers(args);

        return new PerforceEvent(
            cmd.group(1),
            cmd.group(2),
            cmd.group(3),
            cmd.group(4),
            cmd.group(5),
            cmd.group(6),
            commande,
            args,
            fichiers,
            "",
            new MetriquesServeur()
        );
    }

    static boolean estCommandeUtilisateur(String commandeBrute) {
        return COMMANDES_UTILISATEUR.contains(extraireCommande(commandeBrute));
    }

    static String extraireCommande(String commandeBrute) {
        int espace = commandeBrute.indexOf(' ');
        return espace < 0 ? commandeBrute : commandeBrute.substring(0, espace);
    }

    static String extraireArguments(String commandeBrute) {
        int espace = commandeBrute.indexOf(' ');
        return espace < 0 ? "" : commandeBrute.substring(espace + 1);
    }

    /**
     * Extrait les chemins de fichiers depuis les arguments d'une commande.
     */
    public static List<String> extraireFichiers(String argsBruts) {
        if (argsBruts == null || argsBruts.trim().isEmpty()) {
            return Collections.emptyList();
        }

        Matcher matcher = MARQUEUR_CHEMIN.matcher(argsBruts);
        List<Integer> debuts = new ArrayList<Integer>();
        while (matcher.find()) {
            debuts.add(matcher.start());
        }

        if (debuts.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> fichiers = new ArrayList<String>(debuts.size());
        for (int i = 0; i < debuts.size(); i++) {
            int debut = debuts.get(i);
            int fin = (i + 1 < debuts.size()) ? debuts.get(i + 1) : argsBruts.length();
            String chemin = argsBruts.substring(debut, fin).trim();
            if (!chemin.isEmpty()) {
                fichiers.add(chemin);
            }
        }
        return Collections.unmodifiableList(fichiers);
    }

    private static final class LecteurAvecRemise {
        private final BufferedReader br;
        private final Deque<String> lignesEnAttente = new ArrayDeque<String>();

        LecteurAvecRemise(BufferedReader br) {
            this.br = br;
        }

        String lireLigne() throws IOException {
            if (!lignesEnAttente.isEmpty()) {
                return lignesEnAttente.removeFirst();
            }
            return br.readLine();
        }

        void remettre(String ligne) {
            lignesEnAttente.addFirst(ligne);
        }
    }
}
