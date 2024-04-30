package com.aitsuki.liveness.sample.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun StartScreen() {
    val navController = LocalNavController.current
    var faceImages by rememberSaveable { mutableStateOf(emptyArray<String>()) }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LaunchedEffect(Unit) {
            navController.currentBackStackEntry?.savedStateHandle?.let { savedStateHandle ->
                val images = savedStateHandle.remove<Array<String>>("liveness_images")
                if (!images.isNullOrEmpty()) {
                    faceImages = images
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (faceImages.isNotEmpty()) {
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
        }

        Button(
            onClick = { navController.navigate("liveness") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RectangleShape,
        ) {
            Text(text = stringResource(id = com.aitsuki.liveness.R.string.start_liveness))
        }
    }
}