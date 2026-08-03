#!/bin/bash
# Lance le projet sans Eclipse (compile + exécute Main)
cd "$(dirname "$0")"
javac -cp "lib/beepbeep-3.13.jar:lib/jfreechart-1.0.19.jar:lib/jcommon-1.0.23.jar" -d bin -sourcepath src src/ca/uqac/detection/*.java || exit 1
java -cp "bin:lib/beepbeep-3.13.jar:lib/jfreechart-1.0.19.jar:lib/jcommon-1.0.23.jar" ca.uqac.detection.Main "$@"
