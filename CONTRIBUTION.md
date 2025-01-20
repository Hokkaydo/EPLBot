# Structure du projet
## Modules
Comme précisé dans le [README.md](README.md), le projet est découpé en modules.

Chaque module est composé :
- d'un package propre
- d'une classe mère qui étend la classe [`Module`](src/main/java/com/github/hokkaydo/eplbot/module/Module.java)
- de commandes qui implémentent l'interface [`Command`](src/main/java/com/github/hokkaydo/eplbot/command/Command.java) *(optionnel)*
- de *listeners* qui étendent la classe `ListenerAdapter` *(propre à JDA)* *(optionnel)*

### Classe mère
La classe abstraite [`Module`](src/main/java/com/github/hokkaydo/eplbot/module/Module.java), définit plusieurs méthodes que la classe mère doit implémenter.
- `String getName()`: Renvoie le nom du module
- `List<Command> getCommands()`: Renvoie une liste des commandes du module
- `List<ListenerAdaptater> getListeners()`: Renvoie une liste des *listeners* du module
Ces 3 méthodes à redéfinir permettent de charger correctement le module et d'identifier ses fonctionnalités.

La classe mère doit être enregistrée dans la classe principale du bot : [`Main`](src/main/java/com/github/hokkaydo/eplbot/Main.java). Pour ce faire, il suffit d'ajouter la 
classe du module à une des listes `globalModules` ou `eplModules` (en fonction de la destination du module) dans la méthode 
`void registerModules()`.

De plus, le nom du module doit être enregistré dans la classe [`Config`](src/main/java/com/github/hokkaydo/eplbot/configuration/Config.java) dans la `Map<String, ConfigurationParser> DEFAULT_CONFIGURATION`.

:warning: Il est à noter qu'une instance d'un module est spécifique à un serveur *(guild)* particulier. Plusieurs instances 
d'un même module pourraient s'exécuter en même temps. Il est donc nécessaire de bien vérifier dans chaque `ListenerAdaptater`
que vous êtes bien dans la guilde ciblée afin de ne pas exécuter le même code trois fois si trois serveurs ont activé ce module en même temps.

#### Module de code

- La tâche Gradle `buildRunners` permet de build les dockers pour la commande [`code`](src/main/java/com/github/hokkaydo/eplbot/module/code/command/CodeCommand.java). Il n'est pas nécessaire de l'utiliser si vous ne comptez pas travailler sur le module de [`code`](src/main/java/com/github/hokkaydo/eplbot/module/code/CodeModule.java)

###### Fonctionnement
Pour chaque commande une classe [`Runner`](src/main/java/com/github/hokkaydo/eplbot/module/code/Runner.java) est appelée, cette classe s'occupe de formatter le code si nécessaire
et de faire tourner un nouveau docker temporaire dont la sortie est capturée et rendue à l'utilisateur au travers de la commande [`CodeCommand`](src/main/java/com/github/hokkaydo/eplbot/module/code/command/CodeCommand.java)

Pour rajouter un nouveau langage, il suffit de créer un dossier dans [`src/main/java/com/github/hokkaydo/eplbot/module/code/`](src/main/java/com/github/hokkaydo/eplbot/module/code) avec un `Dockerfile`
contenant les informations relatives au langage à rajouter.
Il faut également rajouter un `.sh` (pour compiler et run dans le docker) pour exécuter le code et tous les autres fichiers nécessaires ex:[`requirements.txt`](src/main/java/com/github/hokkaydo/eplbot/module/code/python/requirements.txt)
Pour le `Dockerfile` et le `.sh`, vous pouvez prendre exemple sur ceux déjà faits.
Il faut ensuite ajouter le nom du langage avec la classe Runner dans [`RUNNER_MAP`](src/main/java/com/github/hokkaydo/eplbot/module/code/command/CodeCommand.java).
La classe [`GlobalRunner.java`](src/main/java/com/github/hokkaydo/eplbot/module/code/GlobalRunner.java) permet de lancer un docker et peut servir d'exemple. 
Il est à noter que cette classe prend en argument le nom du docker qu'il faut exécuter, ce dernier doit être indiqué dans `build_code_docker.sh`

### Commandes
L'interface `Command` définit une série de méthodes à réimplémenter. Nous vous renvoyons vers la documentation de celles-ci.
### Listeners
Les *listeners* d'un module permettent de réagir aux divers évènements que le bot intercepte. Une classe *listener* doit
étendre la classe abstraite `ListenerAdaptater`. Pour une description complète des évènements existants, nous vous renvoyons 
vers la documentation de celle-ci.

## Base de données
Le système de sauvegarde utilisé est une base de données SQLite locale.
### Config
Un système de configuration vous permet de sauver et récupérer différentes valeurs, même après un redémarrage du bot.
Il vous suffit d'utiliser les méthodes présentes dans la classe [`Config`](src/main/java/com/github/hokkaydo/eplbot/configuration/Config.java) après avoir rajouté la clé de sauvegarde de votre valeur
dans la `Map<String, ConfigurationParser> DEFAULT_CONFIGURATION`.

### Sauvegarde avancée
Il vous est possible de créer une table dédiée à votre module. Pour cela, il vous faudra créer 2 sous-*packages* :
- `module/model`: Vous y placerez vos modèles, généralement des `Record` contenant seulement les champs nécessaires
- `modules/repository`: Vous y définirez une paire interface-classe par *repository* (table) souhaité :
  - Une interface `MyRepository` qui étend l'interface [`CRUDRepository<MyModel>`](src/main/java/com/github/hokkaydo/eplbot/database/CRUDRepository.java) et définit les éventuelles méthodes supplémentaires nécessaires
  - Une classe `MyRepositorySQLite` qui implémente l'interface `MyRepository`. Nous vous renvoyons vers la documentation de `CRUDRepository` pour plus d'informations.

Le nom de cette table doit être enregistré dans la classe [`DatabaseManager`](src/main/java/com/github/hokkaydo/eplbot/database/DatabaseManager.java).

Le package d'un module doit donc toujours suivre la structure suivante :
```
module/
    ...
    example/
        repository/
            MyRepository.java
            MyRepositorySQLite.java
        model/
            MyModel.java
      ExampleModule.java
      Listener1.java
      Command1.java
```

## Messages
### Strings
Ce projet propose une centralisation des messages dans [`resources/locales`](src/main/resources/locales). 
Une internationalisation est possible en rajoutant un fichier `<language>.json` dans ce dossier.

Une fois votre message défini, il vous est possible de le récupérer par sa clé via la classe [`Strings`](src/main/java/com/github/hokkaydo/eplbot/Strings.java) et sa méthode `Strings.getString(String key)`

### `MessageUtil`
Cette [classe](src/main/java/com/github/hokkaydo/eplbot/MessageUtil.java) vous propose différentes méthodes afin de formatter un message, un embed ou encore 
d'envoyer automatiquement un message dans le salon administrateur.

# Utilisation et test

## Build

### Gradle
Gradle étant le gestionnaire de dépendances utilisé, il est vivement recommandé de l'utiliser pour build le projet.
La tâche `buildAndRun` permet de build le projet, créer une image du bot, créer un conteneur Docker et le lancer.

### Script 
Il est possible de build le projet sans avoir Gradle installé sur sa machine, dans ce cas il suffit de supprimer l'argument `--target local-build` de la ligne `docker-compose`.

## Utilisation
### Commandes utiles
- `/refreshcommands`: Permet de resynchroniser les commandes du bot sur votre serveur (à faire après ajout/retrait d'une commande uniquement)
- `/enable|disable <module>`: Permet d'activer|désactiver un module
- `/config <key> <value>`: Permet de modifier les valeurs de configuration
  - Omettre `key` et `value` affiche l'état actuel de la configuration
  - Omettre `value` affiche la valeur actuelle pour la clé donnée
  - Omettre `key` affiche une erreur

### Debug
:warning: Pensez à bien lire la sortie dans votre console, les erreurs et informations de débug y seront affichées !