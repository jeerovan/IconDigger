package com.jeerovan.icondigger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jeerovan.icondigger.ui.theme.IconDiggerTheme

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IconDiggerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    IconPackListScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = mainViewModel,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPackListScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    // specific to lifecycle-runtime-compose, safer than collectAsState
    val iconPacks by viewModel.iconPacks.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // Trigger data load when this composable enters the composition
    LaunchedEffect(Unit) {
        viewModel.installedIconPacks()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text("Installed Icon Packs") })
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { viewModel.saveMyappFilters() },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Device Appfilters")
                }
                Button(
                    onClick = { viewModel.saveDeviceAppIcons() },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Device AppIcons")
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = innerPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = iconPacks,
                    key = { it.packageName } // Key optimization for Lazy lists
                ) { pack ->
                    IconPackItem(
                        iconPack = pack,
                        onSaveAppFilters = { viewModel.saveAppFilters(pack.packageName) },
                        onSaveIcons = {viewModel.savePackageIconBitmaps(pack.packageName)},
                        onSaveDrawables = {viewModel.saveOriginalDrawables(pack.packageName)}
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun IconPackItem(
    iconPack: IconPackInfo,
    onSaveAppFilters: () -> Unit,
    onSaveIcons: () -> Unit,
    onSaveDrawables: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            leadingContent = {
                // Coil is excellent for handling raw Drawables in Compose
                Image(
                    painter = rememberAsyncImagePainter(model = iconPack.icon),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            },
            headlineContent = {
                Text(
                    text = iconPack.name,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            supportingContent = {
                Text(
                    text = iconPack.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        // Buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onSaveAppFilters,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("AppFilters")
            }

            Button(
                onClick = onSaveIcons,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Icons")
            }

            Button(
                onClick = onSaveDrawables,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Drawables")
            }
        }
    }
}
