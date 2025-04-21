package com.infomericainc.infotranscribe

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomericainc.infotranscribe.api.LiveTranscribe
import com.infomericainc.infotranscribe.ui.theme.InfoTranscribeTheme
import com.infomericainc.infotranscribe.util.Constants.AZURE_API_KEY
import com.infomericainc.infotranscribe.util.Constants.AZURE_REGION

@Composable
fun LiveTranscribeExample(
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val liveTranscribe = remember { LiveTranscribe.getTranscribe(context = context) }
    val isPreview = LocalInspectionMode.current
    var isTranscribeStarted by remember {
        mutableStateOf(false)
    }

    var result by remember {
        mutableStateOf("")
    }


    var currentError by remember {
        mutableStateOf("")
    }

    var hasPermissions by remember {
        mutableStateOf(false)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermissions = isGranted
        }
    )

    LaunchedEffect(key1 = hasPermissions) {
        if (hasPermissions) {
            liveTranscribe.apply {
                initialize(
                    context = context,
                    apiKey = AZURE_API_KEY,
                    region = AZURE_REGION
                )

                addOnErrorListener {
                    currentError = it
                }

                observe(
                    appendResponses = true
                ) { output ->
                    result += output
                }

            }
        } else {
            //Show Rationale
        }
    }

    if (!isPreview) {
        DisposableEffect(Unit) {
            //Launching the permissions if microphone permission is not found
            if (liveTranscribe.hasMicrophonePermission().not()) {
                permissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO
                )
            }

            onDispose {
                liveTranscribe.endTranscribe()
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.Top
    ) {
        AnimatedVisibility(visible = currentError.isNotEmpty()) {
            Text(
                text = currentError,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 10.dp
                    )
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(
                        all = 15.dp
                    ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp
            )
        }

        Row(
            modifier = Modifier
                .padding(
                    top = 20.dp
                )
                .padding(
                    horizontal = 15.dp
                )
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Button(
                onClick = {
                    if (isTranscribeStarted) {
                        liveTranscribe.pauseTranscribe()
                        isTranscribeStarted = false
                    } else {
                        liveTranscribe.startTranscribe()
                        isTranscribeStarted = true
                    }
                },
                modifier = Modifier
                    .weight(.5f)
            ) {
                Text(text = if (isTranscribeStarted) "Pause Transcribe" else "Start Transcribe")
            }

            Button(
                onClick = {
                    liveTranscribe.endTranscribe()
                    isTranscribeStarted = false
                },
                modifier = Modifier
                    .weight(.5f),
                enabled = isTranscribeStarted
            ) {
                Text(text = "End Transcribe")
            }

        }

        Text(
            text = result.plus("sdsd"),
            modifier = Modifier
                .padding(
                    top = 20.dp
                )
                .padding(
                    horizontal = 15.dp
                )
                .fillMaxWidth(),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview
@Composable
private fun LiveTranscribeExamplePreview() {
    InfoTranscribeTheme {
        LiveTranscribeExample(
            paddingValues = PaddingValues()
        )
    }
}