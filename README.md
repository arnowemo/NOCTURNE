# NOCTURNE — Android starter 0.1

Premier squelette Android natif (Kotlin + Jetpack Compose) pour NOCTURNE.

## Contenu actuel
- Interface sombre Android native
- Étagère horizontale d'albums de démonstration
- Recherche locale dans ces albums
- Mini lecteur visuel
- Identifiant Android `com.nocturne.music`
- URI de retour prévue : `nocturne://callback`
- GitHub Actions pour compiler une APK debug dans le cloud

## État
Cette version est un socle visuel et technique, pas encore la connexion Spotify.
L'authentification OAuth PKCE, la récupération de la bibliothèque via Spotify Web API et le contrôle de lecture seront ajoutés ensuite.

Le Client ID Spotify est public et configuré dans `app/build.gradle.kts`. Ne jamais ajouter le Client Secret dans l'application.
La clé de signature Android doit être créée et conservée par le propriétaire. L'empreinte SHA-1 configurée dans Spotify doit correspondre à la clé qui signe réellement l'APK distribuée. N'utilisez pas une empreinte SHA-1 fournie sans vérification.
