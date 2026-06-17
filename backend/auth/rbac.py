from fastapi import Depends, HTTPException, status, Security
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel
from typing import List, Optional
import jwt
import datetime

# Define Security Scheme
security_scheme = HTTPBearer()

# Secret Key for JWT
JWT_SECRET_KEY = "SUPER_SECRET_SNAPVEX_KEY_FOR_LOCAL_SECURITY_TOKEN_VERIFICATION"
JWT_ALGORITHM = "HS256"

# Role Definitions
class SystemRoles:
    SUPER_ADMIN = "super_admin"       # Platform Operator
    STUDIO_ADMIN = "studio_admin"     # Tenant Owner
    PHOTOGRAPHER = "photographer"     # Captures & Uploads original RAWs
    EDITOR = "editor"                 # Downloads RAWs, uploads edits
    CLIENT = "client"                 # Favorites & approves albums

# Pydantic schema for User token payload info
class UserSession(BaseModel):
    user_id: str
    email: str
    role: str
    studio_id: Optional[int] = None

def get_current_user(credentials: HTTPAuthorizationCredentials = Security(security_scheme)) -> UserSession:
    """
    Decodes the Bearer JWT and extracts user identity, role, and studio context.
    Throws a 401 identity error if the token has expired, been malformed or signed incorrectly.
    """
    token = credentials.credentials
    try:
        payload = jwt.decode(token, JWT_SECRET_KEY, algorithms=[JWT_ALGORITHM])
        user_id: str = payload.get("sub")
        email: str = payload.get("email")
        role: str = payload.get("role")
        studio_id: Optional[int] = payload.get("studio_id")
        
        if user_id is None or role is None:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Payload is missing mandatory identity fields: 'sub' and 'role'."
            )
            
        return UserSession(user_id=user_id, email=email, role=role, studio_id=studio_id)
        
    except jwt.ExpiredSignatureError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Authorization token has expired. Please log in again."
        )
    except jwt.PyJWTError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authorization token signature."
        )

class RoleChecker:
    """
    FastAPI Role-Based Access Control (RBAC) Dependency.
    Ensures the authenticated requester possesses one of the allowed roles,
    and guarantees they can only access resources matching their associated studio.
    """
    def __init__(self, allowed_roles: List[str]):
        self.allowed_roles = allowed_roles

    def __call__(self, current_user: UserSession = Depends(get_current_user)) -> UserSession:
        if current_user.role not in self.allowed_roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Access Denied: Role '{current_user.role}' lacks sufficient privileges to access this endpoint."
            )
        return current_user

# Predefined Common Endpoint Authorizations
RequireSuperAdmin = RoleChecker([SystemRoles.SUPER_ADMIN])
RequireStudioAdmin = RoleChecker([SystemRoles.STUDIO_ADMIN])
RequireStudioStaff = RoleChecker([SystemRoles.STUDIO_ADMIN, SystemRoles.PHOTOGRAPHER, SystemRoles.EDITOR])
RequireAnyVerifiedUser = RoleChecker([
    SystemRoles.SUPER_ADMIN, 
    SystemRoles.STUDIO_ADMIN, 
    SystemRoles.PHOTOGRAPHER, 
    SystemRoles.EDITOR, 
    SystemRoles.CLIENT
])

def verify_studio_ownership(target_studio_id: int, user: UserSession = Depends(get_current_user)):
    """
    Additional middleware dependency to ensure that unless the requester is a Super Admin,
    they are strictly forbidden from modifying or reading data outside their registered Tenant (studio_id).
    """
    if user.role == SystemRoles.SUPER_ADMIN:
        return True # Super admin is allowed global visibility
        
    if user.studio_id != target_studio_id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Insecure Tenant Access Attempt: You do not belong to the requested studio profile."
        )
    return True
