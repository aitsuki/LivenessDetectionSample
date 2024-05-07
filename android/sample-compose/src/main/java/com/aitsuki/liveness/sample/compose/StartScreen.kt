package com.aitsuki.liveness.sample.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun StartScreen() {
    val navController = LocalNavController.current
    var faceImages: Array<String>? by remember { mutableStateOf(null) }
    var cardImage: String? by remember { mutableStateOf(null) }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LaunchedEffect(Unit) {
            navController.currentBackStackEntry?.savedStateHandle?.let { savedStateHandle ->
                faceImages = savedStateHandle.remove<Array<String>>("liveness_images")
                cardImage = savedStateHandle.remove("card_image")
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            faceImages?.takeIf { it.isNotEmpty() }?.let { faceImages ->
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(faceImages) { faceImage: String ->
                        AsyncImage(
                            model = faceImage,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
            cardImage?.let { cardImage ->
                AsyncImage(
                    model = cardImage,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { navController.navigate("liveness") },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(id = com.aitsuki.liveness.R.string.start_liveness))
            }
            Button(
                onClick = { navController.navigate("camera") },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(id = com.aitsuki.liveness.R.string.start_camera))
            }
        }
    }
}