package ca.uqac.detection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PerforceEvent {

    private final String timestamp;
    private final String pid;
    private final String user;
    private final String workspace;
    private final String ip;
    private final String client;
    private final String commande;
    private final String args;
    private final List<String> fichiers;
    private final String duration_s;
    private final MetriquesServeur metriques;

    public PerforceEvent(String timestamp, String pid, String user, String workspace,
                         String ip, String client, String commande, String args,
                         List<String> fichiers, String duree_s, MetriquesServeur metriques) {
        this.timestamp = timestamp;
        this.pid = pid;
        this.user = user;
        this.workspace = workspace;
        this.ip = ip;
        this.client = client;
        this.commande = commande;
        this.args = args == null ? "" : args;
        this.fichiers = Collections.unmodifiableList(new ArrayList<String>(fichiers));
        this.duration_s = duree_s == null ? "" : duree_s;
        this.metriques = metriques == null ? new MetriquesServeur() : metriques;
    }

    public String getTimestamp() { return timestamp; }
    public String getPid() { return pid; }
    public String getUser() { return user; }
    public String getWorkspace() { return workspace; }
    public String getIp() { return ip; }
    public String getClient() { return client; }
    public String getCommande() { return commande; }
    public String getArgs() { return args; }
    public List<String> getFichiers() { return fichiers; }
    public String getDuree_s() { return duration_s; }
    public MetriquesServeur getMetriques() { return metriques; }

    public PerforceEvent avecDuree(String duree) {
        return new PerforceEvent(
            timestamp, pid, user, workspace, ip, client, commande, args,
            fichiers, duree, metriques
        );
    }

    public PerforceEvent avecMetriques(MetriquesServeur nouvellesMetriques) {
        return new PerforceEvent(
            timestamp, pid, user, workspace, ip, client, commande, args,
            fichiers, duration_s, nouvellesMetriques
        );
    }

    public String cleDedup() {
        return timestamp + "|" + pid + "|" + commande + "|" + args + "|" + String.join(";", fichiers);
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + user + " → " + commande + " " + fichiers;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PerforceEvent)) {
            return false;
        }
        PerforceEvent autre = (PerforceEvent) obj;
        return timestamp.equals(autre.timestamp)
            && pid.equals(autre.pid)
            && user.equals(autre.user)
            && workspace.equals(autre.workspace)
            && ip.equals(autre.ip)
            && client.equals(autre.client)
            && commande.equals(autre.commande)
            && args.equals(autre.args)
            && fichiers.equals(autre.fichiers)
            && duration_s.equals(autre.duration_s)
            && metriques.getLapse_s().equals(autre.metriques.getLapse_s())
            && metriques.getRpc_in() == autre.metriques.getRpc_in()
            && metriques.getRpc_out() == autre.metriques.getRpc_out();
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, pid, user, workspace, ip, client, commande, args,
            fichiers, duration_s);
    }
}
