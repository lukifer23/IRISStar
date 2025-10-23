package com.nervesparks.iris.ui

import android.app.DownloadManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.R
import com.nervesparks.iris.ui.components.LoadingModal
import com.nervesparks.iris.ui.components.ModelCard
import com.nervesparks.iris.ui.theme.ComponentStyles
import java.io.File
import com.nervesparks.iris.viewmodel.ModelViewModel

@Composable
fun ModelsScreen(
    extFileDir: File?,
    viewModel: MainViewModel,
    modelViewModel: ModelViewModel,
    onSearchResultButtonClick: () -> Unit,
    dm: DownloadManager,
    onQuantizeScreenButtonClicked: () -> Unit
) {
    val installedModels = modelViewModel.availableModels
    val defaultModels = modelViewModel.defaultModels

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        if (viewModel.showAlert) {
            LoadingModal(
                message = "Loading Model",
                onDismiss = { }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ComponentStyles.defaultPadding),
            contentPadding = PaddingValues(bottom = ComponentStyles.largePadding)
        ) {
            // Header Actions
            item {
                Column {
                    // Search Models Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ComponentStyles.smallPadding)
                            .clickable { onSearchResultButtonClick() }
                    ) {
                        Icon(
                            modifier = Modifier.size(ComponentStyles.defaultIconSize),
                            painter = painterResource(id = R.drawable.search_svgrepo_com__3_),
                            contentDescription = "Search Models",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(ComponentStyles.defaultSpacing))
                        Text(
                            text = "Search Hugging-Face Models",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            modifier = Modifier.size(ComponentStyles.defaultIconSize),
                            painter = painterResource(id = R.drawable.right_arrow_svgrepo_com),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    // Quantize Models Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ComponentStyles.smallPadding)
                            .clickable { onQuantizeScreenButtonClicked() }
                    ) {
                        Icon(
                            modifier = Modifier.size(ComponentStyles.defaultIconSize),
                            painter = painterResource(id = R.drawable.ic_quantize),
                            contentDescription = "Quantize Models",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(ComponentStyles.defaultSpacing))
                        Text(
                            text = "Quantize Models",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            modifier = Modifier.size(ComponentStyles.defaultIconSize),
                            painter = painterResource(id = R.drawable.right_arrow_svgrepo_com),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ComponentStyles.defaultPadding),
                        color = MaterialTheme.colorScheme.outline,
                        thickness = ComponentStyles.defaultBorderWidth
                    )
                }
            }

            // Suggested Models Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ComponentStyles.smallPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Suggested Models",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Browse More",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onSearchResultButtonClick() }
                    )
                }
            }

            // Show suggested models from default models
            items(defaultModels.take(6), key = { it["name"] ?: it["destination"] ?: "" }) { model ->
                extFileDir?.let {
                    model["source"]?.let { source ->
                        ModelCard(
                            modelName = model["name"]?.toString() ?: "",
                            supportsReasoning = model["supportsReasoning"] == "true",
                            supportsVision = model["supportsVision"] == "true",
                            viewModel = viewModel,
                            modelViewModel = modelViewModel,
                            dm = dm,
                            extFilesDir = extFileDir,
                            downloadLink = source,
                            showDeleteButton = false // Don't show delete for suggested models
                        )
                    }
                }
            }

            // Divider between sections
            item {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ComponentStyles.largePadding),
                    color = MaterialTheme.colorScheme.outline,
                    thickness = ComponentStyles.defaultBorderWidth
                )
            }

            // My Models Section
            item {
                Text(
                    text = "My Models",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = ComponentStyles.smallPadding)
                )
            }

            // Show installed models
            if (installedModels.isEmpty()) {
                item {
                    Text(
                        text = "No models installed. Download a suggested model above to get started!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = ComponentStyles.defaultPadding)
                    )
                }
            } else {
                items(installedModels, key = { it["name"] ?: it["destination"] ?: "" }) { model ->
                    extFileDir?.let {
                        model["source"]?.let { source ->
                            ModelCard(
                                modelName = model["name"]?.toString() ?: "",
                                supportsReasoning = model["supportsReasoning"] == "true",
                                supportsVision = model["supportsVision"] == "true",
                                viewModel = viewModel,
                                modelViewModel = modelViewModel,
                                dm = dm,
                                extFilesDir = extFileDir,
                                downloadLink = source,
                                showDeleteButton = true
                            )
                        }
                    }
                }
            }
        }
    }
}