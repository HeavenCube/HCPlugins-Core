# Travailler avec une IA sur HCPlugins

## Une seule source de règles par dépôt

`AGENTS.md` contient les règles locales et les invariants indispensables. `CLAUDE.md` et
`GEMINI.md` importent ou référencent ce fichier ; ils ne portent pas une deuxième architecture.
Ces fichiers servent d'entrée, pas de remplacement à la lecture du code et des tests.

- Codex : instructions `AGENTS.md` du dépôt et des éventuels sous-dossiers concernés.
- Claude Code : `CLAUDE.md` importe `@AGENTS.md` ; les guides restent des liens à consulter.
- Gemini CLI : `GEMINI.md` importe `@AGENTS.md`.
- Antigravity : règles de répertoire `AGENTS.md` / `GEMINI.md` ; si une référence n'est pas
  développée par l'outil, lire explicitement AGENTS. Ne pas créer une copie divergente des règles.

Contrats des outils : [Claude Code](https://code.claude.com/docs/en/memory),
[Gemini CLI](https://geminicli.com/docs/cli/gemini-md/),
[Antigravity](https://www.antigravity.google/docs/rules/).
Les réglages et versions des outils peuvent modifier le chargement ; vérifier les instructions
effectivement reçues lors d'une première utilisation dans un nouveau dépôt.

## Routage : lire selon la tâche

| Tâche | Lecture initiale après AGENTS et état Git | Élargir seulement si nécessaire |
| --- | --- | --- |
| Bug métier d'un plugin | Son guide TECHNICAL, code/tests du chemin concerné ; AGENTS Core et API utilisée | Autres consommateurs si contrat commun affecté |
| Commande ou texte commun | Core TECHNICAL, registre ou traductions et leurs tests | Appels des plugins concernés |
| Configuration/reload | HCPluginFiles/BukkitYaml, loader et remplacement du runtime local | Permissions, sessions ou persistance affectées |
| Nouveau plugin | NEW_PLUGIN, capacités Core et plugin analogue pertinent | CI consumer-template, dépendances réelles |
| Changement d'API Core | Contrat, implémentation/tests, usages ciblés dans clones voisins | ECOSYSTEM et tous consommateurs de cette API |
| Glow/carrier/proxy | GlowProfiles, guides Glowing/ItemFrame/pack et assets concernés | TAB/Nexo effectifs si tâche serveur autorisée |
| CI/cache/release | AGENTS/TECHNICAL/README actions et callers affectés | Gradle du consommateur si contrat build affecté |
| Documentation seule | Section et code de référence | Liens, cohérence et diff ; aucune compilation automatique |

Les guides sont dans `docs/` de leur dépôt. Ne pas charger tous les guides au démarrage ;
chercher un titre/symbole avec `rg`, puis lire la section et les sources utiles.
Le Core absent doit être consulté depuis son dépôt public ; ne pas inventer ses signatures.

## Réduire le contexte sans perdre la preuve

1. Définir objectif et contrainte, puis relever les changements Git existants.
2. Chercher chemins/symboles dans la zone utile ; exclure build, .gradle et artefacts générés.
3. Regrouper les lectures indépendantes ; limiter la sortie aux résultats pertinents.
4. Réutiliser les faits vérifiés pendant la tâche ; relire seulement si les sources ont changé.
5. Changer la cause et son contrat, sans refactoring cosmétique ni abstraction spéculative.
6. Tester le risque concret ; arrêter les tests optionnels une fois ce risque couvert.
7. Documenter le contrat modifié à son emplacement ; ne pas coller des historiques de conversation.
8. Livrer quelques points clairs : changement, validation exécutée, limite/action restante.

Ne pas supprimer tests, détails d'erreur utiles ou vérifications d'API pour économiser des tokens.
Les recherches et builds non exécutés ne sont jamais présentés comme réussis. Aucun gain de
coût chiffré n'est promis ; la réduction vise les lectures répétées et le contexte inutile.

## Résumé de passage à une autre IA

Un résumé court dans la conversation suffit ; ne pas créer de journal permanent sans demande.
Inclure uniquement les éléments nécessaires à la reprise :

```text
Objectif : résultat demandé et contraintes qui restent applicables.
Dépôts : chemins, branches/commits inspectés, commit Core compatible.
État : fichiers modifiés, travail préexistant à préserver, décisions confirmées.
Validation : commandes exécutées et résultats ; scénarios serveur non vérifiés.
Suite : prochaine action précise et information bloquante éventuelle.
Autorisation : opérations Git/serveur explicitement autorisées pendant la session.
```

Ne pas inclure secrets, logs complets ou copie des guides. La nouvelle IA vérifie l'état réel
avant reprise : un résumé décrit une observation passée, pas une garantie sur l'état courant.
