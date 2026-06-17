package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. Entities
// ==========================================

@Entity(tableName = "studios")
data class StudioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val status: String, // "Pending", "Active"
    val storageUsedMb: Double,
    val storageLimitMb: Double,
    val isDriveApproved: Boolean,
    val isDriveConnected: Boolean,
    val isOneDriveConnected: Boolean,
    val teamCount: Int,
    val planType: String // "Free Launch Offer", "Pro"
)

@Entity(tableName = "leads")
data class LeadEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val source: String, // "Website", "WhatsApp", "Instagram", "Facebook", "Manual"
    val status: String, // "New Lead", "Follow Up", "Interested", "Quotation Sent", "Booked", "Lost"
    val value: Double,
    val phone: String,
    val email: String,
    val note: String,
    val dateString: String
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val clientName: String,
    val eventType: String, // "Wedding", "Birthday", "Corporate", "Outdoor Shoot"
    val dateString: String,
    val location: String,
    val packageName: String,
    val packagePrice: Double,
    val assignedTeam: String, // comma-separated names
    val storageProvider: String, // "Platform Storage", "Google Drive", "OneDrive"
    val status: String, // "Pre-Production", "Shooting", "Editing", "Client Gallery Ready", "Delivered"
    val dateLong: Long = System.currentTimeMillis()
)

@Entity(tableName = "quotations")
data class QuotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventId: Int,
    val clientName: String,
    val title: String,
    val amount: Double,
    val advanceAmount: Double,
    val notes: String,
    val emailSent: Boolean = false,
    val whatsappSent: Boolean = false,
    val pdfExported: Boolean = false,
    val dateString: String
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventId: Int,
    val clientName: String,
    val totalAmount: Double,
    val advanceAmount: Double,
    val pendingAmount: Double,
    val dueDateString: String,
    val status: String // "Pending", "Partial", "Paid"
)

@Entity(tableName = "gallery_items")
data class GalleryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventId: Int,
    val fileName: String,
    val storageProvider: String, // "Platform", "Google Drive", "OneDrive"
    val sizeBytes: Long,
    val fileType: String, // "image/jpeg", "video/mp4"
    val localUri: String = "",
    val isFavorite: Boolean = false,
    val isApprovedForAlbum: Boolean = false,
    
    // AI Metadata
    val aiTags: String = "", // comma-separated tags e.g. "Bride, Smiling, Portrait"
    val faceCoordinates: String = "", // face details JSON or description
    val isBlur: Boolean = false,
    val isDuplicate: Boolean = false,
    val isBestShot: Boolean = false,
    val uploadProgress: Int = 100
)

@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studioName: String,
    val subject: String,
    val description: String,
    val priority: String, // "Low", "Medium", "High"
    val status: String, // "Open", "Closed"
    val dateString: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val channel: String, // "In-App", "Email", "WhatsApp"
    val triggers: String, // e.g. "New Lead"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

// ==========================================
// 2. Data Access Objects (DAOs)
// ==========================================

@Dao
interface StudioDao {
    @Query("SELECT * FROM studios ORDER BY id DESC")
    fun getAllStudios(): Flow<List<StudioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudio(studio: StudioEntity)

    @Update
    suspend fun updateStudio(studio: StudioEntity)

    @Query("SELECT * FROM studios WHERE id = :id LIMIT 1")
    suspend fun getStudioById(id: Int): StudioEntity?

    @Query("DELETE FROM studios WHERE id = :id")
    suspend fun deleteStudio(id: Int)
}

@Dao
interface LeadDao {
    @Query("SELECT * FROM leads ORDER BY id DESC")
    fun getAllLeads(): Flow<List<LeadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: LeadEntity)

    @Update
    suspend fun updateLead(lead: LeadEntity)

    @Query("DELETE FROM leads WHERE id = :id")
    suspend fun deleteLead(id: Int)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY dateLong DESC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Int): EventEntity?

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEvent(id: Int)
}

@Dao
interface QuotationDao {
    @Query("SELECT * FROM quotations ORDER BY id DESC")
    fun getAllQuotations(): Flow<List<QuotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotation(quota: QuotationEntity)

    @Update
    suspend fun updateQuotation(quota: QuotationEntity)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY id DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)
}

@Dao
interface GalleryDao {
    @Query("SELECT * FROM gallery_items ORDER BY id DESC")
    fun getAllGalleryItems(): Flow<List<GalleryItemEntity>>

    @Query("SELECT * FROM gallery_items WHERE eventId = :eventId ORDER BY id DESC")
    fun getGalleryItemsForEvent(eventId: Int): Flow<List<GalleryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGalleryItem(item: GalleryItemEntity): Long

    @Update
    suspend fun updateGalleryItem(item: GalleryItemEntity)

    @Query("DELETE FROM gallery_items WHERE id = :itemId")
    suspend fun deleteGalleryItem(itemId: Int)
}

@Dao
interface SupportTicketDao {
    @Query("SELECT * FROM support_tickets ORDER BY id DESC")
    fun getAllTickets(): Flow<List<SupportTicketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: SupportTicketEntity)

    @Update
    suspend fun updateTicket(ticket: SupportTicketEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
}

// ==========================================
// 3. Database
// ==========================================

@Database(
    entities = [
        StudioEntity::class,
        LeadEntity::class,
        EventEntity::class,
        QuotationEntity::class,
        PaymentEntity::class,
        GalleryItemEntity::class,
        SupportTicketEntity::class,
        NotificationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StudioDatabase : RoomDatabase() {
    abstract fun studioDao(): StudioDao
    abstract fun leadDao(): LeadDao
    abstract fun eventDao(): EventDao
    abstract fun quotationDao(): QuotationDao
    abstract fun paymentDao(): PaymentDao
    abstract fun galleryDao(): GalleryDao
    abstract fun supportTicketDao(): SupportTicketDao
    abstract fun notificationDao(): NotificationDao
}
