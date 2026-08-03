# Argos
Argos automatically detects 5 collaboration antipatterns (Collective Procrastination, Lone Wolf, Bystander Apathy...) from Perforce logs, using Prometheus-style metrics and complex event processing (BeepBeep 3.13). Tested on 1.7GB of logs (16,262 events in 14s), it identifies 217 Edit-Revert sequences and 250 abandoned files.
