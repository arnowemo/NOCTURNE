package com.nocturne.music

import android.os.Bundle
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
            AsyncImage(model="https://images.unsplash.com/photo-1461360228754-6e81c478b882?auto=format&fit=crop&w=1400&q=85",contentDescription=null,contentScale=ContentScale.Crop,modifier=Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xB9080709),Color(0xD9080709),Color(0xF9080709),Night))))
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0x557D4D2C),Color.Transparent),radius=1050f)))
            AnimatedVisibility(visible=openedAlbum==null,enter=fadeIn(tween(360)),exit=fadeOut(tween(220))) {
                Column(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=18.dp)) {
                        Spacer(Modifier.height(30.dp))
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("N O C T U R N E",color=Parchment,fontSize=12.sp,fontWeight=FontWeight.Bold,letterSpacing=4.sp)
                                Text("LA MAISON DES DISQUES",color=Gold,fontSize=8.sp,letterSpacing=2.5.sp)
                            }
                            Icon(Icons.Default.DarkMode,null,tint=Gold,modifier=Modifier.size(23.dp))
                        }
                        Spacer(Modifier.height(28.dp))
                        Text("Bienvenue dans",color=Parchment.copy(alpha=.76f),fontSize=14.sp,letterSpacing=1.sp)
                        Text("votre archive sonore.",color=Parchment,fontSize=29.sp,lineHeight=34.sp,fontWeight=FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Prenez le temps. Choisissez un disque.",color=Gold.copy(alpha=.9f),fontSize=11.sp)
                        Spacer(Modifier.height(20.dp))
                        OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),placeholder={Text("Rechercher dans les rayonnages…",color=Ash)},leadingIcon={Icon(Icons.Default.Search,null,tint=Gold)},singleLine=true,shape=RoundedCornerShape(18.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=Gold.copy(alpha=.35f),focusedBorderColor=Gold,unfocusedContainerColor=Color(0xA9100D0E),focusedContainerColor=Color(0xD9100D0E),cursorColor=Gold))
                        Spacer(Modifier.height(27.dp))
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("VOTRE COLLECTION",color=Gold,fontSize=10.sp,letterSpacing=3.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                            Text(filtered.size.toString().padStart(2, '0') + " DISQUES",color=Ash,fontSize=8.sp,letterSpacing=1.5.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Vos albums, comme dans un vrai disquaire. Faites défiler et choisissez.",color=Parchment.copy(alpha=.7f),fontSize=10.sp)
                        Spacer(Modifier.height(15.dp))
                        AlbumShelf(filtered,selected){album->selected=album;openedAlbum=album}
                        Spacer(Modifier.height(27.dp))
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("CABINES D'ÉCOUTE",color=Gold,fontSize=10.sp,letterSpacing=3.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                            Text("À DÉCOUVRIR",color=Ash,fontSize=8.sp,letterSpacing=1.5.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                            ArchiveTile("Post-rock","Paysages sonores",Icons.Default.Landscape,Modifier.weight(1f))
                            ArchiveTile("Atmosphères","Lents & immersifs",Icons.Default.Waves,Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("REPRENDRE LE FIL",color=Gold,fontSize=10.sp,letterSpacing=3.sp,fontWeight=FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xD9141012)).border(1.dp,Gold.copy(alpha=.25f),RoundedCornerShape(16.dp)).clickable{openedAlbum=selected}.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                            AlbumArtwork(selected,Modifier.size(56.dp));Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(selected.title,color=Parchment,fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                                Text(selected.artist,color=Ash,fontSize=10.sp)
                            }
                            Icon(Icons.Default.GraphicEq,null,tint=Gold,modifier=Modifier.size(25.dp))
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                    Row(Modifier.fillMaxWidth().background(Color(0xF20C0A0C)).border(1.dp,Gold.copy(alpha=.15f)).padding(vertical=10.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
                        NavItem(Icons.Default.Home,"Maison",activeTab==0){activeTab=0}
                        NavItem(Icons.Default.Explore,"Explorer",activeTab==1){activeTab=1}
                        NavItem(Icons.Default.LibraryMusic,"Archives",activeTab==2){activeTab=2}
                        NavItem(Icons.Default.Settings,"Réglages",activeTab==3){activeTab=3}
                    }
                }
            }
            AnimatedVisibility(visible=openedAlbum!=null,enter=fadeIn(tween(260))+slideInVertically(initialOffsetY={it/5},animationSpec=tween(420,easing=FastOutSlowInEasing)),exit=fadeOut(tween(180))+slideOutVertically(targetOffsetY={it/8},animationSpec=tween(220))) {
                openedAlbum?.let{album->AlbumRoomScreen(album,{openedAlbum=null},playingTrack){playingTrack=it}}
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
