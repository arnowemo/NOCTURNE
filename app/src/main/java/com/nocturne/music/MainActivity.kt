package com.nocturne.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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

private data class Album(val title: String, val artist: String, val tint: Color, val symbol: String)
private val albums = listOf(
    Album("The Earth Is Not a Cold Dead Place", "Explosions in the Sky", Color(0xFF777968), "✦"),
    Album("Ágætis byrjun", "Sigur Rós", Color(0xFF987C75), "☾"),
    Album("Departure Songs", "We Lost The Sea", Color(0xFF526875), "✧"),
    Album("All of a Sudden I Miss Everyone", "Explosions in the Sky", Color(0xFF705665), "✦"),
    Album("Young Mountain", "This Will Destroy You", Color(0xFF82785F), "⌁")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NocturneApp() }
    }
}

@Composable
fun NocturneApp() {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(albums.first()) }
    var activeTab by remember { mutableStateOf(0) }
    var openedAlbum by remember { mutableStateOf<Album?>(null) }
    var playingTrack by remember { mutableStateOf(-1) }
    val filtered = albums.filter { (it.title + it.artist).contains(query, true) }

    MaterialTheme(colorScheme = darkColorScheme(background = Night, surface = Stone, primary = Gold)) {
        Box(Modifier.fillMaxSize().background(Night)) {
            if (openedAlbum != null) {
                AlbumRoomScreen(openedAlbum!!, { openedAlbum = null }, playingTrack) { playingTrack = it }
            } else {
            // Subtle cathedral-like ambient glow behind the library.
            Box(
                Modifier.fillMaxWidth().height(390.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF29212A), Color(0xFF111015), Night)))
            )
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Spacer(Modifier.height(38.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("N O C T U R N E", color = Parchment, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                            Text("THE PRIVATE MUSIC ARCHIVE", color = Gold.copy(alpha = .8f), fontSize = 8.sp, letterSpacing = 2.sp)
                        }
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = Gold, modifier = Modifier.size(22.dp))
                    }

                    Spacer(Modifier.height(30.dp))
                    // Photographic record-store entrance with cinematic overlays.
                    val glow = rememberInfiniteTransition(label = "ambient-glow")
                    val glowAlpha by glow.animateFloat(
                        initialValue = 0.10f,
                        targetValue = 0.24f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(3200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glow-alpha"
                    )
                    Box(
                        Modifier.fillMaxWidth().height(238.dp)
                            .clip(RoundedCornerShape(topStart = 92.dp, topEnd = 92.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                            .border(1.dp, Gold.copy(alpha = .42f), RoundedCornerShape(topStart = 92.dp, topEnd = 92.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                    ) {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1758405392912-eb488da67a82?auto=format&fit=crop&w=1400&q=85",
                            contentDescription = "Intérieur d'un magasin de vinyles",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    listOf(Color(0x33100A08), Color(0xAA120D12), Color(0xF208080B))
                                )
                            )
                        )
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.radialGradient(
                                    colors = listOf(Gold.copy(alpha = glowAlpha), Color.Transparent),
                                    radius = 620f
                                )
                            )
                        )
                        Column(
                            Modifier.align(Alignment.BottomStart).padding(start = 22.dp, end = 22.dp, bottom = 22.dp)
                        ) {
                            Text("✦  ARCHIVES SONORES  ✦", color = Gold, fontSize = 9.sp, letterSpacing = 2.5.sp)
                            Spacer(Modifier.height(7.dp))
                            Text("Entrez dans votre\nunivers musical.", color = Parchment, fontSize = 25.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(5.dp))
                            Text("Une collection à explorer, album après album.", color = Parchment.copy(alpha = .82f), fontSize = 11.sp)
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Rechercher un album ou un artiste…", color = Ash) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Gold) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Gold.copy(alpha = .22f),
                            focusedBorderColor = Gold,
                            unfocusedContainerColor = Color(0xFF100F14),
                            focusedContainerColor = Color(0xFF100F14),
                            cursorColor = Gold
                        )
                    )

                    Spacer(Modifier.height(28.dp))
                    SectionHeading("VOTRE COLLECTION", "05 ALBUMS")
                    Spacer(Modifier.height(14.dp))
                    AlbumShelf(filtered, selected) { selected = it; openedAlbum = it }

                    Spacer(Modifier.height(28.dp))
                    SectionHeading("LES ARCHIVES", "SÉLECTION")
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ArchiveTile("Post-rock", "Paysages sonores", Icons.Default.Landscape, Modifier.weight(1f))
                        ArchiveTile("Atmosphères", "Lents & immersifs", Icons.Default.Waves, Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(26.dp))
                    SectionHeading("EN ÉCOUTE", "LECTEUR")
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF29212A), Color(0xFF17151B))))
                            .border(1.dp, Gold.copy(alpha = .18f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArtwork(selected, Modifier.size(58.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(selected.title, color = Parchment, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(selected.artist, color = Ash, fontSize = 11.sp, maxLines = 1)
                            Text("APERÇU DE LECTEUR", color = Gold, fontSize = 8.sp, letterSpacing = 1.5.sp)
                        }
                        Icon(Icons.Default.PlayArrow, contentDescription = "Lecture (visuel)", tint = Gold, modifier = Modifier.size(34.dp))
                    }
                    Spacer(Modifier.height(22.dp))
                }

                Row(
                    Modifier.fillMaxWidth().background(Color(0xFF100F14)).border(width = 1.dp, color = Gold.copy(alpha = .12f), shape = RoundedCornerShape(0.dp)).padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItem(Icons.Default.Home, "Accueil", activeTab == 0) { activeTab = 0 }
                    NavItem(Icons.Default.Explore, "Découvrir", activeTab == 1) { activeTab = 1 }
                    NavItem(Icons.Default.LibraryMusic, "Archives", activeTab == 2) { activeTab = 2 }
                    NavItem(Icons.Default.Settings, "Réglages", activeTab == 3) { activeTab = 3 }
                }
            }
            }
        }
    }
}

@Composable
private fun AlbumRoomScreen(album: Album, onBack: () -> Unit, playingTrack: Int, onPlayTrack: (Int) -> Unit) {
    val tracks = listOf("First Light", "The Only Moment We Were Alone", "Six Days at the Bottom of the Ocean", "Memorial", "Your Hand in Mine")
    Column(Modifier.fillMaxSize().background(Night).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(30.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Retour", tint = Gold) }
            Text("NOCTURNE  /  SALLE D'ÉCOUTE", color = Gold, fontSize = 10.sp, letterSpacing = 2.sp)
        }
        Box(Modifier.fillMaxWidth().height(310.dp).clip(RoundedCornerShape(topStart = 140.dp, topEnd = 140.dp, bottomStart = 12.dp, bottomEnd = 12.dp)).background(Brush.verticalGradient(listOf(Color(0xFF49343A), Color(0xFF211923), Night))).border(1.dp, Gold.copy(alpha = .3f), RoundedCornerShape(topStart = 140.dp, topEnd = 140.dp, bottomStart = 12.dp, bottomEnd = 12.dp)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✧", color = Gold, fontSize = 24.sp)
                AlbumArtwork(album, Modifier.size(210.dp))
                Text("LA PIÈCE CENTRALE", color = Gold, fontSize = 9.sp, letterSpacing = 2.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(album.title, color = Parchment, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
        Text(album.artist, color = Gold, fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))
        Button(onClick = { onPlayTrack(if (playingTrack < 0) 0 else -1) }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Night)) {
            Icon(if (playingTrack >= 0) Icons.Default.Pause else Icons.Default.PlayArrow, null)
            Text(if (playingTrack >= 0) "PAUSE" else "ÉCOUTER")
        }
        Spacer(Modifier.height(22.dp))
        SectionHeading("LES MORCEAUX", "APERÇU")
        tracks.forEachIndexed { i, track ->
            Row(Modifier.fillMaxWidth().clickable { onPlayTrack(if (playingTrack == i) -1 else i) }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(String.format("%02d", i + 1), color = Gold, modifier = Modifier.width(34.dp))
                Text(track, color = if (playingTrack == i) Gold else Parchment, modifier = Modifier.weight(1f), fontSize = 13.sp)
                Icon(if (playingTrack == i) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Gold)
            }
            Divider(color = Gold.copy(alpha = .12f))
        }
        Spacer(Modifier.height(24.dp))
        Text("Aperçu visuel uniquement : la lecture Spotify sera ajoutée ensuite.", color = Ash, fontSize = 10.sp)
        Spacer(Modifier.height(30.dp))
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
private fun AlbumShelf(items: List<Album>, selected: Album, onSelect: (Album) -> Unit) {
    Column {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items.forEach { album ->
                Column(Modifier.width(142.dp).clickable { onSelect(album) }) {
                    AlbumArtwork(album, Modifier.fillMaxWidth().height(142.dp))
                    Spacer(Modifier.height(9.dp))
                    Text(album.title, color = if (album == selected) Parchment else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
                    Text(album.artist, color = Ash, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        // A wooden shelf lip under the album row.
        Box(
            Modifier.fillMaxWidth().height(12.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF76553A), Color(0xFF302019))))
                .border(1.dp, Color(0xFF9A7650).copy(alpha = .35f), RoundedCornerShape(2.dp))
        )
        Box(Modifier.fillMaxWidth().height(4.dp).background(Color(0xFF050405)))
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
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(album.symbol, color = Gold.copy(alpha = .9f), fontSize = 32.sp)
            Spacer(Modifier.height(3.dp))
            Box(Modifier.width(28.dp).height(1.dp).background(Gold.copy(alpha = .65f)))
            Text("NOCTURNE", color = Parchment.copy(alpha = .8f), fontSize = 7.sp, letterSpacing = 2.sp)
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
