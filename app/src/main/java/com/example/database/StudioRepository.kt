package com.example.database

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudioRepository private constructor(context: Context) {

    private val database: StudioDatabase = Room.databaseBuilder(
        context.applicationContext,
        StudioDatabase::class.java,
        "snapvex_studio_database"
    ).fallbackToDestructiveMigration().build()

    // Exposed DAOs
    val studioDao = database.studioDao()
    val leadDao = database.leadDao()
    val eventDao = database.eventDao()
    val quotationDao = database.quotationDao()
    val paymentDao = database.paymentDao()
    val galleryDao = database.galleryDao()
    val supportTicketDao = database.supportTicketDao()
    val notificationDao = database.notificationDao()

    init {
        // Run seed data on a background coroutine
        CoroutineScope(Dispatchers.IO).launch {
            seedIfEmpty()
        }
    }

    private suspend fun seedIfEmpty() {
        val count = studioDao.getAllStudios().first().size
        if (count == 0) {
            // 1. Seed Studio
            val defaultStudio = StudioEntity(
                name = "Vibrant Frames Co.",
                status = "Active",
                storageUsedMb = 38048.0, // About 38 GB of 50 GB
                storageLimitMb = 51200.0, // 50 GB
                isDriveApproved = true,
                isDriveConnected = true,
                isOneDriveConnected = false,
                teamCount = 5,
                planType = "Free Launch Offer" // 50 GB limit
            )
            studioDao.insertStudio(defaultStudio)

            val pendingStudio = StudioEntity(
                name = "Elegance Studios Mumbai",
                status = "Pending",
                storageUsedMb = 0.0,
                storageLimitMb = 51200.0,
                isDriveApproved = false,
                isDriveConnected = false,
                isOneDriveConnected = false,
                teamCount = 2,
                planType = "Free Launch Offer"
            )
            studioDao.insertStudio(pendingStudio)

            val proStudio = StudioEntity(
                name = "Apex Cinematic & Drone Services",
                status = "Active",
                storageUsedMb = 87400.0, // 87.4 GB
                storageLimitMb = 102400.0, // 100 GB (Pro plan)
                isDriveApproved = false, // Request Google Drive not approved yet
                isDriveConnected = false,
                isOneDriveConnected = false,
                teamCount = 12,
                planType = "Pro Plan"
            )
            studioDao.insertStudio(proStudio)

            // 2. Seed Leads
            val leads = listOf(
                LeadEntity(
                    name = "Aditi & Rahul Wedding",
                    source = "Instagram",
                    status = "Booked",
                    value = 75000.0,
                    phone = "+91 98765 43210",
                    email = "aditi.rahul@gmail.com",
                    note = "Requires standard outdoor pre-shoot, 3 wedding days, drone coverage included.",
                    dateString = "2026-06-12"
                ),
                LeadEntity(
                    name = "Tech Summit Bangalore 2026",
                    source = "Website",
                    status = "Interested",
                    value = 120000.0,
                    phone = "+91 91234 56789",
                    email = "corporate.events@techsummit.in",
                    note = "Corporate gala and keynote sessions. Videography and high-speed delivery requested.",
                    dateString = "2026-06-15"
                ),
                LeadEntity(
                    name = "Rhea's Fashion Portfolio",
                    source = "WhatsApp",
                    status = "Follow Up",
                    value = 25000.0,
                    phone = "+91 94444 88888",
                    email = "rhea@voguemodels.in",
                    note = "Prefers studio high-fashion look. Client sent aesthetic pinterest board links.",
                    dateString = "2026-05-30"
                ),
                LeadEntity(
                    name = "Nisha & Karan Golden Anniversary",
                    source = "Facebook",
                    status = "Quotation Sent",
                    value = 45000.0,
                    phone = "+91 93333 55555",
                    email = "karan.nisha.retro@gmail.com",
                    note = "Retro themed anniversary gala. Sent package catalog pricing, awaiting confirmation.",
                    dateString = "2026-06-05"
                ),
                LeadEntity(
                    name = "Baby Shower Shoot - Tanya",
                    source = "Manual",
                    status = "New Lead",
                    value = 18000.0,
                    phone = "+91 95555 11111",
                    email = "tanya.sharma@yahoo.com",
                    note = "Outdoor cozy aesthetic shoot in Cubbon Park.",
                    dateString = "2026-06-16"
                )
            )
            leads.forEach { leadDao.insertLead(it) }

            // 3. Seed Events (with realistic folder status)
            val event1Id = eventDao.insertEvent(EventEntity(
                name = "Maya & Arjun Royal Wedding",
                clientName = "Arjun Kapoor",
                eventType = "Wedding",
                dateString = "2026-06-20",
                location = "Rajasthan Palace, Udaipur",
                packageName = "Luxury Imperial Package",
                packagePrice = 150000.0,
                assignedTeam = "Vikram (Photographer), Anita (Editor), Sonia (Designer)",
                storageProvider = "Platform Storage",
                status = "Editing"
            )).toInt()

            val event2Id = eventDao.insertEvent(EventEntity(
                name = "Mumbai Fashion Runway 2026",
                clientName = "Zara India Marketing",
                eventType = "Outdoor Shoot",
                dateString = "2026-06-28",
                location = "Nesco Center, Mumbai",
                packageName = "Commercial Editorial Shoot",
                packagePrice = 95000.0,
                assignedTeam = "Vikram (Photographer), Anita (Editor)",
                storageProvider = "Google Drive",
                status = "Shooting"
            )).toInt()

            val event3Id = eventDao.insertEvent(EventEntity(
                name = "Meera & Amit Beach Engagement",
                clientName = "Meera Roy",
                eventType = "Wedding",
                dateString = "2026-05-18",
                location = "Zuri White Sands, Goa",
                packageName = "Golden Hour Pre-Wedding",
                packagePrice = 55000.0,
                assignedTeam = "Kabir (Photographer), Anita (Editor), Sonia (Designer)",
                storageProvider = "OneDrive",
                status = "Delivered"
            )).toInt()

            // 4. Seed Quotations
            quotationDao.insertQuotation(QuotationEntity(
                eventId = event1Id,
                clientName = "Arjun Kapoor",
                title = "Imperial Palace Wedding Cover Package",
                amount = 150000.0,
                advanceAmount = 50000.0,
                notes = "Includes drone cinematography, 1 main visual cinema album, 3 hardbound physical output copies.",
                pdfExported = true,
                emailSent = true,
                whatsappSent = true,
                dateString = "2026-06-10"
            ))

            quotationDao.insertQuotation(QuotationEntity(
                eventId = event2Id,
                clientName = "Zara India Marketing",
                title = "Commercial Runway Cover proposal",
                amount = 95000.0,
                advanceAmount = 30000.0,
                notes = "High speed 24-hr delivery for edited media to Zara central handle.",
                pdfExported = true,
                emailSent = true,
                whatsappSent = false,
                dateString = "2026-06-14"
            ))

            // 5. Seed Payments
            paymentDao.insertPayment(PaymentEntity(
                eventId = event1Id,
                clientName = "Arjun Kapoor",
                totalAmount = 150000.0,
                advanceAmount = 50000.0,
                pendingAmount = 100000.0,
                dueDateString = "2026-06-25",
                status = "Partial"
            ))

            paymentDao.insertPayment(PaymentEntity(
                eventId = event2Id,
                clientName = "Zara India Marketing",
                totalAmount = 95000.0,
                advanceAmount = 0.0,
                pendingAmount = 95000.0,
                dueDateString = "2026-06-30",
                status = "Pending"
            ))

            paymentDao.insertPayment(PaymentEntity(
                eventId = event3Id,
                clientName = "Meera Roy",
                totalAmount = 55000.0,
                advanceAmount = 55000.0,
                pendingAmount = 0.0,
                dueDateString = "2026-05-20",
                status = "Paid"
            ))

            // 6. Seed Gallery items for "Maya & Arjun Royal Wedding" event
            // Some RAW photos, some edited, some with favorites and AI-analysis simulated metadata
            val items = listOf(
                GalleryItemEntity(
                    eventId = event1Id,
                    fileName = "RAW_DSC03049_wedding_gate.arw",
                    storageProvider = "Platform",
                    sizeBytes = 47185920L, // 45 MB RAW Photo
                    fileType = "image/jpeg", // locally previews
                    localUri = "https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&q=80&w=400",
                    isFavorite = false,
                    aiTags = "RAW, Gate Entrance, Setup",
                    isBlur = false,
                    isDuplicate = false,
                    isBestShot = false
                ),
                GalleryItemEntity(
                    eventId = event1Id,
                    fileName = "RAW_DSC03058_bride_getting_ready.arw",
                    storageProvider = "Platform",
                    sizeBytes = 49283072L, // 47 MB RAW
                    fileType = "image/jpeg",
                    localUri = "https://images.unsplash.com/photo-1591604466107-ec97de577aff?auto=format&fit=crop&q=80&w=400",
                    isFavorite = true,
                    aiTags = "RAW, Bride, Getting Ready, Portrait",
                    isBlur = false,
                    isDuplicate = false,
                    isBestShot = true
                ),
                GalleryItemEntity(
                    eventId = event1Id,
                    fileName = "RAW_DSC03099_shub_vivah.arw",
                    storageProvider = "Platform",
                    sizeBytes = 51380224L, // 49 MB
                    fileType = "image/jpeg",
                    localUri = "https://images.unsplash.com/photo-1606800052052-a08af7148866?auto=format&fit=crop&q=80&w=400",
                    isFavorite = false,
                    aiTags = "RAW, Ritual, Fire, Mandap",
                    isBlur = false,
                    isDuplicate = false,
                    isBestShot = false
                ),
                GalleryItemEntity(
                    eventId = event1Id,
                    fileName = "RAW_DSC03112_blur_shub.arw",
                    storageProvider = "Platform",
                    sizeBytes = 46137344L,
                    fileType = "image/jpeg",
                    localUri = "https://images.unsplash.com/photo-1511285560929-80b456fea0bc?auto=format&fit=crop&q=80&w=400",
                    isFavorite = false,
                    aiTags = "RAW, Out of Focus, Hallway",
                    isBlur = true, // Blur Detection Triggered
                    isDuplicate = false,
                    isBestShot = false
                ),
                GalleryItemEntity(
                    eventId = event1Id,
                    fileName = "EDITED_DSC03058_bride_makeup.jpg",
                    storageProvider = "Platform",
                    sizeBytes = 8123904L, // 7.7 MB edited jpg
                    fileType = "image/jpeg",
                    localUri = "https://images.unsplash.com/photo-1583939003579-730e3918a45a?auto=format&fit=crop&q=80&w=400",
                    isFavorite = true,
                    isApprovedForAlbum = true,
                    aiTags = "EDITED, Bride, Glamour, Best Shot, Face Detected",
                    faceCoordinates = "[{x:120, y:230, w:80, h:80, confidence:0.99}]", // Custom face metadata
                    isBlur = false,
                    isDuplicate = false,
                    isBestShot = true
                ),
                GalleryItemEntity(
                    eventId = event1Id,
                    fileName = "EDITED_DSC03099_vows_closeup.jpg",
                    storageProvider = "Platform",
                    sizeBytes = 7241216L,
                    fileType = "image/jpeg",
                    localUri = "https://images.unsplash.com/photo-1507504038482-76210f54ce1a?auto=format&fit=crop&q=80&w=400",
                    isFavorite = true,
                    isApprovedForAlbum = false,
                    aiTags = "EDITED, Couple, Vows, Ring, Mandap",
                    faceCoordinates = "[{x:180, y:150}, {x:240, y:155}]",
                    isBlur = false,
                    isDuplicate = false,
                    isBestShot = false
                ),
                GalleryItemEntity(
                    eventId = event1Id,
                    fileName = "EDITED_DSC03099_duplicate_closeup.jpg",
                    storageProvider = "Platform",
                    sizeBytes = 7136000L,
                    fileType = "image/jpeg",
                    localUri = "https://images.unsplash.com/photo-1507504038482-76210f54ce1a?auto=format&fit=crop&q=80&w=400",
                    isFavorite = false,
                    isApprovedForAlbum = false,
                    aiTags = "EDITED, Couple, Vows, Ring",
                    isBlur = false,
                    isDuplicate = true, // Duplicate detection!
                    isBestShot = false
                )
            )
            items.forEach { galleryDao.insertGalleryItem(it) }

            // 7. Seed Notifications
            val alerts = listOf(
                NotificationEntity(
                    title = "New Lead Generated",
                    message = "Aditi & Rahul Wedding lead captured via Instagram integration pipeline.",
                    channel = "In-App",
                    triggers = "New Lead"
                ),
                NotificationEntity(
                    title = "Event Assigned to Team",
                    message = "You have been assigned to Mumbai Fashion Runway 2026. Review shoot plan instructions.",
                    channel = "WhatsApp",
                    triggers = "Event Assigned"
                ),
                NotificationEntity(
                    title = "Storage Limit Reached 74%",
                    message = "Warning: Vibrant Frames Co. has used 38.0 GB (74%) of the 50.0 GB Launch Offer limit. Complete events to archive folders.",
                    channel = "Email",
                    triggers = "Upload Complete"
                ),
                NotificationEntity(
                    title = "Client Album Approved",
                    message = "Meera & Amit approved their 42-page virtual album design for final execution.",
                    channel = "In-App",
                    triggers = "Album Approval"
                )
            )
            alerts.forEach { notificationDao.insertNotification(it) }

            // 8. Seed Support Tickets
            val tickets = listOf(
                SupportTicketEntity(
                    studioName = "Vibrant Frames Co.",
                    subject = "Unable to connect OneDrive storage API",
                    description = "When trying to OAuth authorize OneDrive, it receives resource mismatch error. Studio Admin claims account is business.",
                    priority = "High",
                    status = "Open",
                    dateString = "2026-06-15"
                ),
                SupportTicketEntity(
                    studioName = "Apex Cinematic",
                    subject = "Inquire about 2TB custom plan billing",
                    description = "Client requested WhatsApp response for customized 2TB pricing tier with physical storage servers hybrid mirroring.",
                    priority = "Medium",
                    status = "Open",
                    dateString = "2026-06-16"
                )
            )
            tickets.forEach { supportTicketDao.insertTicket(it) }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: StudioRepository? = null

        fun getInstance(context: Context): StudioRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = StudioRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
