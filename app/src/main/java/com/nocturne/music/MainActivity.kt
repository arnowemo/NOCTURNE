package com.nocturne.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF09090D)
private val Panel = Color(0xFF171720)
private val Muted = Color(0xFF9A98A8)
private val albums = listOf(
    Triple("The Earth Is Not a Cold Dead Place", "Explosions in the Sky", Color(0xFF6E7164)),
    Triple("Ágætis byrjun", "Sigur Rós", Color(0xFF8C7770)),
    Triple("Departure Songs", "We Lost The Sea", Color(0xFF485B68)),
    Triple("All of a Sudden I Miss Everyone", "Explosions in the Sky", Color(0xFF6A5361)),
    Triple("Young Mountain", "This Will Destroy You", Color(0xFF77705B))
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
    val filtered = albums.filter { (it.first + it.second).contains(query, true) }
    MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Panel, primary = Color(0xFFC2A878))) {
        Box(Modifier.fillMaxSize().background(Ink)) {
            Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
                Spacer(Modifier.height(48.dp))
                Text("NOCTURNE", color = Color(0xFFE8DDC9), fontSize = 13.sp, letterSpacing = 5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(28.dp))
                Text("Là où la musique\nprend forme.", color = Color.White, fontSize = 31.sp, lineHeight = 37.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Rechercher un album ou un artiste…", color = Muted) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) },
                    singleLine = true, shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Panel, focusedBorderColor = Color(0xFFC2A878))
                )
                Spacer(Modifier.height(30.dp))
                Text("VOTRE COLLECTION", color = Color(0xFFC2A878), fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    filtered.forEach { album ->
                        Column(Modifier.width(154.dp).clickable { selected = album }) {
                            Box(Modifier.fillMaxWidth().height(154.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(album.third, Color(0xFF17131D), Color.Black))), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.GraphicEq, null, tint = Color.White.copy(alpha = .72f), modifier = Modifier.size(48.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(album.first, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 2)
                            Text(album.second, color = Muted, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(30.dp))
                Text("EN ÉCOUTE", color = Color(0xFFC2A878), fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)).background(selected.third), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, null, tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(selected.first, color = Color.White, fontSize = 12.sp, maxLines = 1)
                        Text(selected.second, color = Muted, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.PlayArrow, null, tint = Color(0xFFC2A878), modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    Icon(Icons.Default.Home, null, tint = Color(0xFFC2A878))
                    Icon(Icons.Default.Explore, null, tint = Muted)
                    Icon(Icons.Default.LibraryMusic, null, tint = Muted)
                    Icon(Icons.Default.Settings, null, tint = Muted)
                }
            }
        }
    }
}
