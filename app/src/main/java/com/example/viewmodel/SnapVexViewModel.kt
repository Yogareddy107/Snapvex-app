package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiHelper
import com.example.database.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

class SnapVexViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudioRepository.getInstance(application)

    // Live state flows collected from DB
    val studios = repository.studioDao.getAllStudios().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val leads = repository.leadDao.getAllLeads().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val events = repository.eventDao.getAllEvents().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val quotations = repository.quotationDao.getAllQuotations().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val payments = repository.paymentDao.getAllPayments().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val notifications = repository.notificationDao.getAllNotifications().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val supportTickets = repository.supportTicketDao.getAllTickets().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI Interactive States
    val _currentRole = MutableStateFlow<String>("Landing Page") // "Landing Page", "Super Admin", "Studio Admin", "Photographer", "Editor", "Album Designer", "Client"
    val currentRole: StateFlow<String> = _currentRole.asStateFlow()

    // Selection helper
    private val _selectedEventId = MutableStateFlow<Int>(-1)
    val selectedEventId: StateFlow<Int> = _selectedEventId.asStateFlow()

    val activeEvent: Flow<EventEntity?> = _selectedEventId.flatMapLatest { id ->
        if (id == -1) flowOf(null)
        else flow {
            emit(repository.eventDao.getEventById(id))
        }
    }

    val galleryItems: Flow<List<GalleryItemEntity>> = _selectedEventId.flatMapLatest { id ->
        if (id == -1) flowOf(emptyList())
        else repository.galleryDao.getGalleryItemsForEvent(id)
    }

    // Active Studio configuration (simulating multi-tenant dashboard)
    private val _activeStudioId = MutableStateFlow<Int>(1) // Swappable in Super Admin or Settings
    val activeStudioId: StateFlow<Int> = _activeStudioId.asStateFlow()

    val activeStudio: Flow<StudioEntity?> = _activeStudioId.flatMapLatest { id ->
        flow {
            emit(repository.studioDao.getStudioById(id))
        }
    }

    // Interactive Registration / Connection States
    val registerStep = MutableStateFlow<String>("Form") // "Form", "Verification", "AwaitApproval"
    val registeredStudioName = MutableStateFlow<String>("")
    val registeredStudioEmail = MutableStateFlow<String>("")

    // ----------------------------------------------------
    // Phone-based SMS OTP Setup & Activity Log Tracking
    // ----------------------------------------------------
    val phoneAuthStep = MutableStateFlow<String>("PhoneInput") // "PhoneInput", "VerifyCode", "LoggedIn"
    val smsSentPhoneNumber = MutableStateFlow<String>("")
    val simulatedOtps = MutableStateFlow<String>("420888")

    private val _activityLogs = MutableStateFlow<List<ActivityLogItem>>(listOf(
        ActivityLogItem(1, System.currentTimeMillis() - 7200000, "Studio Admin", "Connected Microsoft OneDrive API pipeline", "Storage Event", "Global"),
        ActivityLogItem(2, System.currentTimeMillis() - 3600000, "Super Admin", "Approved Google Drive connection telemetry integration", "System Notification", "Global"),
        ActivityLogItem(3, System.currentTimeMillis() - 1800000, "Photographer", "Uploaded 48 RAW photos to Aisha & Kabir Wedding event", "User Action", "Aisha & Kabir Wedding", 1),
        ActivityLogItem(4, System.currentTimeMillis() - 600000, "Editor", "Retouched and updated 24 files with category labels and wedding styles", "User Action", "Aisha & Kabir Wedding", 1)
    ))
    val activityLogs: StateFlow<List<ActivityLogItem>> = _activityLogs.asStateFlow()

    fun logActivity(role: String, action: String, type: String, eventName: String = "Global", eventId: Int = -1) {
        val newItem = ActivityLogItem(
            id = (_activityLogs.value.maxOfOrNull { it.id } ?: 0) + 1,
            timestamp = System.currentTimeMillis(),
            role = role,
            action = action,
            type = type,
            eventName = eventName,
            eventId = eventId
        )
        _activityLogs.value = listOf(newItem) + _activityLogs.value
    }

    fun sendSmsOtp(phoneNumber: String) {
        viewModelScope.launch {
            smsSentPhoneNumber.value = phoneNumber
            phoneAuthStep.value = "VerifyCode"
            // Insert notification simulating SMS provider
            repository.notificationDao.insertNotification(NotificationEntity(
                title = "SUPABASE SMS OTP DISPATCH",
                message = "Secure SMS OTP verification code 420888 generated for phone: $phoneNumber. Expires in 5 minutes.",
                channel = "WhatsApp",
                triggers = "SMS Dispatch"
            ))
            logActivity("System", "Dispatched Twilio pipeline OTP to $phoneNumber via Supabase Auth adapter", "System Notification")
        }
    }

    fun verifySmsOtpAndLogIn(code: String): Boolean {
        if (code == "420888") {
            phoneAuthStep.value = "LoggedIn"
            _currentRole.value = "Studio Admin"
            logActivity("Studio Admin", "Logged in securely via phone OTP authentication code", "User Action")
            return true
        }
        return false
    }

    fun resetPhoneAuth() {
        phoneAuthStep.value = "PhoneInput"
        smsSentPhoneNumber.value = ""
    }

    // Simulated task state
    private val _isUploading = MutableStateFlow<Boolean>(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Int>(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress.asStateFlow()

    fun switchRole(role: String) {
        _currentRole.value = role
    }

    fun selectEvent(id: Int) {
        _selectedEventId.value = id
    }

    fun selectStudio(id: Int) {
        _activeStudioId.value = id
    }

    // ==========================================
    // Core Workflow Methods
    // ==========================================

    // 1. Studio Registration Flow
    fun registerStudio(name: String, email: String) {
        viewModelScope.launch {
            registeredStudioName.value = name
            registeredStudioEmail.value = email
            registerStep.value = "Verification"
        }
    }

    fun verifyEmailAndCreateStudio() {
        viewModelScope.launch {
            val newStudio = StudioEntity(
                name = registeredStudioName.value,
                status = "Pending", // Awaiting approval
                storageUsedMb = 0.0,
                storageLimitMb = 51200.0, // 50 GB standard Launch offer
                isDriveApproved = false,
                isDriveConnected = false,
                isOneDriveConnected = false,
                teamCount = 1,
                planType = "Free Launch Offer"
            )
            repository.studioDao.insertStudio(newStudio)

            // Trigger notification
            repository.notificationDao.insertNotification(NotificationEntity(
                title = "New Studio Registered",
                message = "${registeredStudioName.value} registered for SnapVex Studio OS. Awaiting Super Admin approval.",
                channel = "In-App",
                triggers = "New Lead"
            ))

            registerStep.value = "AwaitApproval"
        }
    }

    // 2. Super Admin Approval Flow
    fun approveStudio(studioId: Int) {
        viewModelScope.launch {
            val studio = repository.studioDao.getStudioById(studioId)
            if (studio != null) {
                val updated = studio.copy(status = "Active")
                repository.studioDao.updateStudio(updated)

                repository.notificationDao.insertNotification(NotificationEntity(
                    title = "Studio Approved",
                    message = "${studio.name} registration was approved by platform administrator. Plan: ${studio.planType}.",
                    channel = "In-App",
                    triggers = "Event Assigned"
                ))
            }
        }
    }

    // 3. Storage Provider Request & Approval Flow
    fun requestDriveApproval(studioId: Int) {
        viewModelScope.launch {
            val studio = repository.studioDao.getStudioById(studioId)
            if (studio != null) {
                // Submit storage request to Super Admin
                repository.notificationDao.insertNotification(NotificationEntity(
                    title = "Google Drive API Access Request",
                    message = "Studio '${studio.name}' submitted a request to approve Google Drive API storage connection.",
                    channel = "In-App",
                    triggers = "New Lead"
                ))
            }
        }
    }

    fun approveDriveRequest(studioId: Int) {
        viewModelScope.launch {
            val studio = repository.studioDao.getStudioById(studioId)
            if (studio != null) {
                val updated = studio.copy(isDriveApproved = true)
                repository.studioDao.updateStudio(updated)

                repository.notificationDao.insertNotification(NotificationEntity(
                    title = "Drive Provider Approved",
                    message = "Platform verified Google Drive API limits for studio: ${studio.name}.",
                    channel = "In-App",
                    triggers = "Event Assigned"
                ))
            }
        }
    }

    fun connectDrive(studioId: Int, isConnected: Boolean, provider: String) {
        viewModelScope.launch {
            val studio = repository.studioDao.getStudioById(studioId)
            if (studio != null) {
                val updated = if (provider == "Google Drive") {
                    studio.copy(isDriveConnected = isConnected)
                } else {
                    studio.copy(isOneDriveConnected = isConnected)
                }
                repository.studioDao.updateStudio(updated)

                repository.notificationDao.insertNotification(NotificationEntity(
                    title = "$provider Connected",
                    message = "API Synchronization connection active for $provider in ${studio.name}.",
                    channel = "In-App",
                    triggers = "Upload Complete"
                ))
            }
        }
    }

    fun grantExtraStorage(studioId: Int, sizeMb: Double) {
        viewModelScope.launch {
            val studio = repository.studioDao.getStudioById(studioId)
            if (studio != null) {
                val updated = studio.copy(storageLimitMb = studio.storageLimitMb + sizeMb)
                repository.studioDao.updateStudio(updated)

                repository.notificationDao.insertNotification(NotificationEntity(
                    title = "Extended Storage Granted",
                    message = "Super Admin added ${sizeMb / 1024} GB extra storage space to studio ${studio.name}.",
                    channel = "In-App",
                    triggers = "Event Assigned"
                ))
            }
        }
    }

    // 4. Event Management with Auto Folder Creation
    fun createEvent(
        name: String,
        clientName: String,
        eventType: String,
        dateString: String,
        location: String,
        packagePrice: Double,
        assignedTeam: String,
        storageProvider: String
    ) {
        viewModelScope.launch {
            val newEvent = EventEntity(
                name = name,
                clientName = clientName,
                eventType = eventType,
                dateString = dateString,
                location = location,
                packageName = "Standard Commercial Suite",
                packagePrice = packagePrice,
                assignedTeam = assignedTeam,
                storageProvider = storageProvider,
                status = "Pre-Production"
            )
            val eventId = repository.eventDao.insertEvent(newEvent).toInt()

            // Trigger notification
            repository.notificationDao.insertNotification(NotificationEntity(
                title = "New Event Created: $name",
                message = "Auto-generated structured storage on $storageProvider: RAW Photos, RAW Videos, Edited Photos, Edited Videos, Album Design, Client Selection, Delivery, Archive folders active.",
                channel = "WhatsApp",
                triggers = "Event Assigned"
            ))

            // Create initial payment tracker automatically
            repository.paymentDao.insertPayment(PaymentEntity(
                eventId = eventId,
                clientName = clientName,
                totalAmount = packagePrice,
                advanceAmount = 0.0,
                pendingAmount = packagePrice,
                dueDateString = dateString,
                status = "Pending"
            ))

            // Create placeholder Quotation automatically
            repository.quotationDao.insertQuotation(QuotationEntity(
                eventId = eventId,
                clientName = clientName,
                title = "$name - Studio Coverage Package",
                amount = packagePrice,
                advanceAmount = packagePrice * 0.3,
                notes = "Auto-generated from packaging details. Includes physical delivery folders setup.",
                dateString = dateString
            ))
        }
    }

    // 5. CRM Leads Operations
    fun addLead(name: String, source: String, status: String, value: Double, phone: String, email: String, note: String) {
        viewModelScope.launch {
            val newLead = LeadEntity(
                name = name,
                source = source,
                status = status,
                value = value,
                phone = phone,
                email = email,
                note = note,
                dateString = "2026-06-16"
            )
            repository.leadDao.insertLead(newLead)

            repository.notificationDao.insertNotification(NotificationEntity(
                title = "New Lead Logged",
                message = "Lead '$name' added manually via agency desk CRM pipeline ($source).",
                channel = "In-App",
                triggers = "New Lead"
            ))
        }
    }

    fun updateLeadStatus(leadId: Int, newStatus: String) {
        viewModelScope.launch {
            val all = LeadsFlowWorkaround()
            val lead = all.firstOrNull { it.id == leadId }
            if (lead != null) {
                val updated = lead.copy(status = newStatus)
                repository.leadDao.insertLead(updated) // REPLACE conflict inserts/updates

                // If lead booked, automatically trigger booked log
                if (newStatus == "Booked") {
                    repository.notificationDao.insertNotification(NotificationEntity(
                        title = "Lead Confirmed Booked!",
                        message = "Lead ${lead.name} marked as BOOKED. Converting profile to Active Studio Client.",
                        channel = "WhatsApp",
                        triggers = "New Lead"
                    ))
                }
            }
        }
    }

    private suspend fun LeadsFlowWorkaround(): List<LeadEntity> {
        return repository.leadDao.getAllLeads().first()
    }

    // 6. Media Uploads (Photographer Workflow with AI Scanning integrated)
    fun photographerUploadMedia(eventId: Int, name: String, fileType: String, sizeBytes: Long, customUrl: String? = null) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadProgress.value = 10
            
            // Generate standard Unsplash image triggers based on keywords
            val localUriSeed = customUrl ?: when {
                fileType.contains("video", true) -> "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?auto=format&fit=crop&q=80&w=400"
                name.contains("bride", true) -> "https://images.unsplash.com/photo-1591604466107-ec97de577aff?auto=format&fit=crop&q=80&w=400"
                name.contains("couple", true) -> "https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&q=80&w=400"
                name.contains("ring", true) -> "https://images.unsplash.com/photo-1507504038482-76210f54ce1a?auto=format&fit=crop&q=80&w=400"
                else -> "https://images.unsplash.com/photo-1542038784456-1ea8e935640e?auto=format&fit=crop&q=80&w=400"
            }

            // Simulate progress bar filling
            for (progress in 20..100 step 20) {
                kotlinx.coroutines.delay(150)
                _uploadProgress.value = progress
            }

            // Generate initial item in database
            val newItem = GalleryItemEntity(
                eventId = eventId,
                fileName = "RAW_$name",
                storageProvider = "Platform",
                sizeBytes = sizeBytes,
                fileType = fileType,
                localUri = localUriSeed,
                isFavorite = false,
                aiTags = "RAW, Analyzing...",
                uploadProgress = 100
            )
            val itemId = repository.galleryDao.insertGalleryItem(newItem).toInt()

            _isUploading.value = false

            // Update studio storage limits
            val studioList = studios.value
            val mainStudio = studioList.firstOrNull { it.id == _activeStudioId.value }
            if (mainStudio != null) {
                val addedMb = sizeBytes.toDouble() / (1024.0 * 1024.0)
                val updatedMb = mainStudio.storageUsedMb + addedMb
                repository.studioDao.updateStudio(mainStudio.copy(storageUsedMb = updatedMb))
                
                // Monitor sizes and fire triggers
                val usagePercent = (updatedMb / mainStudio.storageLimitMb) * 100.0
                if (usagePercent >= 100.0) {
                    repository.notificationDao.insertNotification(NotificationEntity(
                        title = "CRITICAL LIMIT REACHED",
                        message = "100% capacity filled! Studio upload forms deactivated. Reach support to upgrade.",
                        channel = "Email",
                        triggers = "Upload Complete"
                    ))
                } else if (usagePercent >= 90.0) {
                    repository.notificationDao.insertNotification(NotificationEntity(
                        title = "90% STORAGE ALERT",
                        message = "Critical limit approaching: ${String.format("%.1f", usagePercent)}% storage spaces filled.",
                        channel = "WhatsApp",
                        triggers = "Upload Complete"
                    ))
                } else if (usagePercent >= 80.0) {
                    repository.notificationDao.insertNotification(NotificationEntity(
                        title = "80% STORAGE WARN",
                        message = "Studio is over 80% usage limits! Notify active team leaders.",
                        channel = "In-App",
                        triggers = "Upload Complete"
                    ))
                }
            }

            repository.notificationDao.insertNotification(NotificationEntity(
                title = "Media File Sync Uploaded",
                message = "File RAW_$name securely moved to Connected storage via API tunnel.",
                channel = "In-App",
                triggers = "Upload Complete"
            ))

            // Trigger AI analysis on upload
            runAIScanOnItem(itemId, name)
        }
    }

    // AI SCANNING ENGINE (Face recognition, Blur detection, Tagging, Duplicate detection simulations)
    fun runAIScanOnItem(itemId: Int, descriptionText: String) {
        viewModelScope.launch {
            val apiText = GeminiHelper.analyzePhoto("A photo describing: $descriptionText")
            
            var generatedTags = "RAW, Photographed"
            var faces = 0
            var blurs = false
            var duplicates = false
            var bests = false

            if (apiText != "MOCK_KEY" && apiText.isNotEmpty()) {
                try {
                    val json = JSONObject(apiText)
                    val tgArray = json.optJSONArray("tags")
                    if (tgArray != null) {
                        val buffer = ArrayList<String>()
                        for (i in 0 until tgArray.length()) {
                            buffer.add(tgArray.getString(i))
                        }
                        generatedTags = buffer.joinToString(", ").uppercase(Locale.getDefault())
                    }
                    faces = json.optInt("facesDetected", 0)
                    blurs = json.optBoolean("isBlur", false)
                    duplicates = json.optBoolean("isDuplicate", false)
                    bests = json.optBoolean("isBestShot", false)
                } catch (e: Exception) {
                    // Fallback to text matching
                    if (apiText.contains("blur", true)) blurs = true
                }
            } else {
                // HIGH QUALITY LOCAL SIMULATOR (Falls back if API Key is empty)
                val descLower = descriptionText.lowercase(Locale.getDefault())
                when {
                    descLower.contains("bride") || descLower.contains("makeup") -> {
                        generatedTags = "AI TAGS: BRIDE, PORTRAIT, GLAMOUR, TRADITIONAL"
                        faces = 1
                        bests = true
                    }
                    descLower.contains("couple") || descLower.contains("wedding") || descLower.contains("vivah") -> {
                        generatedTags = "AI TAGS: COUPLE, ROMANCE, CEREMONY, WEDDING"
                        faces = 2
                        bests = descLower.contains("royal")
                    }
                    descLower.contains("ring") || descLower.contains("jewelry") -> {
                        generatedTags = "AI TAGS: MACRO, DETAIL, RING, ACCESSORY"
                        faces = 0
                    }
                    descLower.contains("shub") -> {
                        generatedTags = "AI TAGS: MANDAP, RITUAL, RUSTIC"
                        faces = 3
                        duplicates = true
                    }
                    descLower.contains("blur") || descLower.contains("out of focus") -> {
                        generatedTags = "AI TAGS: BLURRY, POOR FOCUS"
                        blurs = true
                    }
                    else -> {
                        generatedTags = "AI TAGS: EVENT, CANDID, INDOOR"
                        faces = 1
                    }
                }
            }

            // Retrieve and update database
            val allItems = repository.galleryDao.getAllGalleryItems().first()
            val existing = allItems.find { it.id == itemId }
            if (existing != null) {
                val updated = existing.copy(
                    aiTags = generatedTags,
                    faceCoordinates = if (faces > 0) "[{faces_detected: $faces}]" else "",
                    isBlur = blurs,
                    isDuplicate = duplicates,
                    isBestShot = bests
                )
                repository.galleryDao.updateGalleryItem(updated)
            }
        }
    }

    // 7. Editor Workflow (Editor Login, Download RAW, Upload EDITED placed automatically)
    fun editorDownloadRawMedia(filename: String) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadProgress.value = 10
            for (p in 20..100 step 20) {
                kotlinx.coroutines.delay(100)
                _uploadProgress.value = p
            }
            _isUploading.value = false

            repository.notificationDao.insertNotification(NotificationEntity(
                title = "Raw File Downloaded",
                message = "Editor checked out file $filename for editing locally.",
                channel = "In-App",
                triggers = "Event Assigned"
            ))
        }
    }

    fun editorUploadEditedMedia(eventId: Int, rawFilename: String, sizeMb: Double) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadProgress.value = 20
            
            val editedName = rawFilename.replace("RAW_", "EDITED_").replace(".arw", ".jpg")
            
            for (p in 40..100 step 20) {
                kotlinx.coroutines.delay(120)
                _uploadProgress.value = p
            }

            val sizeBytes = (sizeMb * 1024.0 * 1024.0).toLong()

            val editedItem = GalleryItemEntity(
                eventId = eventId,
                fileName = editedName,
                storageProvider = "Platform",
                sizeBytes = sizeBytes,
                fileType = "image/jpeg",
                localUri = "https://images.unsplash.com/photo-1511285560929-80b456fea0bc?auto=format&fit=crop&q=80&w=400",
                isFavorite = false,
                aiTags = "EDITED, High Definition, Color Corrected",
                uploadProgress = 100
            )
            repository.galleryDao.insertGalleryItem(editedItem)

            _isUploading.value = false

            // Update event status to Client Gallery Ready
            val event = repository.eventDao.getEventById(eventId)
            if (event != null) {
                repository.eventDao.updateEvent(event.copy(status = "Client Gallery Ready"))
            }

            repository.notificationDao.insertNotification(NotificationEntity(
                title = "Edited File Uploaded",
                message = "Media automatically saved inside 'Edited Photos' directory. Client Gallery online.",
                channel = "Email",
                triggers = "Gallery Ready"
            ))
        }
    }

    // 8. Client Album Design & Gallery interactions
    fun clientFavoritePhoto(itemId: Int, isFav: Boolean) {
        viewModelScope.launch {
            val allItems = repository.galleryDao.getAllGalleryItems().first()
            val existing = allItems.find { it.id == itemId }
            if (existing != null) {
                repository.galleryDao.updateGalleryItem(existing.copy(isFavorite = isFav))
            }
        }
    }

    fun clientSetAlbumSelection(itemId: Int, selectForAlbum: Boolean) {
        viewModelScope.launch {
            val allItems = repository.galleryDao.getAllGalleryItems().first()
            val existing = allItems.find { it.id == itemId }
            if (existing != null) {
                repository.galleryDao.updateGalleryItem(existing.copy(isApprovedForAlbum = selectForAlbum))
                if (selectForAlbum) {
                    repository.notificationDao.insertNotification(NotificationEntity(
                        title = "Favorite Photo Approved for Album",
                        message = "Client marked ${existing.fileName} as 'Required for Design' output list.",
                        channel = "In-App",
                        triggers = "Album Approval"
                    ))
                }
            }
        }
    }

    fun clientApproveAlbum(eventId: Int, approve: Boolean, comment: String) {
        viewModelScope.launch {
            val event = repository.eventDao.getEventById(eventId)
            if (event != null) {
                val newStatus = if (approve) "Delivered" else "Editing"
                repository.eventDao.updateEvent(event.copy(status = newStatus))

                val notificationTitle = if (approve) "Album Fully Approved!" else "Revision Requested for Album"
                val notificationMsg = if (approve) {
                    "Client approved full hardbound design. Generating printable layout folders."
                } else {
                    "Client requested alterations. Message: '$comment'"
                }

                repository.notificationDao.insertNotification(NotificationEntity(
                    title = notificationTitle,
                    message = notificationMsg,
                    channel = "WhatsApp",
                    triggers = "Album Approval"
                ))
            }
        }
    }

    // 9. Billing and Quotations exportations
    fun recordPayment(paymentId: Int, recAmountString: String) {
        viewModelScope.launch {
            val amt = recAmountString.toDoubleOrNull() ?: 0.0
            val all = repository.paymentDao.getAllPayments().first()
            val pay = all.find { it.id == paymentId }
            if (pay != null && amt > 0.0) {
                val newAdvance = pay.advanceAmount + amt
                val newPending = maxOf(0.0, pay.totalAmount - newAdvance)
                val newStatus = when {
                    newPending <= 0 -> "Paid"
                    newAdvance > 0 -> "Partial"
                    else -> "Pending"
                }
                
                repository.paymentDao.insertPayment(pay.copy(
                    advanceAmount = newAdvance,
                    pendingAmount = newPending,
                    status = newStatus
                ))

                // trigger in app notification
                repository.notificationDao.insertNotification(NotificationEntity(
                    title = "Payment Logged: ₹$amt",
                    message = "Advance/Installment payment received from ${pay.clientName}. Outstanding balance: ₹$newPending.",
                    channel = "In-App",
                    triggers = "Payment Due"
                ))
            }
        }
    }

    fun exportQuotation(quotaId: Int, type: String) {
        viewModelScope.launch {
            val all = repository.quotationDao.getAllQuotations().first()
            val existing = all.find { it.id == quotaId }
            if (existing != null) {
                val updated = when(type) {
                    "PDF" -> existing.copy(pdfExported = true)
                    "WhatsApp" -> existing.copy(whatsappSent = true)
                    "Email" -> existing.copy(emailSent = true)
                    else -> existing
                }
                repository.quotationDao.insertQuotation(updated)

                repository.notificationDao.insertNotification(NotificationEntity(
                    title = "Quotation Exported via $type",
                    message = "Quotation of ₹${existing.amount} dispatched successfully using $type stream pipeline.",
                    channel = type,
                    triggers = "Payment Due"
                ))
            }
        }
    }

    fun deleteLead(id: Int) {
        viewModelScope.launch {
            repository.leadDao.deleteLead(id)
        }
    }

    fun deleteEvent(id: Int) {
        viewModelScope.launch {
            repository.eventDao.deleteEvent(id)
        }
    }

    fun markNotificationAsRead(id: Int) {
        viewModelScope.launch {
            repository.notificationDao.markAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.notificationDao.markAllAsRead()
        }
    }
}

data class ActivityLogItem(
    val id: Int,
    val timestamp: Long,
    val role: String,
    val action: String,
    val type: String, // "User Action", "System Notification", "Storage Event"
    val eventName: String,
    val eventId: Int = -1
)
