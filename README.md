# 📝 AnnotPlatform — Plateforme d'annotation NLP

## Comptes utilisateurs (initialisés automatiquement)

| Login   | Mot de passe | Rôle          |
|---------|-------------|---------------|
| `admin` | `admin`     | Administrateur |
| `user1` | `user1`     | Annotateur     |
| `user2` | `user2`     | Annotateur     |
| `user3` | `user3`     | Annotateur     |

---

## Lancement avec Docker 

```bash
# Construire et démarrer
docker-compose up --build

# Accéder à l'application
http://localhost:8080
```

---

## Lancement du JAR directement

> Prérequis : Java 17 installé + MySQL accessible

```bash
# Compiler
mvn clean package -DskipTests

# Lancer 
java -jar target/app.jar
```

Les données (rôles et comptes) sont **initialisées automatiquement au démarrage**.

---

## Lancement avec IntelliJ IDEA

1. Ouvrir le dossier `annotation-platform` comme projet Maven
2. Démarrer MySQL (via Docker : `docker-compose up db`)
3. Lancer `AnnotationPlatformApplication.java`

---

## Format des fichiers d'import

### CSV — mono-texte (Sentiment Analysis)
```csv
texte1
"Ce film est excellent !"
"Je suis très déçu."
```

### CSV — bi-texte (NLI)
```csv
texte1,texte2
"Les oiseaux volent.","Les oiseaux sont des animaux."
"Il pleut dehors.","Le soleil brille."
```

### JSON — bi-texte
```json
[
  {"textes": ["Les oiseaux volent.", "Les oiseaux sont des animaux."]},
  {"textes": ["Il pleut dehors.", "Le soleil brille."]}
]
```
