package com.infomericainc.infotranscribe

import android.Manifest
import android.content.res.Configuration
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infomericainc.infotranscribe.R.drawable
import com.infomericainc.infotranscribe.api.InfoTranscribe
import com.infomericainc.infotranscribe.api.TranscribeStatus
import com.infomericainc.infotranscribe.api.ResponseType
import com.infomericainc.infotranscribe.ui.theme.InfoTranscribeTheme


@Composable
fun LiveTranscribeExample(
    paddingValues: PaddingValues
) {

    val isPreview = LocalInspectionMode.current

    /**
     * Some of the classes are not available
     * in preview, So passing null as instance
     * for the previews.
     */
    val infoTranscribe = remember {
        if (isPreview) {
            null
        } else {
            InfoTranscribe.getTranscribe()
        }
    }

    /**
     * Check weather the transcribe is active or not.
     */
    var isTranscribeActive by remember {
        mutableStateOf(false)
    }

    /**
     * Check weather the live transcribe is going on or not.
     */
    var isTranscribing by remember {
        mutableStateOf(false)
    }

    var result1 by rememberSaveable {
        mutableStateOf("")
    }

    var result2 by rememberSaveable {
        mutableStateOf("")
    }

    val listeners = remember {
        mutableStateListOf<String>()
    }

    var currentError by remember {
        mutableStateOf("")
    }

    val transcribeStatus = infoTranscribe?.transcribeStatus?.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ ->
            // Handle
        }
    )

    LaunchedEffect(key1 = transcribeStatus?.value) {
        when (transcribeStatus?.value) {
            is TranscribeStatus.Idle -> {
                isTranscribeActive = false
                isTranscribing = false
            }

            is TranscribeStatus.Transcribing -> {
                isTranscribeActive = true
                isTranscribing = true
            }

            is TranscribeStatus.Paused -> {
                isTranscribing = false
            }

            is TranscribeStatus.Ended -> {
                isTranscribeActive = false
                isTranscribing = false
            }

            null -> TODO()
        }
    }

    LaunchedEffect(key1 = isTranscribing) {
        if (isTranscribing) {
            infoTranscribe?.apply {
                if (listeners.isEmpty()) {
                    addOnObserveListener(
                        responseType = ResponseType.Partial
                    ) { output ->
                        result1 = output
                    }.also {
                        listeners.add(it)
                    }

                    addOnObserveListener(
                        responseType = ResponseType.Continuous
                    ) { output ->
                        result2 = output
                    }.also {
                        listeners.add(it)
                    }
                }

            }
        } else {
            if (isTranscribeActive) {
                if(isTranscribing) {
                    infoTranscribe?.apply {
                        pauseTranscribe()
                    }
                }
            }
        }
    }

    if (!isPreview) {
        DisposableEffect(Unit) {
            //Launching the permissions if microphone permission is not found
            if (infoTranscribe?.hasMicrophonePermission()?.not() == true) {
                permissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO
                )
            }

            infoTranscribe?.addOnErrorListener {
                currentError = it
            }

            onDispose {
                infoTranscribe?.endTranscribe()
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
        Row(
            modifier = Modifier
                .padding(
                    top = 15.dp
                )
                .padding(
                    horizontal = 15.dp
                )
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(
                    horizontal = 20.dp,
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Designed and Developed by Infomerica, inc",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .alpha(.6f)
            )
            Image(
                painter = painterResource(id = drawable.logo),
                contentDescription = "",
                modifier = Modifier
                    .width(70.dp)
                    .padding(
                        vertical = 10.dp
                    ),
                contentScale = ContentScale.Fit
            )
        }
        AnimatedVisibility(visible = currentError.isNotEmpty()) {
            Text(
                text = currentError,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 15.dp
                    )
                    .padding(
                        horizontal = 10.dp
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
                    top = 15.dp
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
                    infoTranscribe?.startTranscribe()
                },
                modifier = Modifier
                    .weight(.5f),
                enabled = isTranscribing.not()
            ) {
                Text(text = "Start Transcribe")
            }

            Button(
                onClick = {
                    infoTranscribe?.pauseTranscribe()
                },
                modifier = Modifier
                    .weight(.5f),
                enabled = isTranscribing
            ) {
                Text(text = "Pause Transcribe")
            }

            Button(
                onClick = {
                    infoTranscribe?.removeActiveListeners()
                    infoTranscribe?.endTranscribe()
                    listeners.clear()
                },
                modifier = Modifier
                    .weight(.5f),
                enabled = isTranscribing.not() && isTranscribeActive
            ) {
                Text(text = "End Transcribe")
            }

        }

        Text(
            text = "Active Listeners",
            modifier = Modifier
                .padding(
                    top = 10.dp
                )
                .padding(
                    horizontal = 20.dp
                ),
            fontWeight = FontWeight.Bold
        )
        LazyRow(
            modifier = Modifier
                .padding(
                    top = 10.dp
                )
        ) {
            itemsIndexed(listeners) { index, id ->
                if (index == 0) {
                    Spacer(modifier = Modifier.width(20.dp))
                }
                Text(
                    text = id,
                    modifier = Modifier
                        .padding(
                            end = 10.dp
                        )
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        )
                        .clickable {
                            infoTranscribe?.removeActiveObservationListener(
                                id
                            )
                            listeners.remove(id)
                        },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(
                    top = 15.dp
                )
                .padding(
                    horizontal = 15.dp
                )
        ) {
            Column(
                modifier = Modifier
                    .weight(.5f)
            ) {
                Text(
                    text = "Partial Transcribe",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 10.dp
                        ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = result1,
                    modifier = Modifier
                        .padding(
                            top = 10.dp,
                            bottom = 10.dp
                        )
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(
                            all = 15.dp
                        ),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

            }

            Column(
                modifier = Modifier
                    .weight(.5f)
            ) {
                Text(
                    text = "Continuous Transcribe",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 10.dp
                        ),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = result2,
                    modifier = Modifier
                        .padding(
                            top = 10.dp,
                            bottom = 10.dp
                        )
                        .padding(
                            start = 10.dp,
                        )
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(
                            all = 15.dp
                        )
                        .verticalScroll(
                            state = rememberScrollState()
                        ),
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview(
    widthDp = 1080,
    heightDp = 720,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun LiveTranscribeExamplePreview() {
    InfoTranscribeTheme {
        Surface {
            LiveTranscribeExample(
                paddingValues = PaddingValues()
            )
        }
    }
}