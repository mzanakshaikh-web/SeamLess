package com.shaikh.seamless

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.ui.platform.LocalContext

data class Song(
    val title: String,
    val artist: String,
    val uri: android.net.Uri
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SeamlessPlayer()
        }
    }
}

@Composable
fun SeamlessPlayer() {

    val context = LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context).build()
    }

    var songs by remember {
        mutableStateOf<List<Song>>(emptyList())
    }

    var currentSong by remember {
        mutableStateOf<Song?>(null)
    }

    var isPlaying by remember {
        mutableStateOf(false)
    }

    fun loadSongs() {
        val musicList = mutableListOf<Song>()

        val collection =
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )

        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->

            val idColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media._ID
            )

            val titleColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.TITLE
            )

            val artistColumn = cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.ARTIST
            )

            while (cursor.moveToNext()) {

                val id = cursor.getLong(idColumn)

                val title = cursor.getString(titleColumn)
                    ?: "Unknown Song"

                val artist = cursor.getString(artistColumn)
                    ?: "Unknown Artist"

                val uri = ContentUris.withAppendedId(
                    collection,
                    id
                )

                musicList.add(
                    Song(title, artist, uri)
                )
            }
        }

        songs = musicList
    }

    fun playSong(song: Song) {

        currentSong = song

        player.setMediaItem(
            MediaItem.fromUri(song.uri)
        )

        player.prepare()
        player.play()

        isPlaying = true
    }

    val permission =
        if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                loadSongs()
            }
        }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    LaunchedEffect(Unit) {

        val granted = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            loadSongs()
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFB8A4FF),
            background = Color(0xFF101014),
            surface = Color(0xFF1B1B22)
        )
    ) {

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF101014)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {

                Text(
                    text = "Seamless",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White
                )

                Text(
                    text = "Your music. Without interruption.",
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "YOUR MUSIC",
                    color = Color(0xFFB8A4FF),
                    style = MaterialTheme.typography.labelLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (songs.isEmpty()) {

                    Button(
                        onClick = {
                            permissionLauncher.launch(permission)
                        }
                    ) {
                        Text("Load music from my phone")
                    }

                    Text(
                        text = "Allow access to your music files " +
                            "to begin listening.",
                        color = Color.LightGray
                    )

                } else {

                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {

                        items(songs) { song ->

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playSong(song)
                                    }
                                    .padding(vertical = 12.dp)
                            ) {

                                Text(
                                    text = song.title,
                                    color = Color.White
                                )

                                Text(
                                    text = song.artist,
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            HorizontalDivider(
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = currentSong?.title ?: "Nothing playing",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = currentSong?.artist ?: "Select a song",
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Button(
                        onClick = {

                            if (player.isPlaying) {
                                player.pause()
                                isPlaying = false
                            } else {
                                player.play()
                                isPlaying = true
                            }
                        },
                        enabled = currentSong != null
                    ) {

                        Text(
                            if (isPlaying) "Pause" else "Play"
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = {

                            if (songs.isNotEmpty()) {

                                val index = songs.indexOf(
                                    currentSong
                                )

                                val nextIndex =
                                    (index + 1) % songs.size

                                playSong(songs[nextIndex])
                            }
                        },
                        enabled = songs.isNotEmpty()
                    ) {

                        Text("Next")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Developed by Shaikh Mzanak",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(
                        Alignment.CenterHorizontally
                    )
                )
            }
        }
    }
}
