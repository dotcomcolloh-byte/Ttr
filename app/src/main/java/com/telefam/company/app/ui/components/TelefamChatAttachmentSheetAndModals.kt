package com.telefam.company.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.telefam.company.app.domain.ChatContactPayload
import com.telefam.company.app.domain.ChatDocumentPayload
import com.telefam.company.app.domain.ChatEventPayload
import com.telefam.company.app.domain.ChatMediaAttachmentItem
import com.telefam.company.app.domain.ChatPollOptionItem
import com.telefam.company.app.domain.ChatPollPayload
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ChatAttachmentMenuOption {
    GALLERY,
    CAMERA,
    LOCATION,
    CONTACT,
    DOCUMENT,
    POLL,
    EVENT
}

private data class AttachmentGridActionSpec(
    val option: ChatAttachmentMenuOption,
    val label: String,
    val iconKind: LucideIconKind,
    val circleColor: Color,
    val testTag: String
)

/**
 * Floating 4x2 attachment menu matching the reference design:
 * Row 1: Gallery, Camera, Location, Contact
 * Row 2: Document, Poll, Event
 */
@Composable
fun TelefamAttachmentMenuSheet(
    onDismiss: () -> Unit,
    onSelectOption: (ChatAttachmentMenuOption) -> Unit
) {
    val row1 = remember {
        listOf(
            AttachmentGridActionSpec(
                option = ChatAttachmentMenuOption.GALLERY,
                label = "Gallery",
                iconKind = LucideIconKind.MEDIA_GALLERY,
                circleColor = Color(0xFF2563EB),
                testTag = "attach_option_gallery"
            ),
            AttachmentGridActionSpec(
                option = ChatAttachmentMenuOption.CAMERA,
                label = "Camera",
                iconKind = LucideIconKind.CAMERA,
                circleColor = Color(0xFFE11D48),
                testTag = "attach_option_camera"
            ),
            AttachmentGridActionSpec(
                option = ChatAttachmentMenuOption.LOCATION,
                label = "Location",
                iconKind = LucideIconKind.MAP_PIN,
                circleColor = Color(0xFF16A34A),
                testTag = "attach_option_location"
            ),
            AttachmentGridActionSpec(
                option = ChatAttachmentMenuOption.CONTACT,
                label = "Contact",
                iconKind = LucideIconKind.USER_CONTACT,
                circleColor = Color(0xFF0284C7),
                testTag = "attach_option_contact"
            )
        )
    }
    val row2 = remember {
        listOf(
            AttachmentGridActionSpec(
                option = ChatAttachmentMenuOption.DOCUMENT,
                label = "Document",
                iconKind = LucideIconKind.FILE_DOC,
                circleColor = Color(0xFF7C3AED),
                testTag = "attach_option_document"
            ),
            AttachmentGridActionSpec(
                option = ChatAttachmentMenuOption.POLL,
                label = "Poll",
                iconKind = LucideIconKind.BAR_CHART_POLL,
                circleColor = Color(0xFFF59E0B),
                testTag = "attach_option_poll"
            ),
            AttachmentGridActionSpec(
                option = ChatAttachmentMenuOption.EVENT,
                label = "Event",
                iconKind = LucideIconKind.CALENDAR_EVENT,
                circleColor = Color(0xFFF43F5E),
                testTag = "attach_option_event"
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onDismiss)
                .padding(horizontal = 16.dp, vertical = 76.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .clickable(enabled = false) {}
                    .testTag("chat_attachment_menu_card"),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 8.dp,
                shadowElevation = 14.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row1.forEach { spec ->
                            AttachmentMenuTile(
                                spec = spec,
                                onClick = { onSelectOption(spec.option) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row2.forEach { spec ->
                            AttachmentMenuTile(
                                spec = spec,
                                onClick = { onSelectOption(spec.option) }
                            )
                        }
                        // Invisible 4th column spacer so the 3 items align cleanly under the 4 columns above
                        Spacer(modifier = Modifier.width(68.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentMenuTile(
    spec: AttachmentGridActionSpec,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .testTag(spec.testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = spec.circleColor.copy(alpha = 0.14f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                spec.circleColor.copy(alpha = 0.32f)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                TelefamLucideIcon(
                    kind = spec.iconKind,
                    tint = spec.circleColor,
                    size = 25.dp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = spec.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Camera mode selector dialog: lets the user capture a photo or record a video with the device camera.
 */
@Composable
fun TelefamCameraModeChooserDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onRecordVideo: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = Color(0xFFE11D48).copy(alpha = 0.14f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.CAMERA,
                                tint = Color(0xFFE11D48),
                                size = 22.dp
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Camera Capture",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Take a high-resolution photo or record a video clip",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onTakePhoto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("camera_take_photo_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    TelefamLucideIcon(
                        kind = LucideIconKind.CAMERA,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        size = 18.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Take Photo", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onRecordVideo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("camera_record_video_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    TelefamLucideIcon(
                        kind = LucideIconKind.CLIP_VIDEO,
                        tint = MaterialTheme.colorScheme.primary,
                        size = 18.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Record Video", fontWeight = FontWeight.SemiBold)
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * Pre-send Media Composer Dialog for Gallery & Camera selections:
 * - Previews selected image(s) and video(s) with video thumbnail + duration badge
 * - Allows adding more media via "+" button
 * - Shows View-Once (`1`) toggle when a single image or video is selected
 * - Includes a caption text field so tapping Send dispatches both the media and caption together
 */
@Composable
fun TelefamMediaComposerDialog(
    initialItems: List<ChatMediaAttachmentItem>,
    initialCaption: String = "",
    onDismiss: () -> Unit,
    onSendMedia: (items: List<ChatMediaAttachmentItem>, caption: String, isViewOnce: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaItems = remember(initialItems) {
        mutableStateListOf<ChatMediaAttachmentItem>().apply { addAll(initialItems) }
    }
    var selectedPreviewIndex by remember { mutableIntStateOf(0) }
    var captionText by remember { mutableStateOf(initialCaption) }
    var isViewOnce by remember { mutableStateOf(false) }
    var isImportingMore by remember { mutableStateOf(false) }

    val addMoreMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isImportingMore = true
            scope.launch {
                val imported = TelefamAttachmentDeviceHelper.importPickedMediaUris(context, uris)
                if (imported.isNotEmpty()) {
                    mediaItems.addAll(imported)
                    if (mediaItems.size > 1) isViewOnce = false
                }
                isImportingMore = false
            }
        }
    }

    if (mediaItems.isEmpty()) {
        onDismiss()
        return
    }

    val safeIndex = selectedPreviewIndex.coerceIn(0, (mediaItems.size - 1).coerceAtLeast(0))
    val activeItem = mediaItems[safeIndex]
    val canToggleViewOnce = mediaItems.size == 1

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0B0F17)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("media_composer_close_button")
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.CLOSE_X,
                            tint = Color.White,
                            size = 22.dp
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (mediaItems.size == 1) {
                                if (activeItem.isVideo) "Send Video" else "Send Photo"
                            } else {
                                "Send Album (${mediaItems.size} items)"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${activeItem.fileName} · ${TelefamAttachmentDeviceHelper.formatBytesReadable(activeItem.fileSizeBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (mediaItems.size > 1) {
                        IconButton(
                            onClick = {
                                mediaItems.removeAt(safeIndex)
                                selectedPreviewIndex = (safeIndex - 1).coerceAtLeast(0)
                            },
                            modifier = Modifier.testTag("media_composer_remove_current")
                        ) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.TRASH,
                                tint = Color(0xFFF87171),
                                size = 20.dp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Main large preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF141B26)),
                    contentAlignment = Alignment.Center
                ) {
                    val previewModel = activeItem.thumbnailPath ?: activeItem.localPath
                    AsyncImage(
                        model = File(previewModel),
                        contentDescription = activeItem.fileName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    if (activeItem.isVideo) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.PLAY,
                                    tint = Color.White,
                                    size = 28.dp
                                )
                            }
                        }

                        if (activeItem.durationMs > 0L) {
                            val totalSec = (activeItem.durationMs / 1000L).coerceAtLeast(1L)
                            val durLabel = String.format(Locale.US, "%d:%02d", totalSec / 60L, totalSec % 60L)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.72f),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.CLIP_VIDEO,
                                        tint = Color.White,
                                        size = 14.dp
                                    )
                                    Text(
                                        text = durLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    if (isViewOnce && canToggleViewOnce) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.92f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.VIEW_ONCE_ONE,
                                    tint = Color.White,
                                    size = 16.dp
                                )
                                Text(
                                    text = if (activeItem.isVideo) "Video set to View Once (No download)" else "Photo set to View Once (No download)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Thumbnail strip + "+" Add More button
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(mediaItems) { idx, item ->
                        val isSelected = idx == safeIndex
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.24f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedPreviewIndex = idx }
                        ) {
                            AsyncImage(
                                model = File(item.thumbnailPath ?: item.localPath),
                                contentDescription = item.fileName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (item.isVideo) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.PLAY,
                                        tint = Color.White,
                                        size = 16.dp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isImportingMore) {
                                    addMoreMediaLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                }
                                .testTag("media_composer_add_more_button"),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isImportingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.PLUS,
                                        tint = Color.White,
                                        size = 22.dp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Caption input + View Once toggle + Send button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = captionText,
                        onValueChange = { captionText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("media_composer_caption_input"),
                        placeholder = {
                            Text(
                                text = "Add a caption...",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        },
                        maxLines = 3,
                        shape = RoundedCornerShape(22.dp),
                        trailingIcon = if (canToggleViewOnce) {
                            {
                                IconButton(
                                    onClick = { isViewOnce = !isViewOnce },
                                    modifier = Modifier.testTag("media_composer_view_once_toggle")
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isViewOnce) Color(0xFF10B981) else Color.White.copy(alpha = 0.16f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            TelefamLucideIcon(
                                                kind = LucideIconKind.VIEW_ONCE_ONE,
                                                tint = Color.White,
                                                size = 18.dp
                                            )
                                        }
                                    }
                                }
                            }
                        } else null
                    )

                    Surface(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .clickable {
                                onSendMedia(
                                    mediaItems.toList(),
                                    captionText.trim(),
                                    isViewOnce && canToggleViewOnce
                                )
                            }
                            .testTag("media_composer_send_button"),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.SEND,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                size = 22.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Location Composer Dialog:
 * - Reads real GPS coordinates from the device (with runtime permission support)
 * - Displays an interactive vector map preview card
 * - Supports Live Location sharing with server-issued session tokens (15m, 1h, 8h)
 * - Supports View-Once Location toggle (`1`) that expires immediately after being opened once
 */
@Composable
fun TelefamLocationComposerDialog(
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onDismiss: () -> Unit,
    onSendLocation: (
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        placeTitle: String,
        addressLine: String,
        isLiveLocation: Boolean,
        liveDurationMinutes: Int,
        isViewOnce: Boolean,
        caption: String
    ) -> Unit
) {
    val context = LocalContext.current
    var isLoadingGps by remember { mutableStateOf(false) }
    var latitudeText by remember { mutableStateOf("37.77493") }
    var longitudeText by remember { mutableStateOf("-122.41942") }
    var accuracyMeters by remember { mutableStateOf(10.0f) }
    var placeTitle by remember { mutableStateOf("Current GPS Location") }
    var addressLine by remember { mutableStateOf("Encrypted E2EE Coordinate Fix") }
    var isLiveLocation by remember { mutableStateOf(false) }
    var liveDurationMinutes by remember { mutableIntStateOf(60) }
    var isViewOnce by remember { mutableStateOf(false) }
    var captionText by remember { mutableStateOf("") }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            isLoadingGps = true
            val fix = TelefamAttachmentDeviceHelper.fetchCurrentDeviceLocation(context)
            if (fix != null) {
                latitudeText = String.format(Locale.US, "%.6f", fix.latitude)
                longitudeText = String.format(Locale.US, "%.6f", fix.longitude)
                accuracyMeters = fix.accuracyMeters
                placeTitle = fix.placeTitle
                addressLine = fix.addressLine
            }
            isLoadingGps = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = Color(0xFF16A34A).copy(alpha = 0.14f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.MAP_PIN,
                                tint = Color(0xFF16A34A),
                                size = 22.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Share Location",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Opens exclusively in Telefam Secure In-App Map",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.CLOSE_X,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 20.dp
                        )
                    }
                }

                // Beautiful Vector Map Preview Banner
                TelefamInteractiveMapPreviewCard(
                    placeTitle = placeTitle,
                    addressLine = addressLine,
                    latitude = latitudeText.toDoubleOrNull() ?: 37.7749,
                    longitude = longitudeText.toDoubleOrNull() ?: -122.4194,
                    isLiveLocation = isLiveLocation,
                    isViewOnce = isViewOnce
                )

                if (!hasLocationPermission) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.MAP_PIN,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                size = 18.dp
                            )
                            Text(
                                text = "Grant GPS permission to auto-fill your exact device coordinates.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = onRequestLocationPermission,
                                modifier = Modifier.testTag("location_grant_permission_button")
                            ) {
                                Text("Grant GPS", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (isLoadingGps) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Acquiring high-accuracy GPS telemetry...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = placeTitle,
                    onValueChange = { placeTitle = it },
                    label = { Text("Location Name / Landmark") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_place_title_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = addressLine,
                    onValueChange = { addressLine = it },
                    label = { Text("Street Address / Description") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_address_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = latitudeText,
                        onValueChange = { latitudeText = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = longitudeText,
                        onValueChange = { longitudeText = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Live Location Toggle
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.NAVIGATION_LIVE,
                                tint = Color(0xFF16A34A),
                                size = 20.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Share Live Location",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Generates an E2EE server token with real-time expiry",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isLiveLocation,
                                onCheckedChange = {
                                    isLiveLocation = it
                                    if (it) isViewOnce = false
                                },
                                modifier = Modifier.testTag("location_live_toggle")
                            )
                        }

                        AnimatedVisibility(visible = isLiveLocation) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(15 to "15 min", 60 to "1 hour", 480 to "8 hours").forEach { (mins, label) ->
                                    FilterChip(
                                        selected = liveDurationMinutes == mins,
                                        onClick = { liveDurationMinutes = mins },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }

                // View-Once Location Toggle
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.VIEW_ONCE_ONE,
                            tint = Color(0xFF10B981),
                            size = 20.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "View-Once Location",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Expires immediately after recipient opens the map once",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isViewOnce,
                            onCheckedChange = {
                                isViewOnce = it
                                if (it) isLiveLocation = false
                            },
                            modifier = Modifier.testTag("location_view_once_toggle")
                        )
                    }
                }

                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    label = { Text("Optional Note / Directions") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_caption_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val lat = latitudeText.toDoubleOrNull() ?: 37.7749
                            val lon = longitudeText.toDoubleOrNull() ?: -122.4194
                            onSendLocation(
                                lat,
                                lon,
                                accuracyMeters,
                                placeTitle.ifBlank { "Shared Location" },
                                addressLine,
                                isLiveLocation,
                                liveDurationMinutes,
                                isViewOnce,
                                captionText.trim()
                            )
                        },
                        modifier = Modifier.testTag("location_send_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.SEND,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            size = 16.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Location", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Reusable stylized vector map preview card used both in the Location Composer and inside Chat Bubbles.
 */
@Composable
fun TelefamInteractiveMapPreviewCard(
    placeTitle: String,
    addressLine: String,
    latitude: Double,
    longitude: Double,
    isLiveLocation: Boolean,
    isViewOnce: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F2922),
                        Color(0xFF133A2E),
                        Color(0xFF0D2137)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 28.dp.toPx()
            val gridColor = Color(0xFF34D399).copy(alpha = 0.14f)
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx()
                )
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += step
            }
            // Stylized road vectors
            val roadColor = Color(0xFF38BDF8).copy(alpha = 0.22f)
            drawLine(
                color = roadColor,
                start = Offset(0f, size.height * 0.68f),
                end = Offset(size.width, size.height * 0.28f),
                strokeWidth = 5.dp.toPx()
            )
            drawLine(
                color = roadColor,
                start = Offset(size.width * 0.28f, 0f),
                end = Offset(size.width * 0.65f, size.height),
                strokeWidth = 4.dp.toPx()
            )
            // Radar accuracy rings
            val center = Offset(size.width * 0.5f, size.height * 0.44f)
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = 0.18f),
                radius = 38.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = 0.45f),
                radius = 38.dp.toPx(),
                center = center,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            )
        }

        // Center Pin
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 14.dp)
                .size(42.dp),
            shape = CircleShape,
            color = if (isLiveLocation) Color(0xFF16A34A) else Color(0xFFE11D48),
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                TelefamLucideIcon(
                    kind = if (isLiveLocation) LucideIconKind.NAVIGATION_LIVE else LucideIconKind.MAP_PIN,
                    tint = Color.White,
                    size = 22.dp
                )
            }
        }

        // Top-left Live / View-Once badge
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isLiveLocation) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFF16A34A)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.NAVIGATION_LIVE,
                            tint = Color.White,
                            size = 12.dp
                        )
                        Text(
                            text = "LIVE TOKEN",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (isViewOnce) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color(0xFF059669)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.VIEW_ONCE_ONE,
                            tint = Color.White,
                            size = 12.dp
                        )
                        Text(
                            text = "VIEW ONCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bottom coordinate overlay bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = Color.Black.copy(alpha = 0.62f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = placeTitle.ifBlank { "Shared Location" },
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = addressLine.ifBlank {
                            String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = String.format(Locale.US, "%.4f, %.4f", latitude, longitude),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF34D399),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Poll Creation Sheet:
 * - Supports both standard text polls and image/candidate polls (each option can have an uploaded photo + caption/name beside it!)
 * - Supports "Allow multiple answers" toggle (or single-choice voting) and "Anonymous voting" toggle
 */
private data class EditablePollOptionDraft(
    val optionId: String = "opt_${UUID.randomUUID().toString().take(8)}",
    val text: String = "",
    val imageLocalPath: String? = null
)

@Composable
fun TelefamPollComposerDialog(
    onDismiss: () -> Unit,
    onSendPoll: (ChatPollPayload) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var question by remember { mutableStateOf("") }
    val options = remember {
        mutableStateListOf(
            EditablePollOptionDraft(text = ""),
            EditablePollOptionDraft(text = "")
        )
    }
    var allowMultipleVotes by remember { mutableStateOf(false) }
    var isAnonymous by remember { mutableStateOf(false) }
    var activeOptionIndexForPhoto by remember { mutableIntStateOf(-1) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val optionPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val idx = activeOptionIndexForPhoto
        if (uri != null && idx in options.indices) {
            scope.launch {
                val imported = TelefamAttachmentDeviceHelper.importSingleMediaUri(context, uri)
                if (imported != null && idx in options.indices) {
                    options[idx] = options[idx].copy(imageLocalPath = imported.localPath)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .widthIn(max = 500.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.BAR_CHART_POLL,
                                tint = Color(0xFFF59E0B),
                                size = 22.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Create Poll",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Add text or photo options with live vote percentages",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.CLOSE_X,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 20.dp
                        )
                    }
                }

                OutlinedTextField(
                    value = question,
                    onValueChange = {
                        question = it
                        validationError = null
                    },
                    label = { Text("Poll Question") },
                    placeholder = { Text("Ask a question...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("poll_question_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Text(
                    text = "Poll Options (tap camera icon to attach a photo to any option)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                options.forEachIndexed { index, draft ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option Image Picker / Thumbnail
                        Surface(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    activeOptionIndexForPhoto = index
                                    optionPhotoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("poll_option_photo_button_$index"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            if (!draft.imageLocalPath.isNullOrBlank()) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = File(draft.imageLocalPath),
                                        contentDescription = "Option ${index + 1} photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(18.dp)
                                            .clickable {
                                                options[index] = draft.copy(imageLocalPath = null)
                                            },
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.7f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            TelefamLucideIcon(
                                                kind = LucideIconKind.CLOSE_X,
                                                tint = Color.White,
                                                size = 11.dp
                                            )
                                        }
                                    }
                                }
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    TelefamLucideIcon(
                                        kind = LucideIconKind.MEDIA_GALLERY,
                                        tint = MaterialTheme.colorScheme.primary,
                                        size = 20.dp
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = draft.text,
                            onValueChange = { newText ->
                                options[index] = draft.copy(text = newText)
                                validationError = null
                            },
                            placeholder = { Text("Option ${index + 1} name or caption") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("poll_option_text_input_$index"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        if (options.size > 2) {
                            IconButton(
                                onClick = { options.removeAt(index) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                TelefamLucideIcon(
                                    kind = LucideIconKind.TRASH,
                                    tint = MaterialTheme.colorScheme.error,
                                    size = 18.dp
                                )
                            }
                        }
                    }
                }

                if (options.size < 10) {
                    OutlinedButton(
                        onClick = { options.add(EditablePollOptionDraft()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("poll_add_option_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.PLUS,
                            tint = MaterialTheme.colorScheme.primary,
                            size = 16.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Option")
                    }
                }

                HorizontalDivider()

                // Poll Settings
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Allow multiple answers",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (allowMultipleVotes) {
                                        "Participants can vote for more than one option"
                                    } else {
                                        "Single choice only — participants can select one option"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = allowMultipleVotes,
                                onCheckedChange = { allowMultipleVotes = it },
                                modifier = Modifier.testTag("poll_multiple_votes_toggle")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Anonymous voting",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Show vote counts & percentages without voter identities",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isAnonymous,
                                onCheckedChange = { isAnonymous = it },
                                modifier = Modifier.testTag("poll_anonymous_toggle")
                            )
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cleanQ = question.trim()
                            val validOpts = options.filter {
                                it.text.trim().isNotBlank() || !it.imageLocalPath.isNullOrBlank()
                            }
                            if (cleanQ.isBlank()) {
                                validationError = "Please enter a poll question."
                                return@Button
                            }
                            if (validOpts.size < 2) {
                                validationError = "Please provide at least 2 poll options."
                                return@Button
                            }
                            val payload = ChatPollPayload(
                                pollId = "poll_${UUID.randomUUID()}",
                                question = cleanQ,
                                options = validOpts.mapIndexed { idx, d ->
                                    ChatPollOptionItem(
                                        optionId = d.optionId,
                                        text = d.text.trim().ifBlank { "Option ${idx + 1}" },
                                        imageLocalPath = d.imageLocalPath,
                                        voterUserIds = emptyList()
                                    )
                                },
                                allowMultipleVotes = allowMultipleVotes,
                                isAnonymous = isAnonymous,
                                isClosed = false
                            )
                            onSendPoll(payload)
                        },
                        modifier = Modifier.testTag("poll_send_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.SEND,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            size = 16.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Poll", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Event Creation Sheet:
 * - Creates a structured E2EE Event card with Title, Description, Start/End Schedule, Location, and interactive RSVP tracking.
 */
@Composable
fun TelefamEventComposerDialog(
    onDismiss: () -> Unit,
    onSendEvent: (ChatEventPayload) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var locationLabel by remember { mutableStateOf("") }
    var selectedPresetIndex by remember { mutableIntStateOf(0) }
    var durationMinutes by remember { mutableIntStateOf(60) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val now = remember { System.currentTimeMillis() }
    val timePresets = remember(now) {
        listOf(
            "In 1 Hour" to (now + 3_600_000L),
            "Tonight 7:00 PM" to (now + 6 * 3_600_000L),
            "Tomorrow 10:00 AM" to (now + 24 * 3_600_000L),
            "This Weekend" to (now + 72 * 3_600_000L),
            "Next Week" to (now + 7 * 24 * 3_600_000L)
        )
    }
    val startTimeMs = timePresets[selectedPresetIndex].second
    val endTimeMs = startTimeMs + durationMinutes * 60_000L
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .widthIn(max = 480.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = Color(0xFFF43F5E).copy(alpha = 0.14f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            TelefamLucideIcon(
                                kind = LucideIconKind.CALENDAR_EVENT,
                                tint = Color(0xFFF43F5E),
                                size = 22.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Create Event",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Share an encrypted event invitation with live RSVP tracking",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.CLOSE_X,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            size = 20.dp
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text("Event Title") },
                    placeholder = { Text("e.g., Product Strategy Sync") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Agenda") },
                    placeholder = { Text("Add details, notes, or agenda...") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_description_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                Text(
                    text = "Date & Start Time (${dateFormat.format(Date(startTimeMs))})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timePresets.forEachIndexed { idx, (label, _) ->
                        FilterChip(
                            selected = selectedPresetIndex == idx,
                            onClick = { selectedPresetIndex = idx },
                            label = { Text(label) }
                        )
                    }
                }

                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30 to "30 min", 60 to "1 hr", 120 to "2 hrs", 240 to "4 hrs").forEach { (mins, label) ->
                        FilterChip(
                            selected = durationMinutes == mins,
                            onClick = { durationMinutes = mins },
                            label = { Text(label) }
                        )
                    }
                }

                OutlinedTextField(
                    value = locationLabel,
                    onValueChange = { locationLabel = it },
                    label = { Text("Location or Meeting Link") },
                    placeholder = { Text("e.g., Conference Room A / Telefam Call") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_location_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cleanTitle = title.trim()
                            if (cleanTitle.isBlank()) {
                                validationError = "Please enter an event title."
                                return@Button
                            }
                            onSendEvent(
                                ChatEventPayload(
                                    eventId = "evt_${UUID.randomUUID()}",
                                    title = cleanTitle,
                                    description = description.trim(),
                                    startTimeMs = startTimeMs,
                                    endTimeMs = endTimeMs,
                                    locationLabel = locationLabel.trim().ifBlank { "Telefam Encrypted Room" }
                                )
                            )
                        },
                        modifier = Modifier.testTag("event_send_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        TelefamLucideIcon(
                            kind = LucideIconKind.SEND,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            size = 16.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Event", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
