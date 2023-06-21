# README.md

Ce dépôt contient les sources du bot EPLBot présent sur le discord de l'École Polytechnique de Louvain-la-Neuve (EPL).
___
## Prérequis

Avant de commencer, assurez-vous d'avoir les éléments suivants :

- Java Development Kit (JDK) 17 installé sur votre machine.
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

3. Compilez le projet en exécutant la commande :

    ```shell
    ./gradlew shadowJar
    ```

4. Ajoutez les variables d'environnement :

    ```shell
    DISCORD_BOT_TOKEN=votre-jeton
    TEST_DISCORD_ID=id-de-votre-discord-de-test
    ```

5. Exécutez le bot Discord en utilisant la commande :

    ```shell
      ./gradlew run
    ```

## Configuration du bot Discord

Le bot propose un système modulaire permettant d'activer et désactiver les modules via les commandes Discord `/enable <module>` et `/disable <module>`.
## Contribution

Les contributions à ce projet sont les bienvenues. Si vous souhaitez apporter des améliorations, veuillez créer une branche à partir de la branche `master`, effectuer vos modifications et soumettre une Pull Request (PR).

Afin de savoir ce qui est prévu ou en cours, n'hésitez pas à consulter le [kanban](https://github.com/users/Hokkaydo/projects/3/views/1) du projet.
## Ressources

- Documentation JDA : [https://github.com/DV8FromTheWorld/JDA](https://github.com/DV8FromTheWorld/JDA)
- Tutoriels Discord API : [https://discord.com/developers/docs/intro](https://discord.com/developers/docs/intro)

## Licence

Ce projet est sous licence [GNU GPLv3](https://github.com/Hokkaydo/EPLBot/blob/master/LICENCE).
