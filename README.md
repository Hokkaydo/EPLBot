# README.md

Ce dépôt contient les sources du bot EPLBot présent sur le discord de l'École Polytechnique de Louvain-la-Neuve (EPL).
___
## Prérequis

Avant de commencer, assurez-vous d'avoir les éléments suivants :

- Java Development Kit (JDK) 21 installé sur votre machine.
- Un compte Discord pour créer un bot et obtenir un jeton d'authentification.
- Gradle installé (ou vous pouvez utiliser la version fournie avec le projet).

## Installation

1. Clonez ce dépôt sur votre machine :

    ```shell
    git clone https://github.com/Hokkaydo/EPLBot.git
    ```

2. Accédez au répertoire du projet :

    ```shell
    cd eplbot
    ```
3. Créez un répertoire pour la persistence:

    ```shell
    mkdir persistence
    ```

4. Compilez le projet en exécutant la commande :

    ```shell
    ./gradlew shadowJar
    ```

5. Renommez le fichier `variables.env.example` en `variables.env` et renseignez-y vos variables d'environnement suivant :
   - `DISCORD_BOT_TOKEN`: Jeton d'identification de votre bot Discord
   - `TEST_DISCORD_ID`: Identifiant du discord sur lequel vous souhaitez tester le bot
   - `GITHUB_APPLICATION_ID`: Identifiant de l'application Github liée (permet de gérer les issues) *(Optionnel)* 
   - `GITHUB_APPLICATION_INSTALLATION_ID`: Identifiant d'installation de l'application Github liée (permet de gérer les issues) *(Optionnel)*
   - `HASTEBIN_TOKEN`: Jeton d'identification auprès de l'API de Hastebin
   
6. Lancez le bot :
 ```shell
      ./gradlew run
   ```

# Docker

Pour exécuter le bot dans un conteneur Docker, voici la marche à suivre.

1. Build l'image Docker

    ```shell
    docker build . -t eplbot
    ```
2. Lancer le docker

    ```shell
        docker run --rm -e docker run --rm  -e DISCORD_BOT_TOKEN=token -e TEST_DISCORD_ID=id-de-votre-discord-de-test -t eplbot
    ```

## Configuration du bot Discord

Le bot propose un système modulaire permettant d'activer et désactiver les modules via les commandes Discord `/enable <module>` et `/disable <module>`.
## Contribution

Les contributions à ce projet sont les bienvenues. Si vous souhaitez apporter des améliorations, veuillez créer une branche à partir de la branche `master`, effectuer vos modifications et soumettre une Pull Request (PR).

Pensez à consulter [CONTRIBUTION.md](CONTRIBUTION.md) afin de comprendre la structure du projet

## Ressources

- Documentation JDA : [https://github.com/DV8FromTheWorld/JDA](https://github.com/DV8FromTheWorld/JDA)
- Tutoriels Discord API : [https://discord.com/developers/docs/intro](https://discord.com/developers/docs/intro)

## Licence

Ce projet est sous licence [GNU GPLv3](https://github.com/Hokkaydo/EPLBot/blob/master/LICENCE).
