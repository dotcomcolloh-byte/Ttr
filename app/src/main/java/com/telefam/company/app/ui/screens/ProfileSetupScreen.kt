package com.telefam.company.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.TelefamAvatarCircleDark
import com.example.ui.theme.TelefamAvatarCircleLight
import com.example.ui.theme.TelefamError
import com.example.ui.theme.TelefamInputBorderDark
import com.example.ui.theme.TelefamInputBorderLight
import com.example.ui.theme.TelefamRed
import com.example.ui.theme.TelefamSuccess
import com.example.ui.theme.isTelefamDarkTheme
import com.telefam.company.app.data.MediaFingerprintEntity
import com.telefam.company.app.domain.CountryDialCode
import com.telefam.company.app.domain.TelefamCountries
import com.telefam.company.app.ui.TelefamUiState
import com.telefam.company.app.ui.components.ImageCropperDialog
import com.telefam.company.app.ui.components.KenyaFlagVectorIcon
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pixel-faithful Profile Setup Screen matching Reference Image 2 (file_00000000e994821181b51c0b9278cffb.png)
 * with:
 * - Optional Profile Photo + Perceptual dHash & SHA-256 Fingerprinting for future reuse
 * - Automatic Full Name & Unique Username autofill from email prefix
 * - Real-time backend username uniqueness validation & clickable unique username suggestions
 * - Searchable 240+ Global Country Dial Code picker (defaulting to Kenya +254)
 * - Strict 13+ years Date of Birth validation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    uiState: TelefamUiState,
    onFullNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onApplySuggestedUsername: (String) -> Unit,
    onCountrySelected: (CountryDialCode) -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onGenderSelected: (String) -> Unit,
    onDateOfBirthSelected: (String) -> Unit,
    onBioChanged: (String) -> Unit,
    onAvatarUriPicked: (Uri) -> Unit,
    onCancelCrop: () -> Unit,
    onConfirmCrop: (scale: Float, offsetXNorm: Float, offsetYNorm: Float, rotationDeg: Float) -> Unit,
    onSubmitProfile: () -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onAvatarUriPicked(uri)
        }
    }

    var showCountryDialog by remember { mutableStateOf(false) }
    var showGenderMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isDark = isTelefamDarkTheme()
    val inputBorderColor = if (isDark) TelefamInputBorderDark else TelefamInputBorderLight
    val avatarFillColor = if (isDark) TelefamAvatarCircleDark else TelefamAvatarCircleLight

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TelefamRed)
            .imePadding()
            .testTag("profile_setup_screen_root")
    ) {
        // 1. Top Red Header Bar ("Profile Setup" / "Tell us about yourself")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .testTag("profile_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.profile_setup_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.profile_setup_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                    color = Color.White.copy(alpha = 0.92f)
                )
            }
        }

        // 2. White Rounded Top Sheet Container
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideTabletOrDesktop = maxWidth >= 840.dp

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = if (isWideTabletOrDesktop) 48.dp else 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = if (isWideTabletOrDesktop) 880.dp else 520.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Circular Dashed Red Avatar Upload Widget
                        ProfileAvatarUploadHeader(
                            uiState = uiState,
                            avatarFillColor = avatarFillColor,
                            onAvatarClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )

                        // Error Banner if any
                        uiState.errorMessage?.let { err ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = TelefamError.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_error_banner")
                            ) {
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TelefamError,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        if (isWideTabletOrDesktop) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    FullNameField(
                                        uiState = uiState,
                                        inputBorderColor = inputBorderColor,
                                        onFullNameChanged = onFullNameChanged
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    UsernameFieldWithSuggestions(
                                        uiState = uiState,
                                        inputBorderColor = inputBorderColor,
                                        onUsernameChanged = onUsernameChanged,
                                        onApplySuggestedUsername = onApplySuggestedUsername
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    PhoneNumberWithFlagField(
                                        uiState = uiState,
                                        inputBorderColor = inputBorderColor,
                                        onOpenCountryPicker = { showCountryDialog = true },
                                        onPhoneNumberChanged = onPhoneNumberChanged
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    GenderDropdownField(
                                        uiState = uiState,
                                        inputBorderColor = inputBorderColor,
                                        showGenderMenu = showGenderMenu,
                                        onToggleMenu = { showGenderMenu = it },
                                        onGenderSelected = onGenderSelected
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    DateOfBirthPickerField(
                                        uiState = uiState,
                                        inputBorderColor = inputBorderColor,
                                        onOpenDatePicker = { showDatePicker = true }
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    BioMultilineField(
                                        uiState = uiState,
                                        inputBorderColor = inputBorderColor,
                                        onBioChanged = onBioChanged
                                    )
                                }
                            }
                        } else {
                            FullNameField(
                                uiState = uiState,
                                inputBorderColor = inputBorderColor,
                                onFullNameChanged = onFullNameChanged
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            UsernameFieldWithSuggestions(
                                uiState = uiState,
                                inputBorderColor = inputBorderColor,
                                onUsernameChanged = onUsernameChanged,
                                onApplySuggestedUsername = onApplySuggestedUsername
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            PhoneNumberWithFlagField(
                                uiState = uiState,
                                inputBorderColor = inputBorderColor,
                                onOpenCountryPicker = { showCountryDialog = true },
                                onPhoneNumberChanged = onPhoneNumberChanged
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GenderDropdownField(
                                uiState = uiState,
                                inputBorderColor = inputBorderColor,
                                showGenderMenu = showGenderMenu,
                                onToggleMenu = { showGenderMenu = it },
                                onGenderSelected = onGenderSelected
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            DateOfBirthPickerField(
                                uiState = uiState,
                                inputBorderColor = inputBorderColor,
                                onOpenDatePicker = { showDatePicker = true }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            BioMultilineField(
                                uiState = uiState,
                                inputBorderColor = inputBorderColor,
                                onBioChanged = onBioChanged
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Solid Red "Continue" Button
                        Button(
                            onClick = onSubmitProfile,
                            enabled = !uiState.isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TelefamRed,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(vertical = 15.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("profile_continue_button")
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.continue_button),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 16.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Interactive Image Cropper Dialog
    uiState.pendingCropBitmap?.let { bmp ->
        ImageCropperDialog(
            bitmap = bmp,
            onCancel = onCancelCrop,
            onConfirmCrop = onConfirmCrop
        )
    }

    // Searchable 240+ Country Code Selector Dialog
    if (showCountryDialog) {
        SearchableCountryPickerDialog(
            selectedCountry = uiState.selectedCountry,
            onSelectCountry = {
                onCountrySelected(it)
                showCountryDialog = false
            },
            onDismiss = { showCountryDialog = false }
        )
    }

    // Material 3 DatePickerDialog enforcing 13+ Years Old
    if (showDatePicker) {
        val utc = TimeZone.getTimeZone("UTC")
        val maxAllowedMillis = remember {
            Calendar.getInstance(utc).apply {
                add(Calendar.YEAR, -13)
            }.timeInMillis
        }
        val minAllowedMillis = remember {
            Calendar.getInstance(utc).apply {
                add(Calendar.YEAR, -120)
            }.timeInMillis
        }
        val defaultEighteenYearsAgoMillis = remember {
            Calendar.getInstance(utc).apply {
                add(Calendar.YEAR, -18)
            }.timeInMillis
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = defaultEighteenYearsAgoMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis in minAllowedMillis..maxAllowedMillis
                }

                override fun isSelectableYear(year: Int): Boolean {
                    val currentYear = Calendar.getInstance(utc).get(Calendar.YEAR)
                    return year in (currentYear - 120)..(currentYear - 13)
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis ?: defaultEighteenYearsAgoMillis
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                            timeZone = utc
                        }
                        onDateOfBirthSelected(sdf.format(Date(millis)))
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("confirm_dob_button")
                ) {
                    Text("OK", color = TelefamRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ProfileAvatarUploadHeader(
    uiState: TelefamUiState,
    avatarFillColor: Color,
    onAvatarClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(122.dp)
                .clickable { onAvatarClick() }
                .testTag("profile_avatar_upload_box"),
            contentAlignment = Alignment.Center
        ) {
            // Outer Red Dashed Circle
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 1.6.dp.toPx()
                drawCircle(
                    color = TelefamRed.copy(alpha = 0.78f),
                    radius = (size.minDimension - strokePx) / 2f,
                    style = Stroke(
                        width = strokePx,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f), 0f)
                    )
                )
            }

            // Inner Blush Pink Circle or Verified Cropped Avatar
            Box(
                modifier = Modifier
                    .size(106.dp)
                    .clip(CircleShape)
                    .background(avatarFillColor),
                contentAlignment = Alignment.Center
            ) {
                val localAvatarPath = uiState.sanitizedAvatar?.secureFilePath ?: uiState.savedProfile?.avatarLocalPath
                if (localAvatarPath != null && File(localAvatarPath).exists()) {
                    AsyncImage(
                        model = File(localAvatarPath),
                        contentDescription = stringResource(R.string.upload_profile_photo),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = stringResource(R.string.upload_profile_photo),
                        tint = TelefamRed,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            // Bottom-Right Red Circle '+' Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-6).dp, y = (-4).dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(TelefamRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add profile photo",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.upload_profile_photo),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 18.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.upload_profile_photo_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileFieldHeader(
    icon: ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TelefamRed,
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FullNameField(
    uiState: TelefamUiState,
    inputBorderColor: Color,
    onFullNameChanged: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileFieldHeader(
            icon = Icons.Default.Person,
            label = stringResource(R.string.full_name_label)
        )
        OutlinedTextField(
            value = uiState.fullName,
            onValueChange = onFullNameChanged,
            placeholder = {
                Text(
                    text = stringResource(R.string.full_name_placeholder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = inputBorderColor,
                focusedBorderColor = TelefamRed
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_full_name_input")
        )
    }
}

@Composable
private fun UsernameFieldWithSuggestions(
    uiState: TelefamUiState,
    inputBorderColor: Color,
    onUsernameChanged: (String) -> Unit,
    onApplySuggestedUsername: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileFieldHeader(
            icon = Icons.Default.AlternateEmail,
            label = stringResource(R.string.username_label)
        )
        OutlinedTextField(
            value = uiState.username,
            onValueChange = onUsernameChanged,
            placeholder = {
                Text(
                    text = stringResource(R.string.username_placeholder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (uiState.isUsernameAvailable == false) TelefamError else inputBorderColor,
                focusedBorderColor = if (uiState.isUsernameAvailable == false) TelefamError else TelefamRed
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_username_input")
        )

        // Only show backend-generated unique suggestions when the typed username is actually taken
        if (uiState.isUsernameAvailable == false && uiState.usernameSuggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("username_suggestions_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.usernameSuggestions.forEachIndexed { idx, suggestion ->
                    Surface(
                        onClick = { onApplySuggestedUsername(suggestion) },
                        shape = RoundedCornerShape(50),
                        color = TelefamRed.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, TelefamRed.copy(alpha = 0.35f)),
                        modifier = Modifier.testTag("username_suggestion_chip_$idx")
                    ) {
                        Text(
                            text = "@$suggestion",
                            style = MaterialTheme.typography.labelMedium,
                            color = TelefamRed,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneNumberWithFlagField(
    uiState: TelefamUiState,
    inputBorderColor: Color,
    onOpenCountryPicker: () -> Unit,
    onPhoneNumberChanged: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileFieldHeader(
            icon = Icons.Default.Call,
            label = stringResource(R.string.phone_number_label)
        )
        // Combined Split Box matching Reference Image 2 (Flag + Dropdown on left, Vertical Divider, Phone Input on right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .border(1.dp, inputBorderColor, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable { onOpenCountryPicker() }
                    .padding(horizontal = 14.dp)
                    .testTag("profile_country_code_selector"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (uiState.selectedCountry.isoCode == "KE") {
                    KenyaFlagVectorIcon(width = 28.dp, height = 20.dp)
                } else {
                    Text(
                        text = uiState.selectedCountry.flagEmoji,
                        fontSize = 19.sp
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select country code",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(inputBorderColor)
            )

            // Phone Number Text Input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (uiState.phoneNumber.isEmpty()) {
                    Text(
                        text = stringResource(R.string.phone_number_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                BasicTextField(
                    value = uiState.phoneNumber,
                    onValueChange = onPhoneNumberChanged,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(TelefamRed),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_phone_input")
                )
            }
        }
    }
}

@Composable
private fun GenderDropdownField(
    uiState: TelefamUiState,
    inputBorderColor: Color,
    showGenderMenu: Boolean,
    onToggleMenu: (Boolean) -> Unit,
    onGenderSelected: (String) -> Unit
) {
    val genderOptions = listOf(
        stringResource(R.string.gender_female),
        stringResource(R.string.gender_male),
        stringResource(R.string.gender_non_binary),
        stringResource(R.string.gender_prefer_not_to_say)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileFieldHeader(
            icon = Icons.Default.People,
            label = stringResource(R.string.gender_label)
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .border(1.dp, inputBorderColor, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleMenu(true) }
                    .padding(horizontal = 16.dp)
                    .testTag("profile_gender_selector"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = uiState.gender.ifBlank { stringResource(R.string.gender_placeholder) },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (uiState.gender.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.gender_label),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showGenderMenu,
                onDismissRequest = { onToggleMenu(false) }
            ) {
                genderOptions.forEachIndexed { idx, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onGenderSelected(option)
                            onToggleMenu(false)
                        },
                        modifier = Modifier.testTag("gender_option_$idx")
                    )
                }
            }
        }
    }
}

@Composable
private fun DateOfBirthPickerField(
    uiState: TelefamUiState,
    inputBorderColor: Color,
    onOpenDatePicker: () -> Unit
) {
    val dobError = uiState.profileFieldErrors["dob"]
    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileFieldHeader(
            icon = Icons.Default.CalendarMonth,
            label = stringResource(R.string.dob_label)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .border(
                    1.dp,
                    if (dobError != null) TelefamError else inputBorderColor,
                    RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .clickable { onOpenDatePicker() }
                .padding(horizontal = 16.dp)
                .testTag("profile_dob_selector"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = uiState.dateOfBirth.ifBlank { stringResource(R.string.dob_placeholder) },
                style = MaterialTheme.typography.bodyLarge,
                color = if (uiState.dateOfBirth.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.dob_label),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BioMultilineField(
    uiState: TelefamUiState,
    inputBorderColor: Color,
    onBioChanged: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ProfileFieldHeader(
            icon = Icons.Outlined.Sms,
            label = stringResource(R.string.bio_label)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(114.dp)
                .border(1.dp, inputBorderColor, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            if (uiState.bio.isEmpty()) {
                Text(
                    text = stringResource(R.string.bio_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
            BasicTextField(
                value = uiState.bio,
                onValueChange = onBioChanged,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(TelefamRed),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 20.dp)
                    .testTag("profile_bio_input")
            )
            Text(
                text = "${uiState.bio.length}/150",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .testTag("profile_bio_counter")
            )
        }
    }
}

@Composable
internal fun SearchableCountryPickerDialog(
    selectedCountry: CountryDialCode,
    onSelectCountry: (CountryDialCode) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery) {
        TelefamCountries.search(searchQuery)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Country (${TelefamCountries.ALL.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search country, ISO, or +code…") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search country",
                            tint = TelefamRed
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TelefamRed
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("country_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filteredCountries.isEmpty()) {
                    Text(
                        text = "No countries matching \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                    ) {
                        items(filteredCountries, key = { "${it.isoCode}_${it.dialCode}" }) { country ->
                            Surface(
                                onClick = { onSelectCountry(country) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (country.isoCode == selectedCountry.isoCode) {
                                    TelefamRed.copy(alpha = 0.1f)
                                } else {
                                    Color.Transparent
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("country_option_${country.isoCode}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = country.flagEmoji, fontSize = 20.sp)
                                        Text(
                                            text = "${country.name} (${country.isoCode})",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = country.dialCode,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TelefamRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
