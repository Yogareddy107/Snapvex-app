from fastapi import FastAPI, Depends, HTTPException, status, Query, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from typing import List, Optional
from pydantic import BaseModel, EmailStr

from auth.rbac import (
    SystemRoles,
    UserSession,
    get_current_user,
    RequireSuperAdmin,
    RequireStudioAdmin,
    RequireStudioStaff,
    RequireAnyVerifiedUser,
    verify_studio_ownership
)
from services.storage_portal import CloudStoragePortalManager

app = FastAPI(
    title="SnapVex Studio OS Backend API",
    description="Multi-Tenant Creative Studio ERP & Automated Unified Storage API Engine.",
    version="1.0.0"
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Simulated Database
MOCK_STUDIOS = {
    1: {"name": "Infinity Weddings Mumbai", "status": "Active", "storage_used": 1024.0, "storage_limit": 51200.0, "primary_provider": "Google Drive"},
    2: {"name": "Lumina High-Fashion Delhi", "status": "Pending", "storage_used": 0.0, "storage_limit": 51200.0, "primary_provider": "Platform Storage"}
}
MOCK_LEADS = [
    {"id": 1, "studio_id": 1, "name": "Aisha Sen & Kabir Wedding", "source": "Instagram", "status": "Interested", "value": 150000.0, "phone": "+919876543210"},
    {"id": 2, "studio_id": 1, "name": "Corporate Tech Annual Meetup", "source": "Website", "status": "New Lead", "value": 85000.0, "phone": "+919922334455"}
]
MOCK_EVENTS = [
    {"id": 101, "studio_id": 1, "name": "Aisha & Kabir Mehendi", "location": "Kempinski Mumbai", "status": "Active"}
]

# Client Configs for Portal Manager
GOOGLE_DRIVE_CONFIG = {"client_id": "GOOGLE_MOCK_CLIENT_ID", "client_secret": "GOOGLE_MOCK_CLIENT_SECRET"}
ONEDRIVE_CONFIG = {"client_id": "MS_MOCK_CLIENT_ID", "client_secret": "MS_MOCK_CLIENT_SECRET"}
storage_portal = CloudStoragePortalManager(GOOGLE_DRIVE_CONFIG, ONEDRIVE_CONFIG)


# Pydantic Schemas for Requests
class StudioRegisterRequest(BaseModel):
    name: str
    owner_email: EmailStr
    whatsapp_number: str
    primary_provider: str


class LeadCreateRequest(BaseModel):
    name: str
    source: str
    status: str
    value: float
    phone: str


# ==========================================
# 1. STUDIO REGISTRATION ENDPOINTS (Open / Super Admin)
# ==========================================

@app.post("/api/studios/register", status_code=status.HTTP_201_CREATED, tags=["Studios"])
def register_new_studio(req: StudioRegisterRequest):
    """
    Submits a registration request for a new Studio workspace.
    Funnels into the verification pending status awaiting Super Admin approval.
    """
    new_id = max(MOCK_STUDIOS.keys()) + 1
    MOCK_STUDIOS[new_id] = {
        "name": req.name,
        "status": "Pending",
        "storage_used": 0.0,
        "storage_limit": 51200.0,
        "primary_provider": req.primary_provider
    }
    return {
        "status": "Success",
        "detail": f"Registration sandbox for studio '{req.name}' initialized. Awaiting verification approve triggers.",
        "studio_id": new_id
    }


@app.post("/api/studios/{studio_id}/approve", tags=["Studios"])
def super_admin_approve_studio(studio_id: int = 1, admin: UserSession = Depends(RequireSuperAdmin)):
    """
    Approves a studio sandbox registration. Strictly restricted to Super Admins.
    """
    if studio_id not in MOCK_STUDIOS:
        raise HTTPException(status_code=404, detail="Studio entity not found.")
    
    MOCK_STUDIOS[studio_id]["status"] = "Active"
    return {
        "status": "Success",
        "detail": f"Studio '{MOCK_STUDIOS[studio_id]['name']}' has been officially verified by user: {admin.email}."
    }


# ==========================================
# 2. CRM LEADS MODULE ENDPOINTS (Studio Admin)
# ==========================================

@app.get("/api/leads", tags=["Leads"])
def get_studio_leads(
    studio_id: int, 
    user: UserSession = Depends(RequireStudioAdmin),
    authorized: bool = Depends(verify_studio_ownership)
):
    """
    Retrieves all sales CRM leads captured for the specific studio.
    Guarded by verification that user belongs to the requested studio.
    """
    leads = [lead for lead in MOCK_LEADS if lead["studio_id"] == studio_id]
    return {"studio_id": studio_id, "leads": leads}


@app.post("/api/leads", status_code=status.HTTP_201_CREATED, tags=["Leads"])
def capture_lead(
    studio_id: int,
    req: LeadCreateRequest,
    user: UserSession = Depends(RequireStudioAdmin),
    authorized: bool = Depends(verify_studio_ownership)
):
    """
    Injects a new client lead into the studio's active sales CRM pipeline.
    """
    new_lead_id = len(MOCK_LEADS) + 1
    new_lead = {
        "id": new_lead_id,
        "studio_id": studio_id,
        "name": req.name,
        "source": req.source,
        "status": req.status,
        "value": req.value,
        "phone": req.phone
    }
    MOCK_LEADS.append(new_lead)
    return {"status": "Success", "lead_id": new_lead_id, "lead": new_lead}


# ==========================================
# 3. EVENT MANAGEMENT ENDPOINTS (Staff or Studio Admin)
# ==========================================

@app.get("/api/events", tags=["Events"])
def get_studio_events(
    studio_id: int,
    user: UserSession = Depends(RequireStudioStaff),
    authorized: bool = Depends(verify_studio_ownership)
):
    """
    Lists event schedules configured for shooters, photographers, and editors inside the tenant.
    Accessed by verified studio staff or studio owner only.
    """
    events = [event for event in MOCK_EVENTS if event["studio_id"] == studio_id]
    return {"events": events}


# ==========================================
# 4. PLATFORM-AGNOSTIC FILE UPLOADS ENDPOINTS
# ==========================================

@app.post("/api/events/{event_id}/upload-media", tags=["Hybrid Cloud Storage"])
async def studio_staff_upload_raw_media(
    event_id: int,
    file: UploadFile = File(...),
    user: UserSession = Depends(RequireStudioStaff)
):
    """
    Accepts raw raw/jpeg media, automatically fetches the studio client's connected Cloud Drive provider,
    renews OAuth credentials on-the-fly, and completes a chunked upload into Google Drive or OneDrive.
    """
    # 1. Fetch Event and Studio config
    event = next((e for e in MOCK_EVENTS if e["id"] == event_id), None)
    if not event:
        raise HTTPException(status_code=404, detail="Shoot session event not found.")
        
    studio_id = event["studio_id"]
    if user.role != SystemRoles.SUPER_ADMIN and user.studio_id != studio_id:
        raise HTTPException(status_code=403, detail="Unprivileged access: Attempt to upload to another studio's event.")
        
    studio = MOCK_STUDIOS[studio_id]
    provider_name = studio["primary_provider"]
    
    # 2. Upload file contents
    file_bytes = await file.read()
    
    # In real flows: Fetch credential tokens from PostgreSQL/Supabase encrypt blocks.
    # Simulating long-lived refresh token replacement logic:
    simulated_token = "SIMULATED_DECRYPTED_ACCESS_TOKEN"
    
    try:
        prov = storage_portal.resolve_provider(provider_name)
        upload_result = prov.upload_file_stream(
            token=simulated_token,
            file_bytes=file_bytes,
            filename=file.filename,
            mime_type=file.content_type or "image/jpeg"
        )
        
        # Update Storage Limits
        file_size_mb = len(file_bytes) / (1024 * 1024)
        studio["storage_used"] += file_size_mb
        
        return {
            "status": "Success",
            "provider": provider_name,
            "filename": file.filename,
            "cloud_file_id": upload_result.get("id"),
            "cloud_webView_link": upload_result.get("url"),
            "file_size_mb": round(file_size_mb, 2)
        }
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Cloud Drive upload session failure via provider '{provider_name}': {str(e)}"
        )


@app.get("/health", tags=["Health"])
def health_check():
    return {"status": "healthy", "service": "snapvex-core-api"}
