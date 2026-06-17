from abc import ABC, abstractmethod
from typing import Optional, Dict, Any
import httpx
from google_auth_oauthlib.flow import Flow
from googleapiclient.discovery import build
from google.oauth2.credentials import Credentials
import msal

class CloudStorageProvider(ABC):
    """
    Unified abstract interface for platform-agnostic media uploads and OAuth2 lifecycle management.
    Hides differences between Google Drive (Google APIs) and Microsoft OneDrive (Graph APIs).
    """

    @abstractmethod
    def get_auth_url(self, redirect_uri: str, state: Optional[str] = None) -> str:
        """Generates the OAuth2 auth login url to retrieve access codes."""
        pass

    @abstractmethod
    def exchange_code_for_token(self, code: str, redirect_uri: str) -> Dict[str, Any]:
        """Exchanges single-use authorization codes for long-lived OAuth tokens."""
        pass

    @abstractmethod
    def refresh_token(self, refresh_token: str) -> Dict[str, Any]:
        """Refreshes expired access tokens to keep background uploading flows continuous."""
        pass

    @abstractmethod
    def upload_file_stream(
        self, 
        token: str, 
        file_bytes: bytes, 
        filename: str, 
        mime_type: str, 
        parent_folder_id: Optional[str] = None
    ) -> Dict[str, Any]:
        """Uploads a binary media file stream into the cloud directory."""
        pass

    @abstractmethod
    def create_directory(self, token: str, folder_name: str, parent_folder_id: Optional[str] = None) -> str:
        """Creates a virtual subdirectory inside the workspace, returning its unique remote ID."""
        pass


class GoogleDriveProvider(CloudStorageProvider):
    """
    Google Drive API integration using official Google API libraries.
    """
    def __init__(self, client_id: str, client_secret: str):
        self.client_id = client_id
        self.client_secret = client_secret
        self.scopes = ["https://www.googleapis.com/auth/drive.file"]

    def _get_client_config(self) -> Dict[str, Any]:
        return {
            "web": {
                "client_id": self.client_id,
                "client_secret": self.client_secret,
                "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                "token_uri": "https://oauth2.googleapis.com/token",
            }
        }

    def get_auth_url(self, redirect_uri: str, state: Optional[str] = None) -> str:
        flow = Flow.from_client_config(self._get_client_config(), scopes=self.scopes)
        flow.redirect_uri = redirect_uri
        auth_url, _ = flow.authorization_url(
            access_type="offline",
            prompt="consent",
            state=state
        )
        return auth_url

    def exchange_code_for_token(self, code: str, redirect_uri: str) -> Dict[str, Any]:
        flow = Flow.from_client_config(self._get_client_config(), scopes=self.scopes)
        flow.redirect_uri = redirect_uri
        flow.fetch_token(code=code)
        creds = flow.credentials
        return {
            "access_token": creds.token,
            "refresh_token": creds.refresh_token,
            "expires_at": creds.expiry.isoformat() if creds.expiry else None,
            "provider": "Google Drive"
        }

    def refresh_token(self, refresh_token: str) -> Dict[str, Any]:
        creds = Credentials(
            token=None,
            refresh_token=refresh_token,
            token_uri="https://oauth2.googleapis.com/token",
            client_id=self.client_id,
            client_secret=self.client_secret
        )
        # Using Google official request refresh method
        from google.auth.transport.requests import Request
        creds.refresh(Request())
        return {
            "access_token": creds.token,
            "expires_at": creds.expiry.isoformat() if creds.expiry else None
        }

    def create_directory(self, token: str, folder_name: str, parent_folder_id: Optional[str] = None) -> str:
        creds = Credentials(token=token)
        service = build("drive", "v3", credentials=creds)
        
        file_metadata = {
            "name": folder_name,
            "mimeType": "application/vnd.google-apps.folder"
        }
        if parent_folder_id:
            file_metadata["parents"] = [parent_folder_id]
            
        folder = service.files().create(body=file_metadata, fields="id").execute()
        return folder.get("id")

    def upload_file_stream(
        self, 
        token: str, 
        file_bytes: bytes, 
        filename: str, 
        mime_type: str, 
        parent_folder_id: Optional[str] = None
    ) -> Dict[str, Any]:
        # Utilizing google-api-client MediaIoBaseUpload for platform stream uploads
        from io import BytesIO
        from googleapiclient.http import MediaIoBaseUpload
        
        creds = Credentials(token=token)
        service = build("drive", "v3", credentials=creds)
        
        file_metadata = {"name": filename}
        if parent_folder_id:
            file_metadata["parents"] = [parent_folder_id]
            
        media = MediaIoBaseUpload(BytesIO(file_bytes), mimetype=mime_type, resumable=True)
        uploaded = service.files().create(
            body=file_metadata,
            media_body=media,
            fields="id, webViewLink, size"
        ).execute()
        
        return {
            "id": uploaded.get("id"),
            "url": uploaded.get("webViewLink"),
            "size_bytes": uploaded.get("size")
        }


class OneDriveProvider(CloudStorageProvider):
    """
    Microsoft OneDrive / SharePoint Storage Platform Provider via Microsoft Graph API.
    """
    def __init__(self, client_id: str, client_secret: str, tenant_id: str = "common"):
        self.client_id = client_id
        self.client_secret = client_secret
        self.tenant_id = tenant_id
        self.scopes = ["Files.ReadWrite.All", "offline_access"]
        self.msal_app = msal.ConfidentialClientApplication(
            client_id=self.client_id,
            client_credential=self.client_secret,
            authority=f"https://login.microsoftonline.com/{self.tenant_id}"
        )

    def get_auth_url(self, redirect_uri: str, state: Optional[str] = None) -> str:
        return self.msal_app.get_authorization_request_url(
            scopes=self.scopes,
            redirect_uri=redirect_uri,
            state=state
        )

    def exchange_code_for_token(self, code: str, redirect_uri: str) -> Dict[str, Any]:
        result = self.msal_app.acquire_token_by_authorization_code(
            code=code,
            scopes=self.scopes,
            redirect_uri=redirect_uri
        )
        if "error" in result:
            raise Exception(f"OneDrive token exchange failed: {result.get('error_description')}")
            
        return {
            "access_token": result.get("access_token"),
            "refresh_token": result.get("refresh_token"),
            "expires_in": result.get("expires_in"),
            "provider": "OneDrive"
        }

    def refresh_token(self, refresh_token: str) -> Dict[str, Any]:
        result = self.msal_app.acquire_token_by_refresh_token(
            refresh_token=refresh_token,
            scopes=self.scopes
        )
        if "error" in result:
            raise Exception(f"OneDrive token refresh failed: {result.get('error_description')}")
            
        return {
            "access_token": result.get("access_token"),
            "refresh_token": result.get("refresh_token") # might rotate refresh tokens
        }

    def create_directory(self, token: str, folder_name: str, parent_folder_id: Optional[str] = None) -> str:
        # Microsoft Graph folder mapping
        url = "https://graph.microsoft.com/v1.0/me/drive/root/children"
        if parent_folder_id:
            url = f"https://graph.microsoft.com/v1.0/me/drive/items/{parent_folder_id}/children"
            
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }
        data = {
            "name": folder_name,
            "folder": {},
            "@microsoft.graph.conflictBehavior": "rename"
        }
        
        with httpx.Client() as client:
            resp = client.post(url, headers=headers, json=data)
            resp.raise_for_status()
            return resp.json().get("id")

    def upload_file_stream(
        self, 
        token: str, 
        file_bytes: bytes, 
        filename: str, 
        mime_type: str, 
        parent_folder_id: Optional[str] = None
    ) -> Dict[str, Any]:
        # Microsoft Graph uses Upload Session API for files > 4MB to prevent timeouts
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }
        
        # Define upload endpoint URI
        if parent_folder_id:
            session_url = f"https://graph.microsoft.com/v1.0/me/drive/items/{parent_folder_id}:/{filename}:/createUploadSession"
        else:
            session_url = f"https://graph.microsoft.com/v1.0/me/drive/root:/{filename}:/createUploadSession"
            
        with httpx.Client() as client:
            session_resp = client.post(session_url, headers=headers, json={})
            session_resp.raise_for_status()
            upload_url = session_resp.json().get("uploadUrl")
            
            # Execute upload chunking
            total_size = len(file_bytes)
            content_range = f"bytes 0-{total_size - 1}/{total_size}"
            
            upload_headers = {
                "Content-Length": str(total_size),
                "Content-Range": content_range
            }
            
            resp = client.put(upload_url, headers=upload_headers, content=file_bytes)
            resp.raise_for_status()
            info = resp.json()
            
            return {
                "id": info.get("id"),
                "url": info.get("webUrl"),
                "size_bytes": info.get("size")
            }


class CloudStoragePortalManager:
    """
    Multi-tenant Storage Portal Coordinator.
    Loads client credentials dynamically depending on the active studio's selection.
    """
    def __init__(self, google_config: dict, onedrive_config: dict):
        self.google_provider = GoogleDriveProvider(**google_config)
        self.onedrive_provider = OneDriveProvider(**onedrive_config)

    def resolve_provider(self, provider_name: str) -> CloudStorageProvider:
        if provider_name == "Google Drive":
            return self.google_provider
        elif provider_name == "OneDrive":
            return self.onedrive_provider
        else:
            raise ValueError(f"Unknown storage provider client requested: {provider_name}")
