# README.md

Ce dépôt contient les sources du bot EPLBot présent sur le discord de l'École Polytechnique de Louvain-la-Neuve (EPL).
___

# Installation
## Prérequis

Avant de commencer, assurez-vous d'avoir les éléments suivants :

- Java Development Kit (JDK) 23
- Docker
- Un compte Discord pour créer un bot et obtenir un jeton d'authentification
- Gradle si vous souhaitez compiler le projet localement

## Procédure

1. Clonez ce dépôt sur votre machine :

    ```shell
    git clone https://github.com/Hokkaydo/EPLBot.git
    ```

2. Accédez au répertoire du projet :

    ```shell
    cd EPLbot/
    ```
3. Créez un répertoire pour la persistence:

    ```shell
    mkdir data/
    ```

4. Renommez le fichier `variables.env.example` en `variables.env` et renseignez-y vos variables d'environnement suivant :
    - `DISCORD_BOT_TOKEN`: Jeton d'identification de votre bot Discord
    - `TEST_DISCORD_ID`: Identifiant du discord sur lequel vous souhaitez tester le bot
    - `GITHUB_APPLICATION_ID`: Identifiant de l'application Github liée (permet de gérer les issues) *(Optionnel)*
    - `GITHUB_APPLICATION_INSTALLATION_ID`: Identifiant d'installation de l'application Github liée (permet de gérer les issues) *(Optionnel)*
    - `HASTEBIN_TOKEN`: Jeton d'identification auprès de l'API de Hastebin
    - `CONTRIBUTIONS_REMOTE`: Dossier des contributions du Drive EPL au format rclone, ex. `onedrive:Fichiers de Maxime Drooghaag - Drive EPL/Contributions EPL-Drive` *(Optionnel, module `contributions`)*

5. Lancez le projet avec Docker :
    ```shell
    docker-compose up
    ```
---
# Veille des contributions du Drive EPL *(optionnel)*

Le module `contributions` annonce chaque nouveau fichier du dossier des contributions dans le salon `DRIVE_ADMIN_CHANNEL_ID` (vérification toutes les heures, `CONTRIBUTIONS_UPDATE_PERIOD`), en mentionnant éventuellement le rôle `CONTRIBUTIONS_ROLE_ID`. La commande `/contributions` liste les fichiers pas encore importés.

Il lit le Drive via [rclone](https://rclone.org), inclus dans l'image Docker, dont la configuration est lue depuis `data/rclone.conf` :

1. Sur une machine avec navigateur, créer un token en **lecture seule** :
    ```shell
    rclone authorize "onedrive" --onedrive-access-scopes "Files.Read Files.Read.All Sites.Read.All offline_access"
    ```
2. Sur le serveur, créer le remote avec ce token (type `onedrive`, même option `access_scopes`) via le rclone de l'image, qui écrit dans `data/rclone.conf` :
    ```shell
    docker-compose run --rm --entrypoint rclone eplbot config
    ```
    rclone crée ce fichier lisible uniquement par root (droits 600). Il donne accès au OneDrive du compte utilisé : ne jamais le committer ni le partager.
3. Renseigner `CONTRIBUTIONS_REMOTE` dans `variables.env`, puis `/enable contributions`.

Au premier passage, le bot enregistre les fichiers existants sans les annoncer un par un. Si le token expire (90 jours sans usage, changement de mot de passe), une erreur est envoyée dans le salon administrateur : `docker-compose run --rm --entrypoint rclone eplbot config reconnect <remote>:`.

---
# Configuration du bot Discord

Le bot propose un système modulaire permettant d'activer et désactiver les modules via les commandes Discord `/enable <module>` et `/disable <module>`.

La commande `/config` permet de configurer les paramètres des modules.
# Contribution

Les contributions à ce projet sont les bienvenues. Si vous souhaitez apporter des améliorations, veuillez créer une branche à partir de la branche `master`, effectuer vos modifications et soumettre une Pull Request (PR).

Pensez à consulter [CONTRIBUTION.md](CONTRIBUTION.md) afin de comprendre la structure du projet

# Ressources

- Documentation JDA : [https://github.com/DV8FromTheWorld/JDA](https://github.com/DV8FromTheWorld/JDA)
- Tutoriels Discord API : [https://discord.com/developers/docs/intro](https://discord.com/developers/docs/intro)

## Licence

Ce projet est sous licence [GNU GPLv3](https://github.com/Hokkaydo/EPLBot/blob/master/LICENCE).
