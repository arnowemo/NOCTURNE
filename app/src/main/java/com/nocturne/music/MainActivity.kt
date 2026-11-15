package com.nocturne.music

import android.os.Bundle
import android.content.Intent
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val Night = Color(0xFF08080B)
private val Stone = Color(0xFF17151A)
private val Gold = Color(0xFFC5A875)
private val Parchment = Color(0xFFE8DDC9)
private val Ash = Color(0xFFA5A0A8)

private data class AlbumTrack(val name: String, val uri: String)
private data class Album(val title: String, val artist: String, val tint: Color, val symbol: String, val imageUrl: String? = null, val spotifyId: String? = null, val spotifyUri: String? = null)
private val albums = listOf(
    Album("The Earth Is Not a Cold Dead Place", "Explosions in the Sky", Color(0xFF777968), "✦"),
    Album("Ágætis byrjun", "Sigur Rós", Color(0xFF987C75), "☾"),
    Album("Departure Songs", "We Lost The Sea", Color(0xFF526875), "✧"),
    Album("All of a Sudden I Miss Everyone", "Explosions in the Sky", Color(0xFF705665), "✦"),
    Album("Young Mountain", "This Will Destroy You", Color(0xFF82785F), "⌁")
)

private const val SPOTIFY_REQUEST_CODE = 7301

class MainActivity : ComponentActivity() {
    private var spotifyToken by mutableStateOf<String?>(null)
    private var spotifyAuthStatus by mutableStateOf("Connexion non démarrée")
    private var spotifyRemote: SpotifyAppRemote? = null
    private var pendingSpotifyUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("nocturne_spotify", MODE_PRIVATE)
        val savedToken = prefs.getString("access_token", null)
        val expiresAt = prefs.getLong("expires_at", 0L)
        if (!savedToken.isNullOrBlank() && System.currentTimeMillis() < expiresAt) { spotifyToken = savedToken; spotifyAuthStatus = "Session Spotify restaurée" } else { prefs.edit().remove("access_token").remove("expires_at").apply() }
        setContent { NocturneApp(spotifyToken = spotifyToken, spotifyAuthStatus = spotifyAuthStatus, onSpotifyLogin = { connectSpotify() }, onPlaySpotifyUri = { playSpotifyUri(it) }, onPauseSpotify = { spotifyRemote?.playerApi?.pause() }) }
        if (spotifyToken != null) connectSpotifyRemote()
    }

    private fun connectSpotify() {
        spotifyAuthStatus = "Ouverture de Spotify…"
        try {
        val request = AuthorizationRequest.Builder(
            BuildConfig.SPOTIFY_CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            BuildConfig.SPOTIFY_REDIRECT_URI
        ).setScopes(arrayOf("user-library-read", "user-library-modify", "user-read-private"))
            .setShowDialog(true)
            .build()
        AuthorizationClient.openLoginActivity(this, SPOTIFY_REQUEST_CODE, request)
        } catch (e: Exception) {
            spotifyAuthStatus = "Impossible de lancer Spotify : ${e.javaClass.simpleName} ${e.message ?: ""}"
        }
    }

    @Deprecated("Spotify auth library returns its result through this callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPOTIFY_REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, data)
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> { spotifyToken = response.accessToken; val lifetimeMs = (response.expiresIn.coerceAtLeast(60) - 60).toLong() * 1000L; getSharedPreferences("nocturne_spotify", MODE_PRIVATE).edit().putString("access_token", response.accessToken).putLong("expires_at", System.currentTimeMillis() + lifetimeMs).apply(); spotifyAuthStatus = "Connexion réussie — session mémorisée"; connectSpotifyRemote() }
                AuthorizationResponse.Type.ERROR -> { spotifyToken = null; spotifyAuthStatus = "Erreur Spotify : ${response.error ?: "non précisée"}" }
                AuthorizationResponse.Type.CANCELLED -> { spotifyToken = null; spotifyAuthStatus = "Connexion annulée" }
                else -> { spotifyToken = null; spotifyAuthStatus = "Réponse Spotify inattendue : ${response.type}" }
            }
        }
    }

    private fun connectSpotifyRemote() {
        if (spotifyRemote != null) return
        spotifyAuthStatus = "Autorisation du lecteur Spotify…"
        val params = ConnectionParams.Builder(BuildConfig.SPOTIFY_CLIENT_ID).setRedirectUri(BuildConfig.SPOTIFY_REDIRECT_URI).showAuthView(true).build()
        SpotifyAppRemote.connect(this, params, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyRemote = appRemote
                spotifyAuthStatus = "Spotify prêt pour la lecture"
                pendingSpotifyUri?.let { uri -> appRemote.playerApi.play(uri); pendingSpotifyUri = null }
            }
            override fun onFailure(error: Throwable) { spotifyAuthStatus = "Lecteur Spotify indisponible : " + (error.message ?: error.javaClass.simpleName) }
        })
    }
    private fun playSpotifyUri(uri: String) {
        val remote = spotifyRemote
        if (remote != null) remote.playerApi.play(uri) else { pendingSpotifyUri = uri; connectSpotifyRemote() }
    }
    override fun onStop() {
        spotifyRemote?.let { try { it.playerApi.pause() } catch (_: Exception) { }; SpotifyAppRemote.disconnect(it) }
        spotifyRemote = null
        super.onStop()
    }
}

@Composable
fun NocturneApp(spotifyToken: String?, spotifyAuthStatus: String, onSpotifyLogin: () -> Unit, onPlaySpotifyUri: (String) -> Unit, onPauseSpotify: () -> Unit) {
    var selected by remember { mutableStateOf(albums.first()) }
    var collection by remember { mutableStateOf(albums) }
    var openedAlbum by remember { mutableStateOf<Album?>(null) }
    var playingTrack by remember { mutableStateOf(-1) }
    var spotifyMessage by remember { mutableStateOf("Connectez Spotify pour afficher vos vrais albums") }
    var loadingCollection by remember { mutableStateOf(false) }
    var roomTracks by remember { mutableStateOf<List<AlbumTrack>>(emptyList()) }
    LaunchedEffect(openedAlbum?.spotifyId, spotifyToken) {
        val current = openedAlbum
        if (current?.spotifyId != null && spotifyToken != null) { roomTracks = emptyList(); try { roomTracks = loadAlbumTracks(spotifyToken, current.spotifyId) } catch (_: Exception) { roomTracks = emptyList() } }
        else roomTracks = emptyList()
    }

    LaunchedEffect(spotifyToken) {
        if (spotifyToken != null) {
            loadingCollection = true
            spotifyMessage = "Chargement de votre collection Spotify…"
            try {
                val fetched = loadSavedAlbums(spotifyToken)
                if (fetched.isNotEmpty()) {
                    collection = fetched
                    selected = fetched.first()
                    spotifyMessage = "${fetched.size} albums récupérés depuis Spotify"
                } else {
                    spotifyMessage = "Aucun album enregistré trouvé sur ce compte"
                }
            } catch (e: Exception) {
                spotifyMessage = "Spotify : ${e.message ?: "impossible de charger la collection"}"
            } finally {
                loadingCollection = false
            }
        } else {
            collection = albums
            spotifyMessage = "Connectez Spotify pour afficher vos vrais albums"
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Night, surface = Stone, primary = Gold)) {
        Box(Modifier.fillMaxSize().background(Night)) {
            AsyncImage(model="https://images.unsplash.com/photo-1461360228754-6e81c478b882?auto=format&fit=crop&w=1400&q=85",contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xA9080709),Color(0xD9080709),Color(0xF9080709),Night))))
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0x667D4D2C),Color.Transparent),radius=1050f)))
            AnimatedVisibility(visible=openedAlbum==null,enter=fadeIn(tween(500)),exit=fadeOut(tween(260))) {
                Column(Modifier.fillMaxSize().padding(top=30.dp,bottom=24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.SpaceBetween) {
                    Column(Modifier.fillMaxWidth().padding(horizontal=26.dp)) {
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                            Column {
                                Text("N O C T U R N E",color=Parchment,fontSize=13.sp,fontWeight=FontWeight.Bold,letterSpacing=5.sp)
                                Spacer(Modifier.height(7.dp))
                                Text("LA COLLECTION PRIVÉE",color=Gold,fontSize=9.sp,letterSpacing=3.sp)
                            }
                            TextButton(onClick=onSpotifyLogin) {
                                Text(if(spotifyToken==null) "CONNECTER SPOTIFY" else "SPOTIFY CONNECTÉ",color=Gold,fontSize=9.sp,letterSpacing=1.sp)
                            }
                        }
                        Text(spotifyMessage,color=Ash,fontSize=9.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Text(spotifyAuthStatus,color=Gold.copy(alpha=.85f),fontSize=8.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
                        if(loadingCollection) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=5.dp),color=Gold,trackColor=Stone)
                    }
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Text("Prenez un disque.",color=Parchment,fontSize=27.sp,fontWeight=FontWeight.SemiBold)
                        Spacer(Modifier.height(7.dp))
                        Text("Faites défiler votre collection.",color=Gold.copy(alpha=.82f),fontSize=12.sp,letterSpacing=1.sp)
                    }
                    AlbumCarousel(items=collection,selected=selected,onSelect={selected=it;openedAlbum=it},onFocus={selected=it})
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically) {
                            Icon(Icons.Default.Swipe,null,tint=Gold,modifier=Modifier.size(17.dp))
                            Text("GLISSEZ DE GAUCHE À DROITE",color=Gold.copy(alpha=.85f),fontSize=9.sp,letterSpacing=2.sp)
                        }
                        Spacer(Modifier.height(13.dp))
                        Text("TOUCHEZ LA POCHETTE POUR ÉCOUTER",color=Ash.copy(alpha=.7f),fontSize=8.sp,letterSpacing=1.5.sp)
                    }
                }
            }
            AnimatedVisibility(visible=openedAlbum!=null,enter=fadeIn(tween(300))+slideInVertically(initialOffsetY={it/6},animationSpec=tween(450,easing=FastOutSlowInEasing)),exit=fadeOut(tween(200))) {
                openedAlbum?.let{album->AlbumRoomScreen(album, roomTracks, {openedAlbum=null}, playingTrack, { playingTrack=it }, onPlaySpotifyUri, onPauseSpotify)}
            }
        }
    }
}

private suspend fun loadSavedAlbums(token: String): List<Album> = withContext(Dispatchers.IO) {
    val connection = (URL("https://api.spotify.com/v1/me/albums?limit=50").openConnection() as HttpURLConnection)
    try {
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) throw IllegalStateException("erreur API $code")
        val items = JSONObject(body).optJSONArray("items") ?: return@withContext emptyList()
        val colors = listOf(Color(0xFF777968), Color(0xFF987C75), Color(0xFF526875), Color(0xFF705665), Color(0xFF82785F))
        val symbols = listOf("✦", "☾", "✧", "✦", "⌁")
        buildList {
            for (i in 0 until items.length()) {
                val album = items.getJSONObject(i).getJSONObject("album")
                val images = album.optJSONArray("images")
                val image = if (images != null && images.length() > 0) images.getJSONObject(0).optString("url").takeIf { it.isNotBlank() } else null
                val artists = album.optJSONArray("artists")
                val artist = if (artists != null && artists.length() > 0) artists.getJSONObject(0).optString("name") else "Artiste inconnu"
                val albumId = album.optString("id").takeIf { it.isNotBlank() }
                add(Album(album.optString("name", "Album sans titre"), artist, colors[i % colors.size], symbols[i % symbols.size], image, albumId, albumId?.let { "spotify:album:" + it }))
            }
        }
    } finally {
        connection.disconnect()
    }
}

@Composable
private fun AlbumCarousel(items:List<Album>,selected:Album,onSelect:(Album)->Unit,onFocus:(Album)->Unit) {
    val scroll=rememberScrollState()
    val density= androidx.compose.ui.platform.LocalDensity.current
    val cardWidth=250.dp
    val gap=18.dp
    val stepPx=with(density){(cardWidth+gap).toPx()}
    LaunchedEffect(scroll.value) {
        kotlinx.coroutines.delay(90)
        val index=((scroll.value/stepPx).toInt()).coerceIn(0,items.lastIndex)
        items.getOrNull(index)?.let(onFocus)
    }
    BoxWithConstraints(Modifier.fillMaxWidth().height(430.dp),contentAlignment=Alignment.Center) {
        val sidePad=(maxWidth-cardWidth)/2
        Row(Modifier.fillMaxWidth().horizontalScroll(scroll),verticalAlignment=Alignment.CenterVertically) {
            Spacer(Modifier.width(sidePad))
            items.forEachIndexed{index,album->
                val offset=(scroll.value/stepPx)-index
                val distance=kotlin.math.abs(offset).coerceIn(0f,1.5f)
                val focused=album==selected
                Box(Modifier.width(cardWidth).padding(end=gap).graphicsLayer {
                    val d=offset.coerceIn(-1.4f,1.4f)
                    rotationY=d*-25f
                    rotationZ=d*-2.5f
                    scaleX=1f-distance*.17f
                    scaleY=1f-distance*.17f
                    translationX=d*with(density){18.dp.toPx()}
                    cameraDistance=22f*density.density
                    shadowElevation=(1.5f-distance*.5f).coerceAtLeast(.2f)*28f
                    alpha=1f-distance*.22f
                }.clickable{onSelect(album)}) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF160F12)).border(if(focused)2.dp else 1.dp,if(focused)Gold else Gold.copy(alpha=.3f),RoundedCornerShape(5.dp)).padding(7.dp)) {
                            AlbumArtwork(album,Modifier.fillMaxSize())
                            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0x66FFFFFF),Color.Transparent,Color(0x88000000)))))
                            if(focused) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(4.dp).background(Brush.horizontalGradient(listOf(Color.Transparent,Gold,Color.Transparent))))
                        }
                        Spacer(Modifier.height(15.dp))
                        Text(album.title,color=Parchment,fontSize=13.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(album.artist,color=Ash,fontSize=10.sp,maxLines=1)
                    }
                }
            }
            Spacer(Modifier.width(sidePad))
        }
    }
}

private suspend fun loadAlbumTracks(token: String, albumId: String): List<AlbumTrack> = withContext(Dispatchers.IO) {
    val connection = URL("https://api.spotify.com/v1/albums/" + albumId + "/tracks?limit=50").openConnection() as HttpURLConnection
    try {
        connection.setRequestProperty("Authorization", "Bearer " + token); connection.connectTimeout = 15000; connection.readTimeout = 15000
        val code = connection.responseCode; val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) throw IllegalStateException("erreur pistes " + code)
        val items = JSONObject(body).optJSONArray("items") ?: return@withContext emptyList()
        buildList { for (i in 0 until items.length()) { val item = items.getJSONObject(i); val id = item.optString("id"); if (id.isNotBlank()) add(AlbumTrack(item.optString("name", "Piste " + (i+1)), "spotify:track:" + id)) } }
    } finally { connection.disconnect() }
}

@Composable
private fun AlbumRoomScreen(album: Album, spotifyTracks: List<AlbumTrack>, onBack: () -> Unit, playingTrack: Int, setPlayingTrack: (Int) -> Unit, onPlaySpotifyUri: (String) -> Unit, onPauseSpotify: () -> Unit) {
    val tracks = spotifyTracks.map { it.name }.ifEmpty { listOf("Ouvrir l album dans Spotify") }
    val transition = rememberInfiniteTransition(label = "vinyl")
    val rotation by transition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(tween(9000, easing = androidx.compose.animation.core.LinearEasing)), label = "vinyl-spin")
    val pulse by transition.animateFloat(initialValue = .35f, targetValue = .8f, animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "room-pulse")
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF25191D), Night, Color(0xFF050507)))).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Retour", tint = Gold) }
            Column {
                Text("NOCTURNE  /  SALLE D'ÉCOUTE", color = Gold, fontSize = 9.sp, letterSpacing = 2.sp)
                Text("PRENEZ PLACE, LA PIÈCE EST À VOUS", color = Ash, fontSize = 7.sp, letterSpacing = 1.4.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(350.dp).clip(RoundedCornerShape(26.dp)).background(Brush.radialGradient(listOf(Color(0xFF69463F).copy(alpha=pulse),Color(0xFF211820),Night))).border(1.dp, Gold.copy(alpha=.38f), RoundedCornerShape(26.dp)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(292.dp).graphicsLayer { rotationZ = rotation }.background(Brush.radialGradient(listOf(Color(0xFF38343A),Color(0xFF141217),Color(0xFF050507)), radius=520f), androidx.compose.foundation.shape.CircleShape).border(1.dp, Color.White.copy(alpha=.12f), androidx.compose.foundation.shape.CircleShape))
            Box(Modifier.size(292.dp).graphicsLayer { rotationZ = rotation }.padding(12.dp).border(1.dp, Color.White.copy(alpha=.10f), androidx.compose.foundation.shape.CircleShape))
            Box(Modifier.size(292.dp).graphicsLayer { rotationZ = rotation }.padding(34.dp).border(.7.dp, Color.White.copy(alpha=.10f), androidx.compose.foundation.shape.CircleShape))
            Box(Modifier.size(292.dp).graphicsLayer { rotationZ = rotation }.padding(52.dp).border(.7.dp, Color.White.copy(alpha=.08f), androidx.compose.foundation.shape.CircleShape))
            Box(Modifier.size(292.dp).graphicsLayer { rotationZ = rotation }.padding(70.dp).border(.7.dp, Color.White.copy(alpha=.08f), androidx.compose.foundation.shape.CircleShape))
            Box(Modifier.size(104.dp).graphicsLayer { rotationZ = rotation }.clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color(0xFF17121A))
                .border(2.dp,Gold.copy(alpha=.85f),androidx.compose.foundation.shape.CircleShape)
                .padding(5.dp),contentAlignment=Alignment.Center) {
                if (album.imageUrl != null) {
                    AsyncImage(model=album.imageUrl,contentDescription="Macaron du vinyle : ${album.title}",
                        contentScale=ContentScale.Crop,
                        modifier=Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape))
                } else {
                    Box(Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Brush.linearGradient(listOf(album.tint,Color(0xFF17121A)))),
                        contentAlignment=Alignment.Center) { Text(album.symbol,color=Gold,fontSize=30.sp) }
                }
                Box(Modifier.size(7.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Night).border(1.dp,Gold,androidx.compose.foundation.shape.CircleShape))
            }
            Box(Modifier.align(Alignment.TopCenter).padding(top=18.dp).width(90.dp).height(2.dp).background(Brush.horizontalGradient(listOf(Color.Transparent,Gold,Color.Transparent))))
            Box(Modifier.align(Alignment.TopEnd).padding(top=32.dp,end=42.dp).width(112.dp).height(180.dp).graphicsLayer { rotationZ = if(playingTrack>=0) 8f else 0f; transformOrigin=androidx.compose.ui.graphics.TransformOrigin(.88f,.08f) }) {
                Box(Modifier.align(Alignment.TopEnd).width(8.dp).height(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Brush.radialGradient(listOf(Color(0xFFEAD8B0),Color(0xFF715B3D)))))
                Box(Modifier.align(Alignment.TopEnd).padding(top=5.dp,end=3.dp).width(4.dp).height(148.dp).graphicsLayer { rotationZ=12f }.background(Brush.verticalGradient(listOf(Color(0xFFE4D0A5),Color(0xFF66523C),Color(0xFFD7C49A))),RoundedCornerShape(4.dp)))
                Box(Modifier.align(Alignment.BottomStart).padding(start=9.dp,bottom=16.dp).width(12.dp).height(22.dp).background(Color(0xFFB69A69),RoundedCornerShape(3.dp)))
            }
            Text("FACE A  ·  33⅓ RPM",color=Gold.copy(alpha=.8f),fontSize=8.sp,letterSpacing=2.sp,modifier=Modifier.align(Alignment.BottomCenter).padding(bottom=18.dp))
        }
        Spacer(Modifier.height(25.dp))
        Text("EN COURS D'EXPLORATION",color=Gold,fontSize=8.sp,letterSpacing=2.5.sp)
        Spacer(Modifier.height(7.dp))
        Text(album.title,color=Parchment,fontSize=25.sp,lineHeight=30.sp,fontWeight=FontWeight.SemiBold)
        Text(album.artist,color=Gold,fontSize=14.sp)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Button(onClick={ if (playingTrack >= 0) { onPauseSpotify(); setPlayingTrack(-1) } else { album.spotifyUri?.let(onPlaySpotifyUri); setPlayingTrack(0) } },enabled=album.spotifyUri != null || spotifyTracks.isNotEmpty(),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Night),shape=androidx.compose.foundation.shape.CircleShape,modifier=Modifier.height(50.dp)) {
                Icon(if(playingTrack>=0)Icons.Default.Pause else Icons.Default.PlayArrow,null)
                Spacer(Modifier.width(7.dp))
                Text(if(playingTrack>=0)"METTRE EN PAUSE" else "LANCER L'ÉCOUTE",fontWeight=FontWeight.Bold,letterSpacing=1.sp)
            }
            IconButton(onClick={}) { Icon(Icons.Default.FavoriteBorder,null,tint=Gold) }
            IconButton(onClick={}) { Icon(Icons.Default.MoreHoriz,null,tint=Gold) }
        }
        Spacer(Modifier.height(24.dp))
        SectionHeading("LA FACE A", "5 MORCEAUX")
        tracks.forEachIndexed { i, track ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { val uri = spotifyTracks.getOrNull(i)?.uri; if (playingTrack == i) { onPauseSpotify(); setPlayingTrack(-1) } else if (uri != null) { onPlaySpotifyUri(uri); setPlayingTrack(i) } else album.spotifyUri?.let { onPlaySpotifyUri(it); setPlayingTrack(0) } }.background(if(playingTrack==i)Color(0x332C2228) else Color.Transparent).padding(horizontal=10.dp,vertical=15.dp), verticalAlignment=Alignment.CenterVertically) {
                Text(String.format("%02d", i + 1), color = Gold, modifier = Modifier.width(34.dp),fontSize=11.sp)
                Column(Modifier.weight(1f)) {
                    Text(track, color = if (playingTrack == i) Gold else Parchment, fontSize = 13.sp,fontWeight=if(playingTrack==i)FontWeight.SemiBold else FontWeight.Normal)
                    if(playingTrack==i) Text("EN ÉCOUTE",color=Gold.copy(alpha=.75f),fontSize=7.sp,letterSpacing=1.5.sp)
                }
                Icon(if (playingTrack == i) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Gold)
            }
            Divider(color = Gold.copy(alpha = .12f))
        }
        Spacer(Modifier.height(20.dp))
        Text("Un instant à part. Laissez l'album vous raconter son histoire.",color=Ash,fontSize=10.sp)
        Spacer(Modifier.height(35.dp))
    }
}

@Composable
private fun SectionHeading(title: String, detail: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Gold, fontSize = 10.sp, letterSpacing = 2.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(detail, color = Ash.copy(alpha = .7f), fontSize = 8.sp, letterSpacing = 1.5.sp)
    }
}

@Composable
private fun AlbumShelf(items:List<Album>,selected:Album,onSelect:(Album)->Unit) {
    val scroll=rememberScrollState()
    Column {
        Box(Modifier.fillMaxWidth().height(292.dp)) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xB52D1B17),Color(0xD4110D0F),Color(0xF0060506)))))
            Box(Modifier.fillMaxWidth().height(110.dp).background(Brush.horizontalGradient(listOf(Color.Transparent,Color(0x557C4B2D),Color.Transparent))))
            Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top=8.dp).horizontalScroll(scroll),horizontalArrangement=Arrangement.spacedBy(13.dp),verticalAlignment=Alignment.Bottom) {
                Spacer(Modifier.width(5.dp))
                items.forEachIndexed{index,album->
                    val focused=album==selected
                    val scale by animateFloatAsState(if(focused)1.04f else .91f,spring(dampingRatio=.72f,stiffness=260f),label="record-scale")
                    val tilt by animateFloatAsState(if(focused)0f else if(index%2==0)2.8f else -2.8f,spring(dampingRatio=.78f),label="record-tilt")
                    val lift by animateDpAsState(if(focused)0.dp else 14.dp,spring(dampingRatio=.72f),label="record-lift")
                    Column(Modifier.width(174.dp).padding(bottom=lift).graphicsLayer{scaleX=scale;scaleY=scale;rotationZ=tilt;shadowElevation=if(focused)28f else 10f;cameraDistance=18f*density}.clickable{onSelect(album)},horizontalAlignment=Alignment.Start) {
                        Box(Modifier.fillMaxWidth().height(196.dp).background(Color(0xFF1A1110),RoundedCornerShape(4.dp)).border(if(focused)2.dp else 1.dp,if(focused)Gold else Gold.copy(alpha=.28f),RoundedCornerShape(4.dp)).padding(5.dp)) {
                            AlbumArtwork(album,Modifier.fillMaxSize())
                            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0x55FFFFFF),Color.Transparent,Color(0x66000000)))))
                            if(focused)Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(3.dp).background(Brush.horizontalGradient(listOf(Color.Transparent,Gold,Color.Transparent))))
                        }
                        Spacer(Modifier.height(9.dp))
                        Text(album.title,color=if(focused)Parchment else Parchment.copy(alpha=.78f),fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=2,minLines=2,overflow=TextOverflow.Ellipsis)
                        Text(album.artist,color=Ash,fontSize=9.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.width(5.dp))
            }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(25.dp).background(Brush.verticalGradient(listOf(Color(0xFFD0A06A),Color(0xFF85512F),Color(0xFF29170F))))) {
                Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFFFD59A).copy(alpha=.8f)))
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(6.dp).background(Color.Black.copy(alpha=.65f)))
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.Swipe,null,tint=Gold.copy(alpha=.75f),modifier=Modifier.size(14.dp));Spacer(Modifier.width(6.dp))
            Text("GLISSEZ POUR EXPLORER  ·  TOUCHEZ POUR ÉCOUTER",color=Gold.copy(alpha=.78f),fontSize=7.sp,letterSpacing=1.2.sp)
        }
    }
}

@Composable
private fun AlbumArtwork(album: Album, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(6.dp))
            .background(Brush.linearGradient(listOf(album.tint, Color(0xFF29212A), Color(0xFF08080B))))
            .border(1.dp, Gold.copy(alpha = .25f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (album.imageUrl != null) {
            AsyncImage(model = album.imageUrl, contentDescription = "Pochette de ${album.title}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x55000000)))))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(album.symbol, color = Gold.copy(alpha = .9f), fontSize = 32.sp)
                Spacer(Modifier.height(3.dp))
                Box(Modifier.width(28.dp).height(1.dp).background(Gold.copy(alpha = .65f)))
                Text("NOCTURNE", color = Parchment.copy(alpha = .8f), fontSize = 7.sp, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun ArchiveTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFF19161D))
            .border(1.dp, Gold.copy(alpha = .16f), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, color = Parchment, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Ash, fontSize = 9.sp)
        }
    }
}

@Composable
private fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = if (selected) Gold else Ash, modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = if (selected) Gold else Ash, fontSize = 9.sp)
    }
}
