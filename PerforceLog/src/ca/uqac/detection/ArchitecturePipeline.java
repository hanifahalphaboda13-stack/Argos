package ca.uqac.detection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Génère le schéma d'architecture du pipeline PerforceLog (PNG).
 */
public final class ArchitecturePipeline {

    private static final Color BLEU_FONCE = new Color(0x1A, 0x36, 0x5D);
    private static final Color BLEU = new Color(0x2E, 0x5F, 0x9E);
    private static final Color ACCENT = new Color(0xE8, 0x6C, 0x00);
    private static final Color PHASE1 = new Color(0x2E, 0x7D, 0x4E);
    private static final Color PHASE2 = new Color(0x9E, 0x3A, 0x3A);
    private static final Color FOND = new Color(0xF8, 0xFA, 0xFC);
    private static final Color BOITE = Color.WHITE;
    private static final Color TEXTE = new Color(0x33, 0x33, 0x33);
    private static final Color SOUS_TITRE = new Color(0x55, 0x55, 0x55);

    private static final int LARGEUR = 1180;
    private static final int HAUTEUR = 820;

    public static void generer(File fichier) throws IOException {
        BufferedImage image = new BufferedImage(LARGEUR, HAUTEUR, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(FOND);
            g.fillRect(0, 0, LARGEUR, HAUTEUR);

            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.setColor(BLEU_FONCE);
            g.drawString("PerforceLog — architecture du pipeline", 36, 40);

            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(SOUS_TITRE);
            g.drawString("log brut → blocs d'événement → dataset + métriques OpenMetrics (phase 1) + alertes CEP (phase 2)",
                36, 62);

            int cx = LARGEUR / 2;
            int y = 84;
            int bw = 340;
            int bh = 50;

            boite(g, cx - bw / 2, y, bw, bh, BLEU_FONCE, "log.txt", "entrée (~1,7 Go)", true);
            y += bh + 26;
            fleche(g, cx, y - 26, cx, y - 6);

            boite(g, cx - bw / 2, y, bw, bh + 16, BLEU, "LecteurLog + MetriquesServeur",
                "bloc = cmd user-* + completed + métriques --- (pid)", false);
            y += bh + 16 + 26;
            fleche(g, cx, y - 26, cx, y - 6);

            boite(g, cx - bw / 2, y, bw, bh, BLEU, "Déduplication",
                "clé : timestamp | pid | commande | args | fichiers", false);
            y += bh + 34;

            int forkY = y - 6;
            int gaucheX = cx - 270;
            int droiteX = cx + 270;
            fleche(g, cx, forkY - 26, cx, forkY);
            fleche(g, cx, forkY, gaucheX, forkY + 22);
            fleche(g, cx, forkY, droiteX, forkY + 22);

            int colY = forkY + 22;
            int colW = 300;

            boite(g, gaucheX - colW / 2, colY, colW, 58, ACCENT, "Export dataset (v3)",
                "1 enreg. / bloc · 27 colonnes", false);

            boite(g, droiteX - colW / 2, colY, colW, 58, ACCENT, "BeepBeep 3.13",
                "phase 1 : métriques  |  phase 2 : CEP", false);

            int sortieY = colY + 58 + 32;
            fleche(g, gaucheX, colY + 58, gaucheX, sortieY - 6);
            fleche(g, droiteX, colY + 58, droiteX, sortieY - 6);

            // Branche gauche : fichiers dataset
            int gaucheH = 96;
            int gaucheTitreH = 28;
            boiteContenu(g, gaucheX - colW / 2, sortieY, colW, gaucheH,
                new Color(0xE8, 0xEE, 0xF5), "Fichiers dataset", gaucheTitreH);
            lignesExport(g, gaucheX - colW / 2 + 16, sortieY + gaucheTitreH + 22, new String[] {
                "perforce_events_v3.json",
                "perforce_events_v3.csv"
            });

            // Branche droite : phase 1 + phase 2
            int droiteW = 340;
            int droiteH = 228;
            int droiteTitreH = 28;
            boiteContenu(g, droiteX - droiteW / 2, sortieY, droiteW, droiteH,
                new Color(0xE8, 0xEE, 0xF5), "Fichiers JSON (racine projet)", droiteTitreH);

            int px = droiteX - droiteW / 2 + 16;
            int py = sortieY + droiteTitreH + 20;

            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(PHASE1);
            g.drawString("Phase 1 — métriques OpenMetrics", px, py);
            py += 18;
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(TEXTE);
            String[] phase1 = {
                "perforce_counters.json      (Counter)",
                "perforce_gauges.json        (Gauge)",
                "perforce_histogrammes.json  (Histogram)",
                "perforce_summaries.json     (Summary)"
            };
            for (String ligne : phase1) {
                g.drawString(ligne, px + 8, py);
                py += 17;
            }

            py += 6;
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(PHASE2);
            g.drawString("Phase 2 — alertes CEP", px, py);
            py += 18;
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(TEXTE);
            g.drawString("perforce_events_alertes.json", px + 8, py);
            py += 17;
            g.setFont(new Font("SansSerif", Font.ITALIC, 10));
            g.setColor(SOUS_TITRE);
            g.drawString("edit→revert · apathy · rush collectif", px + 8, py);

            int legendeY = HAUTEUR - 44;
            g.setColor(new Color(0xDD, 0xDD, 0xDD));
            g.drawLine(36, legendeY - 10, LARGEUR - 36, legendeY - 10);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(SOUS_TITRE);
            g.drawString("Tous les exports sont écrits à la racine du projet (même dossier que log.txt).", 36, legendeY + 8);
            g.drawString("Phase 1 = agrégats sans mémoire d'ordre  ·  Phase 2 = séquences fichier par fichier", 36, legendeY + 26);

        } finally {
            g.dispose();
        }

        ImageIO.write(image, "png", fichier);
    }

    private static void lignesExport(Graphics2D g, int x, int y, String[] lignes) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(TEXTE);
        for (String ligne : lignes) {
            g.drawString(ligne, x, y);
            y += 17;
        }
    }

    public static void main(String[] args) throws IOException {
        String chemin = args.length > 0 ? args[0]
            : "figures/fig00_architecture_pipeline.png";
        File fichier = new File(chemin);
        File parent = fichier.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        generer(fichier);
        System.out.println("Figure écrite : " + fichier.getAbsolutePath());
    }

    private ArchitecturePipeline() {
    }

    /** Boîte avec bandeau titre séparé du contenu (évite chevauchement texte). */
    private static void boiteContenu(Graphics2D g, int x, int y, int w, int h,
            Color fond, String titre, int titreH) {
        RoundRectangle2D rect = new RoundRectangle2D.Float(x, y, w, h, 12, 12);
        g.setColor(fond);
        g.fill(rect);
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(0xBB, 0xCC, 0xDD));
        g.draw(rect);

        g.setColor(new Color(0xD8, 0xE2, 0xEC));
        g.fillRoundRect(x + 1, y + 1, w - 2, titreH, 10, 10);

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(TEXTE);
        FontMetrics fm = g.getFontMetrics();
        int titreX = x + (w - fm.stringWidth(titre)) / 2;
        g.drawString(titre, titreX, y + titreH - 9);
    }

    private static void boite(Graphics2D g, int x, int y, int w, int h,
            Color bordure, String titre, String sousTitre, boolean entree) {
        RoundRectangle2D rect = new RoundRectangle2D.Float(x, y, w, h, 12, 12);
        g.setColor(BOITE);
        g.fill(rect);
        g.setStroke(new BasicStroke(entree ? 2.5f : 2f));
        g.setColor(bordure);
        g.draw(rect);

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(TEXTE);
        FontMetrics fm = g.getFontMetrics();
        int titreX = x + (w - fm.stringWidth(titre)) / 2;
        g.drawString(titre, titreX, y + 22);

        if (sousTitre != null && !sousTitre.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(SOUS_TITRE);
            fm = g.getFontMetrics();
            int sousX = x + (w - fm.stringWidth(sousTitre)) / 2;
            g.drawString(sousTitre, sousX, y + (h > 50 ? 40 : 36));
        }
    }

    private static void fleche(Graphics2D g, int x1, int y1, int x2, int y2) {
        g.setStroke(new BasicStroke(2f));
        g.setColor(BLEU);
        g.drawLine(x1, y1, x2, y2);

        double angle = Math.atan2(y2 - y1, x2 - x1);
        int size = 9;
        Path2D tete = new Path2D.Double();
        tete.moveTo(x2, y2);
        tete.lineTo(x2 - size * Math.cos(angle - Math.PI / 6),
            y2 - size * Math.sin(angle - Math.PI / 6));
        tete.lineTo(x2 - size * Math.cos(angle + Math.PI / 6),
            y2 - size * Math.sin(angle + Math.PI / 6));
        tete.closePath();
        g.setColor(BLEU);
        g.fill(tete);
    }
}
