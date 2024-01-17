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
- `List<Command> getCommands()`: Renvoie une liste des commandes du modules
- `List<ListenerAdaptater> getListeners()`: Renvoie une liste des *listeners* du modules
Ces 3 méthodes à redéfinir permettent de charger correctement le module et d'identifier ses fonctionnalités.

La classe mère doit être enregistrée dans la classe principale du bot : [`Main`](src/main/java/com/github/hokkaydo/eplbot/Main.java). Pour ce faire, il suffit d'ajouter la 
classe du module à une des liste `globalModules` ou `eplModules` (en fonction de la destination du module) dans la méthode 
`void registerModules()`.

De plus, le nom du module doit être enregistré dans la classe [`Config`](src/main/java/com/github/hokkaydo/eplbot/configuration/Config.java) dans la `Map<String, ConfigurationParser> DEFAULT_CONFIGURATION`.

:warning: Il est à noter qu'une instance d'un module est spécifique à un serveur *(guild)* particulier. Plusieurs instances 
d'un même module pourraient s'exécuter en même temps. Il est donc nécessaire de bien vérifier dans chaque `ListenerAdaptater`
que vous êtes bien dans la guilde ciblée afin de ne pas exécuter le même code 3 fois si 3 serveurs ont activés ce module en même temps.

### Commandes
L'interface `Command` définie une série de méthodes à réimplémenter. Nous vous renvoyons vers la documentation de celles-ci.

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
Il vous est possible de créer une table dédiée à votre module. Pour cela, il vous faudra créer 2 sous-*packages*:
- `module/model`: Vous y placerez vos modèles, généralement des `Record` contenant seulement les champs nécessaires
- `modules/repository`: Vous y définirez une paire interface-classe par *repository* (table) souhaité :
  - Une interface `MyRepository` qui étend l'interface [`CRUDRepository<MyModel>`](src/main/java/com/github/hokkaydo/eplbot/database/CRUDRepository.java) et définit les éventuelles méthodes supplémentaires nécessaires
  - Une classe `MyRepositorySQLite` qui implémente l'interface `MyRepository`. Nous vous renvoyons vers la documentation de `CRUDRepository` pour plus d'informations.

## Messages
### Strings
Ce projet utilise propose une centralisation des messages dans [`resources/strings.json`](src/main/resources/strings.json). 
Une fois votre message défini, il vous est possible de le récupérer par sa clé via la classe [`Strings`](src/main/java/com/github/hokkaydo/eplbot/Strings.java) et sa méthode `Strings.getString(String key)`

### `MessageUtil`
Cette [classe](src/main/java/com/github/hokkaydo/eplbot/MessageUtil.java) vous propose différentes méthodes afin de formatter un message, un embed ou encore 
d'envoyer automatiquement un message dans le salon administrateur.

# Utilisation et test

### Utilitaires
- L'utilitaire [`build.sh`](build.sh) permet de build le projet, créer une image du bot, créer un conteneur Docker et le lancer.
- L'utilitaire [`updatehub.sh`](updatehub.sh) permet de build le projet, crée une image du bot et la push sur DockerHub afin de mettre à jour l'image existante (nécessite les credentials)

### Commandes utiles
- `/refreshcommands`: Permet de resynchroniser les commandes du bot sur votre serveur (à faire après ajout/retrait d'une commande uniquement)
- `/enable|disable <module>`: Permet d'activer|désactiver un module
- `/config <key> <value>`: Permet de modifier les valeurs de configuration
  - Omettre `key` et `value` affiche l'état actuel de la configuration
  - Omettre `value` affiche la valeur actuelle pour la clé donnée
  - Omettre `key` affiche une erreur

### Debug
:warning: Pensez à bien lire la sortie dans votre console, les erreurs et informations de débug y seront affichées !