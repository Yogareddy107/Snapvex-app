package com.example.ui.screens

import android.widget.Toast
import com.example.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.database.*
import com.example.viewmodel.SnapVexViewModel
import com.example.viewmodel.ActivityLogItem
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapVexApp(viewModel: SnapVexViewModel) {
    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val studios by viewModel.studios.collectAsStateWithLifecycle()
    val listLeads by viewModel.leads.collectAsStateWithLifecycle()
    val listEvents by viewModel.events.collectAsStateWithLifecycle()
    val listQuotations by viewModel.quotations.collectAsStateWithLifecycle()
    val listPayments by viewModel.payments.collectAsStateWithLifecycle()
    val listNotifications by viewModel.notifications.collectAsStateWithLifecycle()
    val listTickets by viewModel.supportTickets.collectAsStateWithLifecycle()
    val listActivityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()

    val activeStudio by viewModel.activeStudio.collectAsStateWithLifecycle(initialValue = null)
    val activeEvent by viewModel.activeEvent.collectAsStateWithLifecycle(initialValue = null)
    val galleryItems by viewModel.galleryItems.collectAsStateWithLifecycle(initialValue = emptyList())

    val isUploading by viewModel.isUploading.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Navigation & Modal triggers
    var showCreateLeadDialog by remember { mutableStateOf(false) }
    var showCreateEventDialog by remember { mutableStateOf(false) }
    var showReceivePaymentDialog by remember { mutableStateOf(false) }
    var activePaymentForDialog by remember { mutableStateOf<PaymentEntity?>(null) }
    var showNotificationDrawer by remember { mutableStateOf(false) }
    var activeGalleryItemForAIDialog by remember { mutableStateOf<GalleryItemEntity?>(null) }

    Scaffold(
        topBar = {
            Column {
                // Persistent elegant Role Control controller floating on top
                DemoRoleController(
                    currentRole = currentRole,
                    onRoleSelected = { role ->
                        viewModel.switchRole(role)
                        // Auto reset or set helper event selections for test convenience
                        if (role == "Client" || role == "Photographer" || role == "Editor") {
                            val targetEvent = listEvents.firstOrNull()
                            if (targetEvent != null) {
                                viewModel.selectEvent(targetEvent.id)
                            }
                        }
                    },
                    notificationsCount = listNotifications.filter { !it.isRead }.size,
                    onNotifClick = { showNotificationDrawer = !showNotificationDrawer }
                )
                
                // Active Studio Quick Banner if modern role is active
                if (currentRole != "Landing Page" && activeStudio != null) {
                    StudioCapacityAlertBanner(studio = activeStudio!!)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main views mapping based on active role
            AnimatedContent(
                targetState = currentRole,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "RoleTransition"
            ) { role ->
                when (role) {
                    "Landing Page" -> LandingPageScreen(
                        viewModel = viewModel,
                        activeStudio = activeStudio,
                        onRegisterClick = {
                            viewModel.registerStudio(it.first, it.second)
                        }
                    )
                    "Super Admin" -> SuperAdminDashboard(
                        studios = studios,
                        tickets = listTickets,
                        notifications = listNotifications,
                        onApproveStudio = { viewModel.approveStudio(it) },
                        onApproveDrive = { viewModel.approveDriveRequest(it) },
                        onGrantStorage = { id, mb -> viewModel.grantExtraStorage(id, mb) }
                    )
                    "Studio Admin" -> StudioAdminDashboard(
                        studio = activeStudio,
                        leads = listLeads,
                        events = listEvents,
                        quotations = listQuotations,
                        payments = listPayments,
                        activityLogs = listActivityLogs,
                        onAddLeadClick = { showCreateLeadDialog = true },
                        onAddEventClick = { showCreateEventDialog = true },
                        onDeleteLead = { viewModel.deleteLead(it) },
                        onDeleteEvent = { viewModel.deleteEvent(it) },
                        onUpdateLeadStatus = { id, status -> viewModel.updateLeadStatus(id, status) },
                        onConnectStorage = { id, connect, prov -> viewModel.connectDrive(id, connect, prov) },
                        onRequestDriveApproval = { viewModel.requestDriveApproval(it) },
                        onExportQuotation = { id, platform -> viewModel.exportQuotation(id, platform) },
                        onLogPaymentClick = { payment ->
                            activePaymentForDialog = payment
                            showReceivePaymentDialog = true
                        },
                        onLogActivityItem = { role, action, type, eventName, eventId ->
                            viewModel.logActivity(role, action, type, eventName, eventId)
                        }
                    )
                    "Photographer" -> PhotographerWorkspace(
                        events = listEvents,
                        selectedEvent = activeEvent,
                        galleryItems = galleryItems,
                        isUploading = isUploading,
                        uploadProgress = uploadProgress,
                        onEventSelect = { viewModel.selectEvent(it) },
                        onUploadImageSimulate = { eventId, name, type, size ->
                            viewModel.photographerUploadMedia(eventId, name, type, size)
                        },
                        onInspectAI = { activeGalleryItemForAIDialog = it }
                    )
                    "Editor" -> EditorWorkspace(
                        events = listEvents,
                        selectedEvent = activeEvent,
                        galleryItems = galleryItems,
                        isUploading = isUploading,
                        uploadProgress = uploadProgress,
                        onEventSelect = { viewModel.selectEvent(it) },
                        onDownloadRaw = { viewModel.editorDownloadRawMedia(it) },
                        onUploadEdited = { eventId, originalRaw ->
                            viewModel.editorUploadEditedMedia(eventId, originalRaw, 6.8)
                        }
                    )
                    "Client" -> ClientGalleryScreen(
                        events = listEvents,
                        selectedEvent = activeEvent,
                        galleryItems = galleryItems,
                        onEventSelect = { viewModel.selectEvent(it) },
                        onFavoritePhoto = { id, fav -> viewModel.clientFavoritePhoto(id, fav) },
                        onChooseForAlbum = { id, select -> viewModel.clientSetAlbumSelection(id, select) },
                        onSubmitAlbumDecision = { eventId, approve, notes ->
                            viewModel.clientApproveAlbum(eventId, approve, notes)
                            Toast.makeText(context, if (approve) "Album design approved!" else "Revision requested", Toast.LENGTH_LONG).show()
                        },
                        onInspectAI = { activeGalleryItemForAIDialog = it }
                    )
                    else -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Unauthorized screen context configuration")
                        }
                    }
                }
            }

            // Standard CRM Modals
            if (showCreateLeadDialog) {
                CreateLeadDialog(
                    onDismiss = { showCreateLeadDialog = false },
                    onConfirm = { name, source, status, valAmt, phone, email, notes ->
                        viewModel.addLead(name, source, status, valAmt, phone, email, notes)
                        showCreateLeadDialog = false
                    }
                )
            }

            if (showCreateEventDialog) {
                CreateEventDialog(
                    onDismiss = { showCreateEventDialog = false },
                    onConfirm = { name, client, type, date, location, price, team, provider ->
                        viewModel.createEvent(name, client, type, date, location, price, team, provider)
                        showCreateEventDialog = false
                    }
                )
            }

            if (showReceivePaymentDialog && activePaymentForDialog != null) {
                ReceivePaymentDialog(
                    payment = activePaymentForDialog!!,
                    onDismiss = { showReceivePaymentDialog = false },
                    onConfirm = { amount ->
                        viewModel.recordPayment(activePaymentForDialog!!.id, amount)
                        showReceivePaymentDialog = false
                    }
                )
            }

            // Notification Drawer Overlay
            if (showNotificationDrawer) {
                NotificationDrawerOverlay(
                    notificationsList = listNotifications,
                    onDismiss = { showNotificationDrawer = false },
                    onMarkRead = { id -> viewModel.markNotificationAsRead(id) },
                    onMarkAllRead = { viewModel.markAllNotificationsAsRead() }
                )
            }

            // AI Metadata Inspect Dialog Drawer
            if (activeGalleryItemForAIDialog != null) {
                AIInspectReportDialog(
                    item = activeGalleryItemForAIDialog!!,
                    onDismiss = { activeGalleryItemForAIDialog = null }
                )
            }
        }
    }
}

// ========================================================
// REUSABLE DASHBOARD ACCENTS & DEMO MANAGER
// ========================================================

@Composable
fun DemoRoleController(
    currentRole: String,
    onRoleSelected: (String) -> Unit,
    notificationsCount: Int,
    onNotifClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary) // Studio Active Status Pulse
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "SnapVex Studio OS",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "v2.6 Enterprise",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = onNotifClick,
                    modifier = Modifier.testTag("notif_bell_btn")
                ) {
                    BadgedBox(
                        badge = {
                            if (notificationsCount > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError) {
                                    Text(notificationsCount.toString(), fontSize = 9.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Alert notifications",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Text(
                text = "DEMO ROLE SIMULATOR (Swap instantly to preview workflows)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val roles = listOf("Landing Page", "Super Admin", "Studio Admin", "Photographer", "Editor", "Client")
                roles.forEach { role ->
                    val isActive = currentRole == role
                    FilterChip(
                        selected = isActive,
                        onClick = { onRoleSelected(role) },
                        label = { Text(role, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.secondary,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = BorderStroke(1.dp, if (isActive) Color.Transparent else MaterialTheme.colorScheme.outline),
                        modifier = Modifier.testTag("role_pill_${role.lowercase().replace(" ", "_")}")
                    )
                }
            }
        }
    }
}

@Composable
fun StudioCapacityAlertBanner(studio: StudioEntity) {
    val usagePercent = (studio.storageUsedMb / studio.storageLimitMb) * 100
    val formattedUsed = String.format("%.1f", studio.storageUsedMb / 1024.0)
    val formattedLimit = String.format("%.1f", studio.storageLimitMb / 1024.0)

    val color = when {
        usagePercent >= 100.0 -> Color.Red
        usagePercent >= 90.0 -> Color(0xFFFF6D00)
        usagePercent >= 80.0 -> Color(0xFFFFD600)
        else -> Color(0xFF00E676)
    }

    val warningMsg = when {
        usagePercent >= 100.0 -> "STORAGE FULL (100%). Studio uploads disabled. Upgrades needed."
        usagePercent >= 90.0 -> "CRITICAL STORAGE ALERT (${String.format("%.1f", usagePercent)}% used). Notify administrators."
        usagePercent >= 80.0 -> "Storage Capacity Warn (${String.format("%.1f", usagePercent)}% capacity filled)."
        else -> "Dual Storage Live (${formattedUsed} GB of ${formattedLimit} GB utilized)"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f))
            .padding(vertical = 6.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (usagePercent >= 90) Icons.Default.Warning else Icons.Default.Storage,
                contentDescription = "Storage Status",
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = warningMsg,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (usagePercent >= 80.0) {
            Text(
                text = "Upgrade Extra Storage",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .background(Color.Red, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

// ========================================================
// SCREEN 1: LANDING SAAS SALES PAGE
// ========================================================

@Composable
fun LandingPageScreen(
    viewModel: SnapVexViewModel,
    activeStudio: StudioEntity?,
    onRegisterClick: (Pair<String, String>) -> Unit
) {
    val registerStep by viewModel.registerStep.collectAsStateWithLifecycle()
    val rName by viewModel.registeredStudioName.collectAsStateWithLifecycle()
    val rEmail by viewModel.registeredStudioEmail.collectAsStateWithLifecycle()
    val phoneAuthStep by viewModel.phoneAuthStep.collectAsStateWithLifecycle()

    var regNameInput by remember { mutableStateOf("") }
    var regEmailInput by remember { mutableStateOf("") }
    var verCodeInput by remember { mutableStateOf("") }

    // Multi-step form sub-wizard states
    var currentSubStep by remember { mutableStateOf(1) }
    var regPhoneInput by remember { mutableStateOf("") }
    var regTeamSize by remember { mutableStateOf("1-5 people") }
    var regStorageProvider by remember { mutableStateOf("Platform Storage") }
    var regAdminPassword by remember { mutableStateOf("") }
    var regAdminPasswordConfirm by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordConfirmError by remember { mutableStateOf<String?>(null) }
    var otpError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp)
    ) {
        // 1. HERO SECTION
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SNAPVEX STUDIO OS",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Enterprise Creative CRM\n& Hybrid Storage Engine",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 38.sp,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Ditch raw folder clutter. Connect Google Drive or OneDrive APIs seamlessly. Manage leads, track photo galleries, and deploy face-tagging automated AI.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(Modifier.height(24.dp))

                // STUDIO REGISTRATION INTERACTIVE COMPOSABLE
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "INITIALIZE YOUR STUDIO TENANT",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = SleekAmber,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(8.dp))

                        when (registerStep) {
                            "Form" -> {
                                // Multi-step header progress status
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Step $currentSubStep of 3", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(Modifier.size(height = 4.dp, width = 16.dp).background(if (currentSubStep >= 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                                        Box(Modifier.size(height = 4.dp, width = 16.dp).background(if (currentSubStep >= 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                                        Box(Modifier.size(height = 4.dp, width = 16.dp).background(if (currentSubStep >= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                                    }
                                }
                                Spacer(Modifier.height(12.dp))

                                when (currentSubStep) {
                                    1 -> {
                                        Text("BASIC STUDIO DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = regNameInput,
                                            onValueChange = { 
                                                regNameInput = it
                                                if (it.length >= 3) nameError = null
                                            },
                                            label = { Text("Studio Name") },
                                            isError = nameError != null,
                                            placeholder = { Text("e.g. Dreamweaver Weddings") },
                                            modifier = Modifier.fillMaxWidth().testTag("reg_studio_name_input"),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        if (nameError != null) {
                                            Text(nameError!!, color = SleekRed, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                                        }

                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = regEmailInput,
                                            onValueChange = { 
                                                regEmailInput = it
                                                if (it.contains("@") && it.contains(".")) emailError = null
                                            },
                                            label = { Text("Business Email Address") },
                                            isError = emailError != null,
                                            placeholder = { Text("e.g. contact@dreamweaver.com") },
                                            modifier = Modifier.fillMaxWidth().testTag("reg_studio_email_input"),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        if (emailError != null) {
                                            Text(emailError!!, color = SleekRed, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                                        }

                                        Spacer(Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                var hasError = false
                                                if (regNameInput.length < 3) {
                                                    nameError = "Studio Name must be at least 3 characters."
                                                    hasError = true
                                                }
                                                if (!regEmailInput.contains("@") || !regEmailInput.contains(".")) {
                                                    emailError = "Please enter a valid email address."
                                                    hasError = true
                                                }
                                                if (!hasError) {
                                                    currentSubStep = 2
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("reg_studio_step1_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Next: Choose Storage & Operations", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    2 -> {
                                        Text("STORAGE PROVIDER & OPERATIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = regPhoneInput,
                                            onValueChange = { 
                                                regPhoneInput = it
                                                if (it.length >= 10) phoneError = null
                                            },
                                            label = { Text("Business Whatsapp / Contact") },
                                            isError = phoneError != null,
                                            placeholder = { Text("e.g. +91 98765 43210") },
                                            modifier = Modifier.fillMaxWidth().testTag("reg_studio_phone_input"),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        if (phoneError != null) {
                                            Text(phoneError!!, color = SleekRed, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                                        }

                                        Spacer(Modifier.height(12.dp))
                                        Text("Primary Automated Storage Portal API:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("Platform Storage", "Google Drive", "OneDrive").forEach { prov ->
                                                val isSelected = regStorageProvider == prov
                                                Button(
                                                    onClick = { regStorageProvider = prov },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(prov, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))
                                        Text("Expected Team Scale:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("1-5 people", "6-20 people", "21+ studios").forEach { scale ->
                                                val isSelected = regTeamSize == scale
                                                Button(
                                                    onClick = { regTeamSize = scale },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(scale, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(16.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { currentSubStep = 1 },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Back", fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = {
                                                    if (regPhoneInput.length < 10) {
                                                        phoneError = "Phone number must be at least 10 digits."
                                                    } else {
                                                        currentSubStep = 3
                                                    }
                                                },
                                                modifier = Modifier.weight(1.5f).testTag("reg_studio_step2_btn"),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Next: Credentials", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    3 -> {
                                        Text("ADMIN SECURE CREDENTIALS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = regAdminPassword,
                                            onValueChange = { 
                                                regAdminPassword = it
                                                if (it.length >= 6) passwordError = null
                                            },
                                            label = { Text("Account Password") },
                                            isError = passwordError != null,
                                            placeholder = { Text("At least 6 characters") },
                                            modifier = Modifier.fillMaxWidth().testTag("reg_studio_password_input"),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        if (passwordError != null) {
                                            Text(passwordError!!, color = SleekRed, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                                        }

                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = regAdminPasswordConfirm,
                                            onValueChange = { 
                                                regAdminPasswordConfirm = it
                                                if (it == regAdminPassword) passwordConfirmError = null
                                            },
                                            label = { Text("Confirm Sandbox Password") },
                                            isError = passwordConfirmError != null,
                                            placeholder = { Text("Re-enter password") },
                                            modifier = Modifier.fillMaxWidth().testTag("reg_studio_password_confirm_input"),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        if (passwordConfirmError != null) {
                                            Text(passwordConfirmError!!, color = SleekRed, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                                        }

                                        Spacer(Modifier.height(16.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { currentSubStep = 2 },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Back", fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = {
                                                    var hasErr = false
                                                    if (regAdminPassword.length < 6) {
                                                        passwordError = "Password must be at least 6 characters."
                                                        hasErr = true
                                                    }
                                                    if (regAdminPasswordConfirm != regAdminPassword) {
                                                        passwordConfirmError = "Passwords do not match."
                                                        hasErr = true
                                                    }
                                                    if (!hasErr) {
                                                        onRegisterClick(regNameInput to regEmailInput)
                                                    }
                                                },
                                                modifier = Modifier.weight(1.5f).testTag("reg_studio_btn"),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Request OTP Setup", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                            "Verification" -> {
                                Text(
                                    text = "Secure Verification Code Dispatched to $rEmail.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "SANDBOX SIMULATOR CODE: Enter OTP '949091' to verify instant access.",
                                    fontSize = 11.sp,
                                    color = SleekAmber,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = verCodeInput,
                                    onValueChange = { 
                                        verCodeInput = it
                                        if (it.length == 6) otpError = null
                                    },
                                    label = { Text("6-Digit Verification OTP") },
                                    isError = otpError != null,
                                    placeholder = { Text("949091") },
                                    modifier = Modifier.fillMaxWidth().testTag("reg_studio_otp_input"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                if (otpError != null) {
                                    Text(otpError!!, color = SleekRed, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                                }

                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        if (verCodeInput == "949091") {
                                            viewModel.verifyEmailAndCreateStudio()
                                        } else {
                                            otpError = "Incorrect verification OTP. Enter '949091' to complete verification sandbox."
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("reg_studio_otp_submit_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Confirm Verification & Create Studio", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                            "AwaitApproval" -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Default.HourglassEmpty, contentDescription = "Pending", tint = SleekAmber, modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("REGISTRATION CREATED SUCCESSFULLY!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Studio entity '$rName' (Storage provider: $regStorageProvider, Phone: $regPhoneInput) is awaiting platform administrator verification.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "FAST TRACK STEP: Change active role to 'Super Admin' in the top bar to approve this registration immediately!",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekAmber,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SECURE PHONE OTP LOGIN MODULE Card
        item {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("otp_login_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SleekPrimary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECURE PHONE OTP LOGIN",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = SleekPrimary,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .background(SleekPrimary.copy(0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("SUPABASE AUTH", color = SleekPrimary, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    var phoneInputText by remember { mutableStateOf("") }
                    var otpCodeInputText by remember { mutableStateOf("") }
                    var otpFormError by remember { mutableStateOf<String?>(null) }

                    when (phoneAuthStep) {
                        "PhoneInput" -> {
                            Text("Fast-track login using WhatsApp/SMS verification code.", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = phoneInputText,
                                onValueChange = { phoneInputText = it },
                                label = { Text("Phone Number") },
                                placeholder = { Text("e.g. +91 94909 64015") },
                                modifier = Modifier.fillMaxWidth().testTag("otp_phone_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = SleekPrimary
                                )
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    if (phoneInputText.length < 10) {
                                        otpFormError = "Please enter a valid phone number with country code."
                                    } else {
                                        otpFormError = null
                                        viewModel.sendSmsOtp(phoneInputText)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("send_otp_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Dispatch SMS Verification Token", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            if (otpFormError != null) {
                                Text(otpFormError!!, color = SleekRed, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                        "VerifyCode" -> {
                            val sentPhone by viewModel.smsSentPhoneNumber.collectAsStateWithLifecycle()
                            Text("SMS verification code dispatched to $sentPhone.", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(4.dp))
                            
                            // High contrast simulator guide card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SleekAmber.copy(0.12f), RoundedCornerShape(6.dp))
                                    .border(0.5.dp, SleekAmber, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("SANDBOX SIMULATOR TOKEN INJECTOR", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = SleekAmber)
                                    Text("Enter simulated OTP '420888' to bypass Twilio carrier gateways.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = otpCodeInputText,
                                onValueChange = { otpCodeInputText = it },
                                label = { Text("6-Digit Verification OTP") },
                                placeholder = { Text("420888") },
                                modifier = Modifier.fillMaxWidth().testTag("otp_token_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = SleekPrimary
                                )
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.resetPhoneAuth() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Reset", fontSize = 11.sp)
                                }
                                Button(
                                    onClick = {
                                        if (otpCodeInputText == "420888") {
                                            otpFormError = null
                                            viewModel.verifySmsOtpAndLogIn(otpCodeInputText)
                                        } else {
                                            otpFormError = "Incorrect code. Please enter '420888'."
                                        }
                                    },
                                    modifier = Modifier.weight(2f).testTag("verify_otp_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Verify Token", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            if (otpFormError != null) {
                                Text(otpFormError!!, color = SleekRed, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                        "LoggedIn" -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "", tint = SleekEmerald, modifier = Modifier.size(36.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("SECURE AUTHENTICATION ESTABLISHED", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(Modifier.height(4.dp))
                                Text("You are fully logged in securely.", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        viewModel.switchRole("Studio Admin")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Launch Studio Control Center", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. FEATURE BENCHMARKS
        item {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text("THE SNAPVEX BLUEPRINT", fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                Spacer(Modifier.height(16.dp))

                LandingFeatureRow(
                    icon = Icons.Default.CloudSync,
                    title = "Hybrid Dual-Storage Pipelines",
                    desc = "Host RAW databases, Lightroom catalogs, drone clips on platform servers, Google Drive API, or OneDrive. Client galleries live forever."
                )
                LandingFeatureRow(
                    icon = Icons.Default.FolderSpecial,
                    title = "Automated Storage Scaffold",
                    desc = "No manual folder nesting. One-click event schedule creates standard RAW, Edited, Album Designs, Delivery structure instantly on the cloud."
                )
                LandingFeatureRow(
                    icon = Icons.Default.Psychology,
                    title = "Candid Face Recognition AI",
                    desc = "Smart tag blur scans, detects duplicates, labels bridal features, indexes vector portraits from connected storage metadata."
                )
            }
        }

        // 3. PRICING PIPELINE
        item {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text("PRICING MODELS", fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
                Spacer(Modifier.height(16.dp))

                 Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("LAUNCH OFFER PLAN", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            Box(
                                modifier = Modifier
                                    .background(SleekEmeraldLight, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("ACTIVE NOW", color = SleekEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("₹0/Month (Regular ₹500)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SleekEmerald)
                        Spacer(Modifier.height(8.dp))
                        Text("• 1 Active Studio Workspace\n• 2 Team Members\n• Core CRM & Event Scheduler\n• Interactive Client Galleries\n• 5 Active Events\n• 50GB Hybrid Cloud Storage Support", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SleekAmber)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PRO CREATIVE SUITE", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            Box(
                                modifier = Modifier
                                    .background(SleekAmberLight, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("PREMIUM", color = SleekAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("₹1,999/Month", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = SleekAmber)
                        Spacer(Modifier.height(8.dp))
                        Text("• UNLIMITED Active Events\n• UNLIMITED Client Portals & Leads\n• AI Face Recognition & Autotag Scanning\n• Advanced analytics panels\n• 100GB Included Hybrid Storage Block", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        // 4. ENTERPRISE LIMITS
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = SleekAmberLight),
                border = BorderStroke(1.dp, SleekAmber.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NEED MORE STORAGE?", fontWeight = FontWeight.Bold, color = SleekAmber, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Upgrade from 500GB up to 100TB+ custom secure servers.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Contact Team via WhatsApp: +91 94909 64015",
                        fontWeight = FontWeight.Bold,
                        color = SleekEmerald,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            // Custom call simulation trigger
                        }
                    )
                }
            }
        }

        // 5. TESTIMONIALS
        item {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text("REPRESENTING PARTNERS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF90A4AE))
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Imperial Royal Udaipur", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF37474F))
                    Text("Mumbai Fashion Runways", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF37474F))
                    Text("Tech Bangalore Elite", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF37474F))
                }
            }
        }
    }
}

@Composable
fun LandingFeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = "", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

// ========================================================
// SCREEN 2: SUPER ADMIN DASHBOARD
// ========================================================

@Composable
fun SuperAdminDashboard(
    studios: List<StudioEntity>,
    tickets: List<SupportTicketEntity>,
    notifications: List<NotificationEntity>,
    onApproveStudio: (Int) -> Unit,
    onApproveDrive: (Int) -> Unit,
    onGrantStorage: (Int, Double) -> Unit
) {
    var activeTab by remember { mutableStateOf("Studios") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Stats telemetry summary widgets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminTelemetryCard("Total Studios", "${studios.size}", "Active clusters", Color(0xFF00E676))
            AdminTelemetryCard("Total Storage", "${String.format("%.1f", studios.sumOf { it.storageUsedMb } / 1024.0)} GB", "Aggregated limits", Color(0xFFFFB300))
            AdminTelemetryCard("Subscription Income", "₹3,998/mo", "Pro and Sandbox clusters", Color(0xFF00E676))
            AdminTelemetryCard("Active Support Tickets", "${tickets.filter { it.status == "Open" }.size}", "Priority open issues", Color.Red)
        }

        Spacer(Modifier.height(16.dp))

        // Tabs
        Row(modifier = Modifier.fillMaxWidth()) {
            TabButton("Studios", activeTab == "Studios") { activeTab = "Studios" }
            Spacer(Modifier.width(8.dp))
            TabButton("Support Tickets", activeTab == "Tickets") { activeTab = "Tickets" }
        }

        Spacer(Modifier.height(12.dp))

        if (activeTab == "Studios") {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(studios) { s ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(s.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Plan Type: ${s.planType}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (s.status == "Active") SleekEmeraldLight else SleekAmberLight,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        s.status.uppercase(Locale.getDefault()),
                                        color = if (s.status == "Active") SleekEmerald else SleekAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Storage display
                            val usedGb = s.storageUsedMb / 1024.0
                            val limGb = s.storageLimitMb / 1024.0
                            val pct = (s.storageUsedMb / s.storageLimitMb) * 100
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Storage Used:", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                Text("${String.format("%.2f", usedGb)} GB of ${String.format("%.1f", limGb)} GB (${String.format("%.1f", pct)}%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }

                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = (s.storageUsedMb / s.storageLimitMb).toFloat().coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth(),
                                color = if (pct > 90) SleekRed else SleekEmerald,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(Modifier.height(8.dp))

                            // Quick Admin approvals inside sandbox controller
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (s.status == "Pending") {
                                    Button(
                                        onClick = { onApproveStudio(s.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).testTag("approve_studio_${s.id}")
                                    ) {
                                        Text("Approve Registration", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (!s.isDriveApproved) {
                                    Button(
                                        onClick = { onApproveDrive(s.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SleekAmber),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Grant Drive API request", fontSize = 11.sp, color = Color.White)
                                    }
                                }

                                Button(
                                    onClick = { onGrantStorage(s.id, 10240.0) }, // Add 10GB
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("+10GB Space grant", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondary)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tickets) { t ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(t.subject, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (t.priority == "High") SleekRedLight else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(t.priority, color = if (t.priority == "High") SleekRed else MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Raised by: ${t.studioName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(t.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("Raised on: ${t.dateString}", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
                                Text("Click to trigger WhatsApp call: +91 94909 64015", fontSize = 10.sp, color = SleekEmerald, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminTelemetryCard(title: String, valStr: String, desc: String, tint: Color) {
    Card(
        modifier = Modifier
            .width(130.dp)
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(valStr, fontSize = 15.sp, fontWeight = FontWeight.Black, color = if (tint == Color(0xFF00E676)) SleekEmerald else if (tint == Color(0xFFFFB300)) SleekAmber else if (tint == Color.Red) SleekRed else tint)
            Spacer(Modifier.height(4.dp))
            Text(desc, fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ========================================================
// SCREEN 3: STUDIO ADMIN DASHBOARD
// ========================================================

@Composable
fun StudioAdminDashboard(
    studio: StudioEntity?,
    leads: List<LeadEntity>,
    events: List<EventEntity>,
    quotations: List<QuotationEntity>,
    payments: List<PaymentEntity>,
    activityLogs: List<ActivityLogItem> = emptyList(),
    onAddLeadClick: () -> Unit,
    onAddEventClick: () -> Unit,
    onDeleteLead: (Int) -> Unit,
    onDeleteEvent: (Int) -> Unit,
    onUpdateLeadStatus: (Int, String) -> Unit,
    onConnectStorage: (Int, Boolean, String) -> Unit,
    onRequestDriveApproval: (Int) -> Unit,
    onExportQuotation: (Int, String) -> Unit,
    onLogPaymentClick: (PaymentEntity) -> Unit,
    onLogActivityItem: (String, String, String, String, Int) -> Unit = { _, _, _, _, _ -> }
) {
    var dashTab by remember { mutableStateOf("Leads CRM") }

    // Dynamic Theme Provider state variables (custom dynamic context)
    var activeOrangeColor by remember { mutableStateOf(SleekPrimary) }
    
    // Grid/View state modes
    var leadViewMode by remember { mutableStateOf("Kanban") } // "Kanban" or "List"
    var eventViewMode by remember { mutableStateOf("Calendar") } // "Calendar" or "List"

    // Custom calendar navigation context
    var calendarMonth by remember { mutableStateOf(6) } // Default: June
    var calendarYear by remember { mutableStateOf(2026) } // Default: 2026
    var calendarSelectedDay by remember { mutableStateOf(16) } // Default: 16th June 2026

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        if (studio == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Verify studio status registration pipeline")
            }
            return
        }

        // ShadCN-inspired Custom High-Level Dashboard Metrics Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("shadcn_metrics_panel"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "STUDIO PORTAL TELEMETRY (METRICS MONITOR)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekAmber,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Metric 1: CRM Active Funnel
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text("ACTIVE LEADS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Text("${leads.count { it.status != "Lost" }} Active", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        val totalPipeline = leads.map { it.value }.sum()
                        Text("₹${String.format("%.0f", totalPipeline)} Value", fontSize = 9.sp, color = SleekEmerald, fontWeight = FontWeight.Bold)
                    }

                    // Metric 2: Scheduled Event Sessions
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text("EVENT SCHED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Text("${events.size} Active", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        val paidOff = payments.count { it.pendingAmount <= 0.0 }
                        Text("$paidOff Terminated", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }

                    // Metric 3: Storage & API Bandwidth limits
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text("HYBRID CLOUD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Text("${String.format("%.1f", studio.storageUsedMb)} MB", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        val pct = if (studio.storageLimitMb > 0) (studio.storageUsedMb / studio.storageLimitMb * 100) else 0.0
                        Text("${String.format("%.1f", pct)}% Capacity", fontSize = 8.sp, color = if (pct > 80) SleekRed else SleekAmber, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // THEME PROVIDER CONTEXT CONTROLLER
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("theme_provider_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, activeOrangeColor.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(activeOrangeColor, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "THEME PROVIDER CONTEXT (Reactive Orange Palette)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "Propagates dynamic white-and-orange accents across dashboard widgets.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                // Color choices selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val palettes = listOf(
                        Triple("Amber Glow", Color(0xFFFF9F0A), "Amber Glow"),
                        Triple("Brand Sleek", Color(0xFFFF6B00), "Brand Sleek"),
                        Triple("Flame Red", Color(0xFFFF2900), "Flame Red")
                    )
                    palettes.forEach { (name, color, tag) ->
                        val isSelected = activeOrangeColor == color
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { 
                                    activeOrangeColor = color 
                                    onLogActivityItem("Studio Admin", "Switched Theme Pipeline Context to palette: $name", "System Notification", "Global", -1)
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.split(" ")[1],
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) color else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // Subheader navigation tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton("Leads CRM", dashTab == "Leads CRM") { dashTab = "Leads CRM" }
            TabButton("Event Schedules", dashTab == "Event Schedules") { dashTab = "Event Schedules" }
            TabButton("Invoicing & Payments", dashTab == "Invoicing") { dashTab = "Invoicing" }
            TabButton("Integrations & Storage API", dashTab == "Storage") { dashTab = "Storage" }
            TabButton("Analytics & Trends", dashTab == "Analytics") { dashTab = "Analytics" }
            TabButton("Activity Log", dashTab == "ActivityLog") { dashTab = "ActivityLog" }
        }

        Spacer(Modifier.height(16.dp))

        when (dashTab) {
            "Leads CRM" -> {
                Column(Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LEADS SALES FUNNEL PIPELINE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Button(
                            onClick = onAddLeadClick,
                            colors = ButtonDefaults.buttonColors(containerColor = activeOrangeColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("add_lead_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "", tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("New Lead", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Segmented toggle: Kanban vs List
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (leadViewMode == "Kanban") MaterialTheme.colorScheme.surface else Color.Transparent, RoundedCornerShape(6.dp))
                                .border(0.5.dp, if (leadViewMode == "Kanban") activeOrangeColor.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable { leadViewMode = "Kanban" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Menu, contentDescription = "", tint = if (leadViewMode == "Kanban") activeOrangeColor else MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Kanban CRM Board", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (leadViewMode == "Kanban") activeOrangeColor else MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (leadViewMode == "List") MaterialTheme.colorScheme.surface else Color.Transparent, RoundedCornerShape(6.dp))
                                .border(0.5.dp, if (leadViewMode == "List") activeOrangeColor.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable { leadViewMode = "List" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.List, contentDescription = "", tint = if (leadViewMode == "List") activeOrangeColor else MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Traditional List", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (leadViewMode == "List") activeOrangeColor else MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (leads.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No leads captured. Tap 'New Lead' to begin.", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else if (leadViewMode == "List") {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(leads) { lead ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text(lead.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                                Text("Originated: ${lead.source} • ${lead.dateString}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                            }
                                            Text(
                                                "₹${lead.value}",
                                                fontWeight = FontWeight.Bold,
                                                color = SleekEmerald,
                                                fontSize = 14.sp
                                            )
                                        }

                                        Spacer(Modifier.height(6.dp))
                                        Text(lead.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                        Spacer(Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Pipeline dropdown status selection
                                            var expanded by remember { mutableStateOf(false) }
                                            Box {
                                                Button(
                                                    onClick = { expanded = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("${lead.status} ▾", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                                }
                                                DropdownMenu(
                                                    expanded = expanded,
                                                    onDismissRequest = { expanded = false },
                                                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                                ) {
                                                    val statuses = listOf("New Lead", "Follow Up", "Interested", "Quotation Sent", "Booked", "Lost")
                                                    statuses.forEach { s ->
                                                        DropdownMenuItem(
                                                            text = { Text(s, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) },
                                                            onClick = {
                                                                onUpdateLeadStatus(lead.id, s)
                                                                expanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = { onDeleteLead(lead.id) },
                                                    modifier = Modifier.size(28.dp).testTag("delete_lead_btn_${lead.id}")
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "", tint = SleekRed, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // KANBAN BOARD SYSTEM WITH 3 SWIMLANES (Columns): New, Interested, Booked
                        val columnNew = leads.filter { it.status == "New Lead" || it.status == "Follow Up" }
                        val columnInterested = leads.filter { it.status == "Interested" || it.status == "Quotation Sent" }
                        val columnBooked = leads.filter { it.status == "Booked" || it.status == "Lost" }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Column 1: NEW STAGE
                            KanbanColumn(
                                title = "NEW LEADS",
                                count = columnNew.size,
                                items = columnNew,
                                accentColor = Color(0xFF64748B),
                                onUpdateStatus = { id, s -> onUpdateLeadStatus(id, s) },
                                onDelete = onDeleteLead,
                                canMoveLeft = false,
                                canMoveRight = true,
                                nextStatus = "Interested",
                                prevStatus = "",
                                activeColor = activeOrangeColor
                            )

                            // Column 2: INTERESTED STAGE
                            KanbanColumn(
                                title = "INTERESTED",
                                count = columnInterested.size,
                                items = columnInterested,
                                accentColor = SleekAmber,
                                onUpdateStatus = { id, s -> onUpdateLeadStatus(id, s) },
                                onDelete = onDeleteLead,
                                canMoveLeft = true,
                                canMoveRight = true,
                                nextStatus = "Booked",
                                prevStatus = "New Lead",
                                activeColor = activeOrangeColor
                            )

                            // Column 3: BOOKED STAGE
                            KanbanColumn(
                                title = "BOOKED CONVERSIONS",
                                count = columnBooked.size,
                                items = columnBooked,
                                accentColor = SleekEmerald,
                                onUpdateStatus = { id, s -> onUpdateLeadStatus(id, s) },
                                onDelete = onDeleteLead,
                                canMoveLeft = true,
                                canMoveRight = false,
                                nextStatus = "",
                                prevStatus = "Interested",
                                activeColor = activeOrangeColor
                            )
                        }
                    }
                }
            }

            "Event Schedules" -> {
                Column(Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("STUDIO ASSIGNED EVENT PIPELINE", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                        Button(
                            onClick = onAddEventClick,
                            colors = ButtonDefaults.buttonColors(containerColor = activeOrangeColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("add_event_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                            Text("Schedule Event", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Segmented toggle: Calendar vs List
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (eventViewMode == "Calendar") MaterialTheme.colorScheme.surface else Color.Transparent, RoundedCornerShape(6.dp))
                                .border(0.5.dp, if (eventViewMode == "Calendar") activeOrangeColor.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable { eventViewMode = "Calendar" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, contentDescription = "", tint = if (eventViewMode == "Calendar") activeOrangeColor else MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Interactive Calendar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (eventViewMode == "Calendar") activeOrangeColor else MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (eventViewMode == "List") MaterialTheme.colorScheme.surface else Color.Transparent, RoundedCornerShape(6.dp))
                                .border(0.5.dp, if (eventViewMode == "List") activeOrangeColor.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable { eventViewMode = "List" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.List, contentDescription = "", tint = if (eventViewMode == "List") activeOrangeColor else MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("All Events List", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (eventViewMode == "List") activeOrangeColor else MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (events.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No events scheduled. Tap 'Schedule Event'.", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else if (eventViewMode == "List") {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(events) { ev ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text(ev.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                                Text("Date: ${ev.dateString} • Client: ${ev.clientName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(SleekAmberLight, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(ev.status, color = SleekAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))
                                        Text("📌 Venue: ${ev.location}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                        Text("👥 Assigned Team: ${ev.assignedTeam}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                        Text("💾 Connected Cloud: ${ev.storageProvider}", fontSize = 12.sp, color = SleekEmerald, fontWeight = FontWeight.Bold)

                                        Spacer(Modifier.height(12.dp))
                                        Text("AUTOMATED DIRECTORY SCAFFOLD DIRECTORY TREE (Cloud Synced):", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SleekAmber)
                                        Spacer(Modifier.height(4.dp))
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                                .padding(6.dp)
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf("RAW Photos", "RAW Videos", "Edited Photos", "Edited Videos", "Album Design", "Client Selection", "Delivery").forEach { folder ->
                                                Row(
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Folder, contentDescription = "", tint = activeOrangeColor, modifier = Modifier.size(12.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(folder, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(6.dp))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            IconButton(
                                                onClick = { onDeleteEvent(ev.id) },
                                                modifier = Modifier.size(24.dp).testTag("delete_event_btn_${ev.id}")
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "", tint = SleekRed, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // INTERACTIVE CALENDAR BOARD ENGINE
                        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            // Month Title Navigator
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        if (calendarMonth == 1) {
                                            calendarMonth = 12
                                            calendarYear--
                                        } else {
                                            calendarMonth--
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "", tint = activeOrangeColor, modifier = Modifier.size(14.dp))
                                }

                                val monthName = when (calendarMonth) {
                                    1 -> "JANUARY"
                                    2 -> "FEBRUARY"
                                    3 -> "MARCH"
                                    4 -> "APRIL"
                                    5 -> "MAY"
                                    6 -> "JUNE"
                                    7 -> "JULY"
                                    8 -> "AUGUST"
                                    9 -> "SEPTEMBER"
                                    10 -> "OCTOBER"
                                    11 -> "NOVEMBER"
                                    12 -> "DECEMBER"
                                    else -> "JUNE"
                                }
                                Text(
                                    text = "$monthName $calendarYear",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 1.sp
                                )

                                IconButton(
                                    onClick = {
                                        if (calendarMonth == 12) {
                                            calendarMonth = 1
                                            calendarYear++
                                        } else {
                                            calendarMonth++
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "", tint = activeOrangeColor, modifier = Modifier.size(14.dp))
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Weeks Header S M T W T F S
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { dayName ->
                                    Text(
                                        text = dayName,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                        modifier = Modifier.width(36.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // Calendar Grid Helper Details
                            val calendarDetails = getMonthDaysDetails(calendarMonth, calendarYear)
                            val daysInMonth = calendarDetails.first
                            val startDayIndex = calendarDetails.second // 1=Sun, 2=Mon... 7=Sat

                            // Grid drawing
                            val totalCells = daysInMonth + (startDayIndex - 1)
                            val totalRows = (totalCells + 6) / 7

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (row in 0 until totalRows) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        for (col in 0..6) {
                                            val cellIndex = row * 7 + col + 1
                                            val day = cellIndex - (startDayIndex - 1)

                                            if (day in 1..daysInMonth) {
                                                val isSelected = calendarSelectedDay == day
                                                val monthStr = String.format("%02d", calendarMonth)
                                                val dayStr = String.format("%02d", day)
                                                val targetDate = "$calendarYear-$monthStr-$dayStr"

                                                // Find events and due payments
                                                val dayEvents = events.filter { it.dateString == targetDate }
                                                val dayPayments = payments.filter { it.dueDateString == targetDate }
                                                val hasEvent = dayEvents.isNotEmpty()
                                                val hasPayment = dayPayments.isNotEmpty()

                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(
                                                            if (isSelected) activeOrangeColor else Color.Transparent,
                                                            CircleShape
                                                        )
                                                        .border(
                                                            width = 0.5.dp,
                                                            color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                                            shape = CircleShape
                                                        )
                                                        .clickable { calendarSelectedDay = day }
                                                        .padding(2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = "$day",
                                                            fontSize = 10.sp,
                                                            fontWeight = if (isSelected || hasEvent) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        // Dots
                                                        if (hasEvent || hasPayment) {
                                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                                if (hasEvent) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(4.dp)
                                                                            .background(if (isSelected) Color.White else activeOrangeColor, CircleShape)
                                                                    )
                                                                }
                                                                if (hasPayment) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(4.dp)
                                                                            .background(if (isSelected) Color.White else SleekEmerald, CircleShape)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                // Empty box for preceding/trailing offsets
                                                Box(modifier = Modifier.size(36.dp))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Selected day schedule list
                            Text(
                                text = "SCHEDULE ON: ${String.format("%02d", calendarMonth)}/${String.format("%02d", calendarSelectedDay)}/$calendarYear",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(6.dp))

                            val monthStr = String.format("%02d", calendarMonth)
                            val dayStr = String.format("%02d", calendarSelectedDay)
                            val activeDayString = "$calendarYear-$monthStr-$dayStr"
                            val dayEventsList = events.filter { it.dateString == activeDayString }
                            val dayPaymentsList = payments.filter { it.dueDateString == activeDayString }

                            if (dayEventsList.isEmpty() && dayPaymentsList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.DateRange, contentDescription = "", tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.height(4.dp))
                                        Text("No scheduled events or payment deadlines.", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(dayEventsList) { ev ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, activeOrangeColor.copy(alpha = 0.3f))
                                        ) {
                                            Column(Modifier.padding(10.dp)) {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Folder, contentDescription = "", tint = activeOrangeColor, modifier = Modifier.size(14.dp))
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(ev.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .background(SleekAmberLight, RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(ev.status, color = SleekAmber, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Text("📍 Venue: ${ev.location} • Lead Client: ${ev.clientName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                                Text("👥 Team assigned: ${ev.assignedTeam}", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                    }

                                    items(dayPaymentsList) { p ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, SleekEmerald.copy(alpha = 0.3f))
                                        ) {
                                            Column(Modifier.padding(10.dp)) {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("💰", fontSize = 12.sp)
                                                        Spacer(Modifier.width(6.dp))
                                                        Text("Payment Deadline: ${p.clientName}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .background(SleekRedLight, RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(p.status, color = SleekRed, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Text("Amount Balance Pending: ₹${p.pendingAmount} of ₹${p.totalAmount} total package price.", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "Invoicing" -> {
                Column(Modifier.weight(1f)) {
                    Text("CLIENTS PAYMENT DISPATCH", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(payments) { p ->
                            val quota = quotations.find { it.eventId == p.eventId }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text(p.clientName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text("Due Balance: ₹${p.pendingAmount} of Total ₹${p.totalAmount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    when(p.status) {
                                                        "Paid" -> SleekEmeraldLight
                                                        "Partial" -> SleekAmberLight
                                                        else -> SleekRedLight
                                                    },
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                p.status,
                                                color = when(p.status) {
                                                    "Paid" -> SleekEmerald
                                                    "Partial" -> SleekAmber
                                                    else -> SleekRed
                                                 },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))
                                    Text("📅 Limit Date: ${p.dueDateString}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

                                    Spacer(Modifier.height(10.dp))
                                    Text("PIPELINE EXPORT ACTIONS:", fontSize = 10.sp, color = SleekAmber, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (quota != null) {
                                            QuotaExportPill("PDF", quota.pdfExported) { onExportQuotation(quota.id, "PDF") }
                                            QuotaExportPill("WhatsApp", quota.whatsappSent) { onExportQuotation(quota.id, "WhatsApp") }
                                            QuotaExportPill("Email", quota.emailSent) { onExportQuotation(quota.id, "Email") }
                                        }

                                        Spacer(Modifier.weight(1f))

                                        if (p.status != "Paid") {
                                            Button(
                                                onClick = { onLogPaymentClick(p) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Log Payment", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "Storage" -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("API COUPLING GATEWAY CONNECTORS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("All photo outputs are seamlessly mapped onto background server providers automatically. Local folders are synchronized dynamically.", fontSize = 12.sp, color = Color(0xFF90A4AE))

                    // PLATFORM CLOUD STORAGE BLOCK (Always Connected)
                    StorageProviderCard(
                        providerName = "SnapVex Platform Cloud Storage",
                        isEnabled = true,
                        isApproved = true,
                        logo = Icons.Default.Cloud,
                        actionText = "Active Base Storage",
                        onAction = { }
                    )

                    // GOOGLE DRIVE CLOUD BLOCK (Connected only if approved by platforms administrator)
                    StorageProviderCard(
                        providerName = "Google Drive API Suite",
                        isEnabled = studio.isDriveConnected,
                        isApproved = studio.isDriveApproved,
                        logo = Icons.Default.CloudQueue,
                        actionText = if (!studio.isDriveApproved) "Request Administrator Verification" else if (studio.isDriveConnected) "Disconnect Google Drive API" else "Connect Google Drive API App",
                        onAction = {
                            if (!studio.isDriveApproved) {
                                onRequestDriveApproval(studio.id)
                            } else {
                                onConnectStorage(studio.id, !studio.isDriveConnected, "Google Drive")
                            }
                        }
                    )

                    // MICROSOFT ONEDRIVE CLOUD BLOCK
                    StorageProviderCard(
                        providerName = "Microsoft Graph OneDrive API",
                        isEnabled = studio.isOneDriveConnected,
                        isApproved = true, // Approved by default for standard testing
                        logo = Icons.Default.CloudUpload,
                        actionText = if (studio.isOneDriveConnected) "Disconnect OneDrive link" else "Connect OneDrive App Link",
                        onAction = {
                            onConnectStorage(studio.id, !studio.isOneDriveConnected, "OneDrive")
                        }
                    )
                }
            }

            "Analytics" -> {
                // Renders beautifully scaled native Canvas diagrams and trend charts
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("STORESTREAM ANALYTICS (HYBRID STORAGE INDEX)", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("Live metrics monitoring physical limits and sync distribution patterns in realtime.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("storage_chart_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("STORAGE DISTRIBUTION BY PROVIDER SERVICE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SleekPrimary)
                            Spacer(Modifier.height(12.dp))

                            // Provider 1: SnapVex Platform
                            StorageBarChartItem("Base Platform Storage", 152.4, 500.0, SleekPrimary, "Active Sync Pipeline")
                            Spacer(Modifier.height(10.dp))

                            // Provider 2: Google Drive API
                            StorageBarChartItem("Google Drive API Suite", if (studio.isDriveConnected) 345.8 else 0.0, 1024.0, Color(0xFF4285F4), if (studio.isDriveConnected) "Connected" else "Disabled")
                            Spacer(Modifier.height(10.dp))

                            // Provider 3: Microsoft OneDrive Graph
                            StorageBarChartItem("Microsoft OneDrive GLink", if (studio.isOneDriveConnected) 248.5 else 0.0, 1024.0, Color(0xFF0078D4), if (studio.isOneDriveConnected) "Connected" else "Standby")
                        }
                    }

                    // Native Canvas Chart representing Monthly storage telemetry growth
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("MONTHLY TELEMETRY GROWTH PATTERN (MB)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SleekPrimary)
                            Text("Consolidated monthly ingestion rates synced securely", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(16.dp))

                            // Interactive Canvas Bar graph
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    val growthData = listOf(
                                        Pair("Jan", 120f),
                                        Pair("Feb", 180f),
                                        Pair("Mar", 290f),
                                        Pair("Apr", 410f),
                                        Pair("May", 580f),
                                        Pair("Jun", 746f)
                                    )
                                    val maxVal = 800f

                                    growthData.forEach { (month, mbs) ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Bottom,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("${mbs.toInt()}M", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                                            Spacer(Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .width(20.dp)
                                                    .fillMaxHeight(mbs / maxVal)
                                                    .background(SleekPrimary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(month, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "ActivityLog" -> {
                // Interactive centralized Activity Log component with filter by Event ID and filter by Role
                var selectedEventFilter by remember { mutableStateOf<Int?>(-1) } // -1 = All, null = Global, Event ID for others
                var selectedRoleFilter by remember { mutableStateOf("All") } // "All", "Studio Admin", ...

                Column(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("CENTRALIZED AUDIT & SYSTEM REVELATIONS FEED", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    Text("Secure log track monitoring cloud events, user assignments, and database triggers.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

                    // Interactive Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Event Filter Dropdown
                        var eventExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { eventExpanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                val label = when (selectedEventFilter) {
                                    -1 -> "All events"
                                    1 -> "Aisha & Kabir Wedding"
                                    else -> "Global Events"
                                }
                                Text("$label ▾", fontSize = 10.sp, color = SleekPrimary, fontWeight = FontWeight.Bold)
                            }
                            DropdownMenu(
                                expanded = eventExpanded,
                                onDismissRequest = { eventExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All events", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = { selectedEventFilter = -1; eventExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Aisha & Kabir Wedding", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = { selectedEventFilter = 1; eventExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Global (System-wide)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = { selectedEventFilter = null; eventExpanded = false }
                                )
                            }
                        }

                        // Role Filter Dropdown
                        var roleExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { roleExpanded = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("${selectedRoleFilter} ▾", fontSize = 10.sp, color = SleekPrimary, fontWeight = FontWeight.Bold)
                            }
                            DropdownMenu(
                                expanded = roleExpanded,
                                onDismissRequest = { roleExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                listOf("All", "Studio Admin", "Super Admin", "Photographer", "Editor", "System").forEach { role ->
                                    DropdownMenuItem(
                                        text = { Text(role, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = { selectedRoleFilter = role; roleExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    // List of filtered Logs
                    val filteredLogs = activityLogs.filter { log ->
                        val matchEvent = when (selectedEventFilter) {
                            -1 -> true
                            null -> log.eventId == -1
                            else -> log.eventId == selectedEventFilter
                        }
                        val matchRole = selectedRoleFilter == "All" || log.role == selectedRoleFilter
                        matchEvent && matchRole
                    }

                    if (filteredLogs.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No audit log matches selected state filters.", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredLogs) { log ->
                                val categoryColor = when (log.type) {
                                    "User Action" -> SleekPrimary
                                    "Storage Event" -> SleekEmerald
                                    else -> SleekAmber
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(categoryColor, CircleShape)
                                                )
                                                Text(
                                                    text = log.role.uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    color = categoryColor
                                                )
                                            }

                                            // Formatted time relative
                                            Text(
                                                text = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date(log.timestamp)),
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }

                                        Spacer(Modifier.height(4.dp))
                                        
                                        Text(
                                            text = log.action,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Event Context: ${log.eventName}",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .background(categoryColor.copy(0.12f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = log.type,
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = categoryColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Simulation injection buttons
                    Button(
                        onClick = {
                            onLogActivityItem("Photographer", "Uploaded custom high-speed RAW wedding frames", "Storage Event", "Aisha & Kabir Wedding", 1)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth().height(36.dp).testTag("simulate_logs_btn")
                    ) {
                        Text("Simulate Live Photo Ingest Log Trigger", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StorageBarChartItem(
    title: String,
    usedMb: Double,
    limitMb: Double,
    color: Color,
    statusText: String
) {
    val fraction = if (limitMb > 0f) (usedMb / limitMb).toFloat() else 0f
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("${String.format("%.1f", usedMb)} MB of ${String.format("%.0f", limitMb)} MB ($statusText)", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .background(color, CircleShape)
            )
        }
    }
}

@Composable
fun StorageProviderCard(
    providerName: String,
    isEnabled: Boolean,
    isApproved: Boolean,
    logo: androidx.compose.ui.graphics.vector.ImageVector,
    actionText: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isEnabled) SleekEmerald else MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(logo, contentDescription = "", tint = if (isEnabled) SleekEmerald else MaterialTheme.colorScheme.onSurface.copy(0.4f), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(providerName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (isEnabled && isApproved) SleekEmeraldLight else SleekRedLight,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (!isApproved) "VERIFICATION REQUIRED" else if (isEnabled) "CONNECTED" else "STANDBY DISPATCH",
                        color = if (isEnabled && isApproved) SleekEmerald else SleekRed,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Instructions on pending approval
            if (!isApproved) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Google Drive Sync permissions details are currently hidden from staff members until platform administrators verify connections telemetry. Turn on Super Admin on top role pills and click approve drive request to preview immediately.",
                    fontSize = 11.sp,
                    color = SleekAmber
                )
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("toggle_connect_${providerName.replace(" ", "_")}")
            ) {
                Text(
                    actionText,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun QuotaExportPill(label: String, isExported: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, fontSize = 10.sp) },
        leadingIcon = {
            Icon(
                imageVector = if (isExported) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                contentDescription = "",
                tint = if (isExported) SleekEmerald else Color.Gray,
                modifier = Modifier.size(12.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            labelColor = if (isExported) SleekEmerald else MaterialTheme.colorScheme.onSurface
        )
    )
}

// ========================================================
// SCREEN 4: PHOTOGRAPHER WORKSPACE
// ========================================================

@Composable
fun PhotographerWorkspace(
    events: List<EventEntity>,
    selectedEvent: EventEntity?,
    galleryItems: List<GalleryItemEntity>,
    isUploading: Boolean,
    uploadProgress: Int,
    onEventSelect: (Int) -> Unit,
    onUploadImageSimulate: (Int, String, String, Long) -> Unit,
    onInspectAI: (GalleryItemEntity) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text("CREW FIELD TERMINAL: PHOTOGRAPHERS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Select an assigned scheduled session shoot below to run media sync directly from field.", fontSize = 12.sp, color = Color(0xFF90A4AE))

        Spacer(Modifier.height(12.dp))

        // Event Selector Dropdown wrapper
        var evExpanded by remember { mutableStateOf(false) }
        Box {
            Button(
                onClick = { evExpanded = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(selectedEvent?.name ?: "Select Assigned Shoot Session ▾", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            DropdownMenu(
                expanded = evExpanded,
                onDismissRequest = { evExpanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface).fillMaxWidth()
            ) {
                events.forEach { e ->
                    DropdownMenuItem(
                        text = { Text(e.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) },
                        onClick = {
                            onEventSelect(e.id)
                            evExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (selectedEvent == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Please select or configure an assigned shoot session", color = MaterialTheme.colorScheme.secondary)
            }
            return
        }

        // Active Event Details & Camera simulator triggers
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("FIELD CAMERA SHUTTER FLUID SIMULATOR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekAmber)
                Text("Trigger simulated RAW photograph capturing to test direct pipeline autotagging AI scanning instantly.", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onUploadImageSimulate(selectedEvent.id, "bride_makeup_face_details.arw", "image/jpeg", 48123904L)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).testTag("shoot_photo_1"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Shutter: Bride Portrait", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onUploadImageSimulate(selectedEvent.id, "vows_blurry_motion.arw", "image/jpeg", 51203840L)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekAmber),
                        modifier = Modifier.weight(1f).testTag("shoot_photo_2"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Shutter: Dynamic Blur", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Progress indicators during background upload simulations
        if (isUploading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Auto Uploading RAW onto Platform Storage cloud via API...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = uploadProgress / 100f, color = SleekEmerald, modifier = Modifier.fillMaxWidth(), trackColor = MaterialTheme.colorScheme.surface)
                    Spacer(Modifier.height(4.dp))
                    Text("Transmited: $uploadProgress% (High-speed RAW packet tunnel)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Output Items
        Text("CURRENT SYNC GALLERY VIEW (${galleryItems.size} Files)", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(galleryItems) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column {
                        Box {
                            AsyncImage(
                                model = item.localUri,
                                contentDescription = "",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentScale = ContentScale.Crop
                            )

                            // AI Flag status indicators overlap
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (item.isBlur) {
                                    Badge(containerColor = SleekRed, contentColor = Color.White) { Text("BLUR", fontSize = 8.sp) }
                                }
                                if (item.isDuplicate) {
                                    Badge(containerColor = SleekAmber, contentColor = Color.White) { Text("DUP", fontSize = 8.sp) }
                                }
                                if (item.isBestShot) {
                                    Badge(containerColor = SleekEmerald, contentColor = Color.White) { Text("BEST", fontSize = 8.sp) }
                                }
                            }
                        }

                        Column(Modifier.padding(8.dp)) {
                            Text(item.fileName, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                            Text("Size: ${String.format("%.1f", item.sizeBytes / 1024.0 / 1024.0)} MB", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                            
                            Spacer(Modifier.height(4.dp))
                            AIMetadataGalleryBadgeDetails(item = item, onInspectClick = { onInspectAI(item) })
                        }
                    }
                }
            }
        }
    }
}

// ========================================================
// SCREEN 5: EDITOR WORKSPACE
// ========================================================

@Composable
fun EditorWorkspace(
    events: List<EventEntity>,
    selectedEvent: EventEntity?,
    galleryItems: List<GalleryItemEntity>,
    isUploading: Boolean,
    uploadProgress: Int,
    onEventSelect: (Int) -> Unit,
    onDownloadRaw: (String) -> Unit,
    onUploadEdited: (Int, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text("CREW RETROCESSION SUITE: EDITORS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Select an assigned shoot event session. Download high-packet RAW catalogs, and deliver final post-production artwork directly back.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

        Spacer(Modifier.height(12.dp))

        var selectionExpand by remember { mutableStateOf(false) }
        Box {
            Button(
                onClick = { selectionExpand = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(selectedEvent?.name ?: "Select Shoot Event ▾", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            DropdownMenu(
                expanded = selectionExpand,
                onDismissRequest = { selectionExpand = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface).fillMaxWidth()
            ) {
                events.forEach { e ->
                    DropdownMenuItem(
                        text = { Text(e.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) },
                        onClick = {
                            onEventSelect(e.id)
                            selectionExpand = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (selectedEvent == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Select an active studio event session", color = MaterialTheme.colorScheme.secondary)
            }
            return
        }

        if (isUploading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Downloading/Uploading media stream packets...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = uploadProgress / 100f, color = SleekEmerald, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Text("RAW DISPATCH LIST AVAILABLE FOR DIRECT DOWNLOAD", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekAmber)
        Spacer(Modifier.height(8.dp))

        val rawItems = galleryItems.filter { it.fileName.startsWith("RAW_") }
        if (rawItems.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No RAW formats submitted by photographer field team yet.", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rawItems) { raw ->
                    val counterpartName = raw.fileName.replace("RAW_", "EDITED_").replace(".arw", ".jpg")
                    val isAlreadyEdited = galleryItems.any { it.fileName == counterpartName }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(raw.fileName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Size: ${String.format("%.1f", raw.sizeBytes / 1024.0 / 1024.0)} MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                }

                                if (isAlreadyEdited) {
                                    Box(
                                        modifier = Modifier
                                            .background(SleekEmeraldLight, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("EDIT COMPLETED", color = SleekEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onDownloadRaw(raw.fileName) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.weight(1f).testTag("dl_raw_${raw.id}"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "", tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Download RAW", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onUploadEdited(selectedEvent.id, raw.fileName) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1f).testTag("upl_edited_${raw.id}"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Upload, contentDescription = "", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Upload Color JPG", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========================================================
// SCREEN 6: CLIENT GALLERY INTERACTIVE PORTAL
// ========================================================

@Composable
fun ClientGalleryScreen(
    events: List<EventEntity>,
    selectedEvent: EventEntity?,
    galleryItems: List<GalleryItemEntity>,
    onEventSelect: (Int) -> Unit,
    onFavoritePhoto: (Int, Boolean) -> Unit,
    onChooseForAlbum: (Int, Boolean) -> Unit,
    onSubmitAlbumDecision: (Int, Boolean, String) -> Unit,
    onInspectAI: (GalleryItemEntity) -> Unit
) {
    var feedbackText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text("CLIENT PORTAL REVELATION HUB", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Securely select custom favorites, organize layouts, and authorize final hardbound press album deliveries.", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

        Spacer(Modifier.height(12.dp))

        var selectionExpand by remember { mutableStateOf(false) }
        Box {
            Button(
                onClick = { selectionExpand = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(selectedEvent?.name ?: "Select Portfolio Event ▾", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            DropdownMenu(
                expanded = selectionExpand,
                onDismissRequest = { selectionExpand = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface).fillMaxWidth()
            ) {
                events.forEach { e ->
                    DropdownMenuItem(
                        text = { Text(e.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) },
                        onClick = {
                            onEventSelect(e.id)
                            selectionExpand = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (selectedEvent == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Select an active studio event session", color = MaterialTheme.colorScheme.secondary)
            }
            return
        }

        // Submissions checklist review drawer
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("APPROVAL PORTAL & ALBUM DECISION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SleekAmber)
                Text("Design review status: Pending. Choose favorites from below, tick 'Approve Design' button or leave annotations.", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    placeholder = { Text("Annotations text e.g. Swap photo 3 and 1...", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onSubmitAlbumDecision(selectedEvent.id, true, "")
                            feedbackText = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).testTag("client_approve_album"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Approve Design", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            if (feedbackText.isNotEmpty()) {
                                onSubmitAlbumDecision(selectedEvent.id, false, feedbackText)
                                feedbackText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekRed),
                        modifier = Modifier.weight(1f).testTag("client_request_changes"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Request Changes", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        val edItems = galleryItems.filter { !it.fileName.startsWith("RAW_") }
        Text("SHOWN WORKS (DELIVERED EDITED PHOTO REVELATIONS: ${edItems.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))

        if (edItems.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("All proofs currently undergoing coloring correctly and cropping. Stay tuned.", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(edItems) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column {
                            Box {
                                AsyncImage(
                                    model = item.localUri,
                                    contentDescription = "",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp),
                                    contentScale = ContentScale.Crop
                                )

                                // Heart selection overlap
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onFavoritePhoto(item.id, !item.isFavorite) },
                                        modifier = Modifier
                                            .background(Color.Black.copy(0.4f), CircleShape)
                                            .size(30.dp)
                                            .testTag("fav_btn_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "",
                                            tint = if (item.isFavorite) SleekRed else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Checkbox choosing for virtual physical physical albums
                                    CustomAlbumSelectCheckbox(
                                        isSelected = item.isApprovedForAlbum,
                                        onSelectedChange = { onChooseForAlbum(item.id, it) },
                                        testTagId = "chk_album_${item.id}"
                                    )
                                }
                            }

                            Column(Modifier.padding(8.dp)) {
                                Text(item.fileName, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                                Text("Artwork File proof", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                                
                                Spacer(Modifier.height(4.dp))
                                AIMetadataGalleryBadgeDetails(item = item, onInspectClick = { onInspectAI(item) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomAlbumSelectCheckbox(
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    testTagId: String
) {
    IconButton(
        onClick = { onSelectedChange(!isSelected) },
        modifier = Modifier
            .background(if (isSelected) Color(0xFF00E676) else Color.Black.copy(0.4f), CircleShape)
            .size(30.dp)
            .testTag(testTagId)
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Album,
            contentDescription = "",
            tint = if (isSelected) Color.Black else Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun AIMetadataGalleryBadgeDetails(
    item: GalleryItemEntity,
    onInspectClick: () -> Unit
) {
    val tags = remember(item.aiTags) {
        if (item.aiTags.isEmpty()) emptyList()
        else item.aiTags
            .replace("AI TAGS:", "", ignoreCase = true)
            .replace("AI:", "", ignoreCase = true)
            .split(",")
            .map { it.trim().uppercase(Locale.getDefault()) }
            .filter { it.isNotEmpty() }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        if (tags.isEmpty()) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("AI: QUEUED SCAN", fontSize = 8.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
            }
        } else {
            // Display facial recognition indicator if detected
            val hasFaces = item.faceCoordinates.isNotEmpty() || item.aiTags.contains("bride", ignoreCase = true) || item.aiTags.contains("couple", ignoreCase = true)
            val faceCount = if (item.aiTags.contains("couple", ignoreCase = true)) 2 else if (item.aiTags.contains("bride", ignoreCase = true)) 1 else 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasFaces) {
                    Box(
                        modifier = Modifier
                            .background(SleekAmberLight, RoundedCornerShape(4.dp))
                            .border(0.5.dp, SleekAmber.copy(0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Default.Face, contentDescription = null, tint = SleekAmber, modifier = Modifier.size(9.dp))
                            Text("👥 ${if (faceCount > 0) faceCount else 1} FACE", fontSize = 8.sp, color = SleekAmber, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Show Content categories tags count
                Box(
                    modifier = Modifier
                        .background(SleekPrimaryLight, RoundedCornerShape(4.dp))
                        .border(0.5.dp, SleekPrimary.copy(0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("🏷️ ${tags.size} CATEGORIES", fontSize = 8.sp, color = SleekPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.weight(1f))

                // Small info click indicator
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Inspect AI Report",
                    tint = SleekPrimary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onInspectClick() }
                        .testTag("ai_inspect_btn_${item.id}")
                )
            }

            Spacer(Modifier.height(4.dp))

            // Scrollable row of tags
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tags.take(3).forEach { t ->
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(t, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (tags.size > 3) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("+${tags.size - 3}", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun AIInspectReportDialog(
    item: GalleryItemEntity,
    onDismiss: () -> Unit
) {
    val tags = remember(item.aiTags) {
        if (item.aiTags.isEmpty()) emptyList()
        else item.aiTags
            .replace("AI TAGS:", "", ignoreCase = true)
            .replace("AI:", "", ignoreCase = true)
            .split(",")
            .map { it.trim().uppercase(Locale.getDefault()) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "", tint = SleekPrimary)
                Text("AI VISION METADATA REPORT", fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "File: ${item.fileName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Face coordinates layout mapping illustration
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("FACIAL RECOGNITION BOUNDING BOXES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(width = 240.dp, height = 120.dp)
                                .background(Color.Gray.copy(0.2f), RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Draw bounding boxes over localized space
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (item.aiTags.contains("bride", ignoreCase = true) || item.aiTags.contains("couple", ignoreCase = true)) {
                                    // Simulated faces circles or boxes
                                    drawRoundRect(
                                        color = SleekAmber,
                                        topLeft = Offset(40.dp.toPx(), 20.dp.toPx()),
                                        size = Size(60.dp.toPx(), 60.dp.toPx()),
                                        cornerRadius = CornerRadius(4.dp.toPx()),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                                if (item.aiTags.contains("couple", ignoreCase = true)) {
                                    // Face 2
                                    drawRoundRect(
                                        color = SleekAmber,
                                        topLeft = Offset(140.dp.toPx(), 25.dp.toPx()),
                                        size = Size(55.dp.toPx(), 55.dp.toPx()),
                                        cornerRadius = CornerRadius(4.dp.toPx()),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Face, contentDescription = "", tint = SleekAmber.copy(0.6f))
                                Text(
                                    text = if (item.aiTags.contains("couple", ignoreCase = true)) "2 FACES DETECTED" 
                                           else if (item.aiTags.contains("bride", ignoreCase = true)) "1 FACE DETECTED" 
                                           else "0 FACES DETECTED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (item.aiTags.contains("couple", ignoreCase = true)) "Subject 1: Confidence 98% | Subject 2: Confidence 95%" 
                                           else if (item.aiTags.contains("bride", ignoreCase = true)) "Subject: Confidence 99% (Focal Lock)" 
                                           else "Telemetry: standard resolution scan",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                // AI Tags classification chips
                Column {
                    Text("CONTENT CATEGORIES CLASSIFIED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(SleekPrimaryLight, RoundedCornerShape(6.dp))
                                    .border(1.dp, SleekPrimary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("# $tag", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                            }
                        }
                    }
                }

                // Asset Health Quality Scores
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("IMAGE VALUE & OPTIMIZATION INDEX", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QualityIndicatorCard("BLUR FOCUS", if (item.isBlur) "Detected" else "Pass (Good)", if (item.isBlur) SleekRed else SleekEmerald, Modifier.weight(1f))
                        QualityIndicatorCard("DUPLICATION", if (item.isDuplicate) "Repeated" else "Unique", if (item.isDuplicate) SleekAmber else SleekEmerald, Modifier.weight(1f))
                        QualityIndicatorCard("BEST RECOMMEND", if (item.isBestShot) "Highly Opt" else "Standard", if (item.isBestShot) SleekEmerald else MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
            ) {
                Text("Dismiss Report", color = Color.White)
            }
        }
    )
}

@Composable
fun QualityIndicatorCard(
    heading: String,
    score: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(heading, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            Text(score, fontSize = 9.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

// ========================================================
// SYSTEM DIALOGS MODALS
// ========================================================

@Composable
fun CreateLeadDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double, String, String, String) -> Unit
) {
    var nm by remember { mutableStateOf("") }
    var sc by remember { mutableStateOf("Instagram") }
    var st by remember { mutableStateOf("New Lead") }
    var vl by remember { mutableStateOf("") }
    var ph by remember { mutableStateOf("") }
    var em by remember { mutableStateOf("") }
    var no by remember { mutableStateOf("") }

    var leadSourceExpand by remember { mutableStateOf(false) }
    var leadStatusExpand by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log New CRM Prospect Lead", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nm,
                    onValueChange = { nm = it },
                    label = { Text("Lead / Couple Project Name") },
                    modifier = Modifier.fillMaxWidth().testTag("lead_dlg_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Lead Source
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { leadSourceExpand = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("$sc ▾", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = leadSourceExpand, onDismissRequest = { leadSourceExpand = false }) {
                            listOf("Website", "WhatsApp", "Instagram", "Facebook", "Manual").forEach { source ->
                                DropdownMenuItem(text = { Text(source, color = MaterialTheme.colorScheme.onSurface) }, onClick = { sc = source; leadSourceExpand = false })
                            }
                        }
                    }

                    // Lead Status
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { leadStatusExpand = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("$st ▾", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = leadStatusExpand, onDismissRequest = { leadStatusExpand = false }) {
                            listOf("New Lead", "Follow Up", "Interested", "Quotation Sent", "Booked", "Lost").forEach { status ->
                                DropdownMenuItem(text = { Text(status, color = MaterialTheme.colorScheme.onSurface) }, onClick = { st = status; leadStatusExpand = false })
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = vl,
                    onValueChange = { vl = it },
                    label = { Text("Value (₹)") },
                    modifier = Modifier.fillMaxWidth().testTag("lead_dlg_val"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = ph,
                    onValueChange = { ph = it },
                    label = { Text("WhatsApp Contact") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = em,
                    onValueChange = { em = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = no,
                    onValueChange = { no = it },
                    label = { Text("Details & Scope notes") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = vl.toDoubleOrNull() ?: 0.0
                    onConfirm(nm, sc, st, amount, ph, em, no)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("lead_dlg_save"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Back", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold) }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun CreateEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, Double, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var eventType by remember { mutableStateOf("Wedding") }
    var dateString by remember { mutableStateOf("2026-06-25") }
    var location by remember { mutableStateOf("") }
    var packagePrice by remember { mutableStateOf("") }
    var assignedTeam by remember { mutableStateOf("") }
    var storageProvider by remember { mutableStateOf("Platform Storage") }

    var typeExpand by remember { mutableStateOf(false) }
    var providerExpand by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Event Work Session", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Event Contract Name") },
                    modifier = Modifier.fillMaxWidth().testTag("ev_dlg_name"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Client/Couple Primary Contact") },
                    modifier = Modifier.fillMaxWidth().testTag("ev_dlg_client"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { typeExpand = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("$eventType ▾", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = typeExpand, onDismissRequest = { typeExpand = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                            listOf("Wedding", "Birthday", "Corporate", "Outdoor Shoot").forEach { type ->
                                DropdownMenuItem(text = { Text(type, color = MaterialTheme.colorScheme.onSurface) }, onClick = { eventType = type; typeExpand = false })
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        Button(
                            onClick = { providerExpand = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("$storageProvider ▾", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(expanded = providerExpand, onDismissRequest = { providerExpand = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                            listOf("Platform Storage", "Google Drive", "OneDrive").forEach { prov ->
                                DropdownMenuItem(text = { Text(prov, color = MaterialTheme.colorScheme.onSurface) }, onClick = { storageProvider = prov; providerExpand = false })
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Palace/Location Details") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = packagePrice,
                    onValueChange = { packagePrice = it },
                    label = { Text("Total Package (₹)") },
                    modifier = Modifier.fillMaxWidth().testTag("ev_dlg_price"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = assignedTeam,
                    onValueChange = { assignedTeam = it },
                    label = { Text("Crew assignments e.g. Vikram, Anita") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = packagePrice.toDoubleOrNull() ?: 0.0
                    onConfirm(name, clientName, eventType, dateString, location, price, assignedTeam, storageProvider)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("ev_dlg_save"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm & Create folders", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Back", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold) }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun ReceivePaymentDialog(
    payment: PaymentEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var amtInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Process Payment Balance Received", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Client: ${payment.clientName}", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Total package: ₹${payment.totalAmount} | Balance Pending: ₹${payment.pendingAmount}", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = amtInput,
                    onValueChange = { amtInput = it },
                    label = { Text("Received Amount (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(amtInput) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Log Transaction", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Back", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold) }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun NotificationDrawerOverlay(
    notificationsList: List<NotificationEntity>,
    onDismiss: () -> Unit,
    onMarkRead: (Int) -> Unit,
    onMarkAllRead: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Assignments", "Payments", "Storage"

    // Filter list based on trigger types
    val filteredList = notificationsList.filter { n ->
        when (selectedFilter) {
            "Assignments" -> n.triggers == "Event Assigned" || n.title.contains("Event", ignoreCase = true) || n.message.contains("assigned", ignoreCase = true)
            "Payments" -> n.triggers.contains("Payment", ignoreCase = true) || n.title.contains("payable", ignoreCase = true) || n.title.contains("Payment", ignoreCase = true) || n.message.contains("received", ignoreCase = true)
            "Storage" -> n.triggers.contains("Storage", ignoreCase = true) || n.triggers.contains("Upload", ignoreCase = true) || n.title.contains("LIMIT", ignoreCase = true) || n.title.contains("STORAGE", ignoreCase = true)
            else -> true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.4f))
            .clickable { onDismiss() }
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(320.dp)
                .clickable(enabled = false) { }
                .border(1.dp, MaterialTheme.colorScheme.outline),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NOTIFICATIONS HUB",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = SleekPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Realtime Supabase Event Piping",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Mark All Read trigger
                if (notificationsList.any { !it.isRead }) {
                    Button(
                        onClick = onMarkAllRead,
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimaryLight),
                        elevation = null,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth().height(32.dp).testTag("notif_mark_all_read")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DoneAll, contentDescription = "", tint = SleekPrimary, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Mark all as read", color = SleekPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Category Filter Pills
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("All", "Assignments", "Payments", "Storage").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) SleekPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(20.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // List
                if (filteredList.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "", tint = SleekEmerald.copy(0.5f), modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("All cleared!", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                            Text("No alerts in ${selectedFilter.lowercase()} stream.", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary.copy(0.7f))
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredList) { n ->
                            val isAssignment = n.triggers == "Event Assigned" || n.title.contains("Event", ignoreCase = true) || n.message.contains("assigned", ignoreCase = true)
                            val isPayment = n.triggers.contains("Payment", ignoreCase = true) || n.title.contains("payable", ignoreCase = true) || n.title.contains("Payment", ignoreCase = true) || n.message.contains("received", ignoreCase = true)
                            
                            val headerColor = when {
                                isAssignment -> SleekPrimary
                                isPayment -> SleekEmerald
                                else -> SleekRed
                            }

                            val icon = when {
                                isAssignment -> Icons.Default.EventNote
                                isPayment -> Icons.Default.Payments
                                else -> Icons.Default.Warning
                            }

                            val cardBg = if (n.isRead) MaterialTheme.colorScheme.surface else SleekPrimaryLight

                            Card(
                                onClick = { onMarkRead(n.id) },
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, if (!n.isRead) SleekPrimary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth().testTag("notif_item_${n.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Colored Category Circle Avatar Icon
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(headerColor.copy(alpha = 0.15f), CircleShape)
                                            .border(1.dp, headerColor.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = headerColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = n.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            
                                            // Channel Tag
                                            Text(
                                                text = n.channel.uppercase(Locale.getDefault()),
                                                color = headerColor,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier
                                                    .background(headerColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(Modifier.height(4.dp))
                                        
                                        Text(
                                            text = n.message,
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (!n.isRead) {
                                            Spacer(Modifier.height(6.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.clickable { onMarkRead(n.id) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Done,
                                                    contentDescription = "Mark read",
                                                    tint = SleekPrimary,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    text = "Tap to dismiss",
                                                    fontSize = 8.sp,
                                                    color = SleekPrimary,
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

                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().testTag("close_notif_drawer")
                ) {
                    Text("Close Panel", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TabButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.testTag("tab_${label.lowercase().replace(" ", "_").replace("&", "and")}")
    ) {
        Text(
            text = label,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun KanbanColumn(
    title: String,
    count: Int,
    items: List<LeadEntity>,
    accentColor: Color,
    onUpdateStatus: (Int, String) -> Unit,
    onDelete: (Int) -> Unit,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    nextStatus: String,
    prevStatus: String,
    activeColor: Color
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Column Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(accentColor, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$count",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (accentColor == Color.White) activeColor else accentColor
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Transparent, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Text("No active leads here.", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f))
                        Text("Ready for dispatch.", fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(items) { lead ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("kanban_lead_card_${lead.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = lead.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .background(activeColor.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(lead.source, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = activeColor)
                                            }
                                            Spacer(Modifier.width(4.dp))
                                            Text(lead.dateString, fontSize = 9.sp, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                    Text(
                                        text = "₹${lead.value}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        color = SleekEmerald
                                    )
                                }

                                if (lead.note.isNotEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = lead.note,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                // Drag Simulation Move Buttons Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Move back
                                    if (canMoveLeft) {
                                        IconButton(
                                            onClick = { onUpdateStatus(lead.id, prevStatus) },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Move Left",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(Modifier.size(24.dp))
                                    }

                                    // Delete and Quick Status indicator
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = lead.status,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                                        )
                                        IconButton(
                                            onClick = { onDelete(lead.id) },
                                            modifier = Modifier.size(24.dp).testTag("delete_lead_btn_${lead.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = SleekRed,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    // Move forward
                                    if (canMoveRight) {
                                        IconButton(
                                            onClick = { onUpdateStatus(lead.id, nextStatus) },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(activeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = "Move Right",
                                                tint = activeColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Simple mock of calendar month details to keep the calendar 100% pure Kotlin and dependency-free.
 * Returns a Pair: (Days in Month, Day-of-week index where 1=Sunday, 2=Monday, ..., 7=Saturday)
 */
fun getMonthDaysDetails(month: Int, year: Int): Pair<Int, Int> {
    val days = when (month) {
        1 -> 31
        2 -> if (year % 4 == 0) 29 else 28
        3 -> 31
        4 -> 30
        5 -> 31
        6 -> 30
        7 -> 31
        8 -> 31
        9 -> 30
        10 -> 31
        11 -> 30
        12 -> 31
        else -> 30
    }
    
    // Quick and clean Zeller's congruence implementation for start of month
    // Sunday = 1, Monday = 2, ..., Saturday = 7
    val q = 1 // 1st day of month
    val m = if (month < 3) month + 12 else month
    val y = if (month < 3) year - 1 else year
    val K = y % 100
    val J = y / 100
    // Zeller's congruenceDay of week:
    // h = (q + 13*(m+1)/5 + K + K/4 + J/4 + 5*J) % 7
    // Returns: 0 = Saturday, 1 = Sunday, 2 = Monday, ..., 6 = Friday
    val h = (q + (13 * (m + 1)) / 5 + K + K / 4 + J / 4 + 5 * J) % 7
    val dayOfWeekSunday1 = when (h) {
        0 -> 7 // Sat
        1 -> 1 // Sun
        2 -> 2 // Mon
        3 -> 3 // Tue
        4 -> 4 // Wed
        5 -> 5 // Thu
        6 -> 6 // Fri
        else -> 1
    }
    return Pair(days, dayOfWeekSunday1)
}
