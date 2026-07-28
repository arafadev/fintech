#!/usr/bin/env python3
"""Generate production-ready Postman Collection for FinTech Spring Boot API."""
import json
from pathlib import Path

COLLECTION_DESC = """# FinTech & Immutable Transaction Ledger API

Enterprise Spring Boot **4.1 / Java 21** banking API delivering customer onboarding, JWT-secured session management, account lifecycle, and money movement with an **append-only ledger**, **idempotent** transaction posting, and **i18n** error contracts.

## Architectural Purpose
- **Domain**: Retail banking operations (register/login, savings accounts, deposit / withdrawal / transfer).
- **Integrity**: Pessimistic row locks, optimistic `@Version`, double-entry transfer legs, immutable `ledger_entries`.
- **Risk controls**: Per-transaction and daily outgoing limits (`app.limits.*`).
- **Security**: Stateless JWT (`Authorization: Bearer`), token blacklist on logout, `tokenVersion` invalidation on password change, role-based `@PreAuthorize` (CUSTOMER / ADMIN / AUDITOR).

## Standard Headers
| Header | Required | Values / Notes |
|--------|----------|----------------|
| `Accept-Language` | Recommended | `en` (default) or `ar` — localizes `Response.message` and Bean Validation errors |
| `Authorization` | Protected routes | `Bearer <JWT>` |
| `Idempotency-Key` | `POST /transactions` | UUID ≤ 64 chars — prevents double-post on retries (also accepted in body) |
| `Content-Type` | Body requests | Runtime: `application/json` (collection form-data is auto-converted via pre-request script). File upload keeps `multipart/form-data`. |

## Unified Response Contract — `Response<T>`
```json
{
  "success": true,
  "statusCode": 200,
  "message": "…",
  "data": { },
  "meta": { "currentPage": 0, "totalItems": 0, "totalPages": 0, "pageSize": 50 },
  "timestamp": "2026-07-23T12:00:00Z"
}
```
Failures use the same envelope with `success: false` and HTTP status aligned to `statusCode` (400 / 401 / 403 / 404 / 409 / 500). Validation failures may place a field→message map in `data`.

## Roles
| Role | How obtained | Capabilities |
|------|----------------|--------------|
| `CUSTOMER` | Public register | Own accounts & transactions |
| `ADMIN` | Seeded once (`DataSeeder`) | Users list, roles CRUD, full audit |
| `AUDITOR` | Assigned manually | `/api/audit/**` read APIs |

## Collection Usage Notes
1. Set `baseUrl` (default `http://localhost:{{port}}/api`) and `port`.
2. Run **Auth → Login**, copy `data.token` into collection variable `accessToken`.
3. Toggle `acceptLanguage` between `en` and `ar` to verify i18n.
4. Body fields are documented as **form-data** with professional English descriptions derived from Bean Validation. A collection **pre-request script** serializes non-file form-data to JSON for Spring `@RequestBody` compatibility.
"""

PRE_REQUEST = """// Convert form-data documentation bodies to application/json for Spring @RequestBody
// File uploads (type=file) are left as multipart/form-data
if (pm.request.body && pm.request.body.mode === 'formdata') {
    const rows = pm.request.body.formdata.all();
    const hasFile = rows.some(r => r.type === 'file' && !r.disabled);
    if (!hasFile) {
        const payload = {};
        rows.filter(r => !r.disabled && r.type !== 'file').forEach(r => {
            let v = r.value;
            if (['amount', 'page', 'size', 'id'].includes(r.key) && v !== '' && !isNaN(Number(v))) {
                v = Number(v);
            }
            if (v !== undefined && v !== null && v !== '') {
                payload[r.key] = v;
            }
        });
        pm.request.headers.upsert({ key: 'Content-Type', value: 'application/json' });
        pm.request.body.update({
            mode: 'raw',
            raw: JSON.stringify(payload, null, 2),
            options: { raw: { language: 'json' } }
        });
    }
}
"""

def hdr(auth=False, idem=False, lang=True):
    h = []
    if lang:
        h.append({
            "key": "Accept-Language",
            "value": "{{acceptLanguage}}",
            "description": "Preferred response language. Accepted values: `en` (default) or `ar`. Localizes `Response.message` and Bean Validation error text."
        })
    if auth:
        h.append({
            "key": "Authorization",
            "value": "Bearer {{accessToken}}",
            "description": "JWT access token issued by Login. Format: `Bearer <token>`."
        })
    if idem:
        h.append({
            "key": "Idempotency-Key",
            "value": "{{$guid}}",
            "description": "Client-generated unique key (max 64 characters). Required for money-movement requests to prevent duplicate posting on retries. Also accepted in the request body as `idempotencyKey`."
        })
    return h

def form_field(key, value, desc, disabled=False):
    return {
        "key": key,
        "value": value,
        "type": "text",
        "description": desc,
        "disabled": disabled
    }

def resp(name, status, body_obj):
    return {
        "name": name,
        "originalRequest": {},
        "status": name.split("—")[0].strip() if "—" in name else name,
        "code": status,
        "_postman_previewlanguage": "json",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": json.dumps(body_obj, ensure_ascii=False, indent=2)
    }

def req(name, method, path, description, headers=None, formdata=None, query=None, responses=None, auth_needed=False):
    url_path = path.strip("/").split("/") if path.strip("/") else []
    item = {
        "name": name,
        "request": {
            "method": method,
            "header": headers or [],
            "url": {
                "raw": "{{baseUrl}}/" + path.lstrip("/"),
                "host": ["{{baseUrl}}"],
                "path": url_path,
            },
            "description": description
        },
        "response": responses or []
    }
    if query:
        item["request"]["url"]["query"] = query
        q = "&".join(f"{q['key']}={q['value']}" for q in query if not q.get("disabled"))
        item["request"]["url"]["raw"] = "{{baseUrl}}/" + path.lstrip("/") + ("?" + q if q else "")
    if formdata is not None:
        item["request"]["body"] = {
            "mode": "formdata",
            "formdata": formdata
        }
    if auth_needed:
        # already in headers
        pass
    return item

# --- Sample response bodies ---
OK_LOGIN = {
    "success": True,
    "statusCode": 200,
    "message": "Login successful",
    "data": {"token": "<jwt>", "roles": ["CUSTOMER"]},
    "timestamp": "2026-07-23T18:00:00Z"
}
OK_REGISTER = {
    "success": True,
    "statusCode": 200,
    "message": "Your account has been created successfully",
    "data": "Email of your account details has been sent to you. Your account number is: 6612345678",
    "timestamp": "2026-07-23T18:00:00Z"
}
ERR_400_VAL = {
    "success": False,
    "statusCode": 400,
    "message": "Validation failed",
    "data": {"email": "Invalid email format", "password": "Password must be between 8 and 30 characters"},
    "timestamp": "2026-07-23T18:00:00Z"
}
ERR_400_AR = {
    "success": False,
    "statusCode": 400,
    "message": "فشل التحقق من البيانات",
    "data": {"email": "صيغة البريد الإلكتروني غير صحيحة"},
    "timestamp": "2026-07-23T18:00:00Z"
}
ERR_401 = {
    "success": False,
    "statusCode": 401,
    "message": "Unauthorized",
    "timestamp": "2026-07-23T18:00:00Z"
}
ERR_403 = {
    "success": False,
    "statusCode": 403,
    "message": "Access denied",
    "timestamp": "2026-07-23T18:00:00Z"
}
ERR_404 = {
    "success": False,
    "statusCode": 404,
    "message": "Account not found",
    "timestamp": "2026-07-23T18:00:00Z"
}
OK_TXN = {
    "success": True,
    "statusCode": 200,
    "message": "Transaction successful",
    "data": {
        "id": 101,
        "amount": 100.00,
        "transactionType": "TRANSFER",
        "status": "SUCCESS",
        "entrySide": "DEBIT",
        "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "idempotencyKey": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    },
    "timestamp": "2026-07-23T18:00:00Z"
}
ERR_INSUFFICIENT = {
    "success": False,
    "statusCode": 400,
    "message": "Insufficient balance in source account",
    "timestamp": "2026-07-23T18:00:00Z"
}

collection = {
    "info": {
        "_postman_id": "f1n7ech-2026-07-23-vois-ready",
        "name": "FinTech Banking API — Production Collection",
        "description": COLLECTION_DESC,
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
        "version": "1.0.0"
    },
    "variable": [
        {"key": "port", "value": "8090", "type": "string"},
        {"key": "baseUrl", "value": "http://localhost:{{port}}/api", "type": "string"},
        {"key": "accessToken", "value": "", "type": "string"},
        {"key": "acceptLanguage", "value": "en", "type": "string"},
        {"key": "myAccountNumber", "value": "6612345678", "type": "string"},
        {"key": "destAccountNumber", "value": "6687654321", "type": "string"}
    ],
    "event": [
        {
            "listen": "prerequest",
            "script": {
                "type": "text/javascript",
                "exec": PRE_REQUEST.split("\n")
            }
        }
    ],
    "item": []
}

# ========== AUTH ==========
auth_folder = {
    "name": "01 — Auth (Public)",
    "description": "Public identity endpoints under `/api/auth/**` (permitAll). No JWT required.\nEndpoints: register, login, forgot-password, reset-password.",
    "item": [
        req(
            "Register Customer",
            "POST", "auth/register",
            "## Register\nCreates a **CUSTOMER** user + savings account. Roles cannot be supplied (privilege-escalation safe).\n\n**Auth:** None\n**DTO:** `RegisterRequest`",
            headers=hdr(auth=False),
            formdata=[
                form_field("firstName", "Ahmed", "Customer's first name. Required. Length: 2–50 characters (`@NotBlank`, `@Size`)."),
                form_field("lastName", "Hassan", "Customer's last name. Required. Length: 2–50 characters (`@NotBlank`, `@Size`)."),
                form_field("phoneNumber", "01012345678", "Primary Egyptian mobile number. Required. Must match pattern `^01[0125]\\d{8}$` (11 digits starting with 010, 011, 012, or 015)."),
                form_field("email", "ahmed.hassan@example.com", "Unique account email address. Required. Valid email format, max 100 characters (`@NotBlank`, `@Email`, `@Size`)."),
                form_field("password", "Secure@123", "Account password. Required. Length: 8–30 characters (`@NotBlank`, `@Size`)."),
            ],
            responses=[
                resp("200 OK — Registered", 200, OK_REGISTER),
                resp("400 Bad Request — Validation", 400, ERR_400_VAL),
                resp("400 Bad Request — Email already exists", 400, {
                    "success": False, "statusCode": 400,
                    "message": "Email already present", "timestamp": "2026-07-23T18:00:00Z"
                }),
            ]
        ),
        req(
            "Login",
            "POST", "auth/login",
            "## Login\nReturns JWT + role list. Inactive users are rejected.\n\n**DTO:** `LoginRequest`",
            headers=hdr(auth=False),
            formdata=[
                form_field("email", "ahmed.hassan@example.com", "Registered account email. Required. Must be a valid email format (`@NotBlank`, `@Email`)."),
                form_field("password", "Secure@123", "Plain-text password. Required (`@NotBlank`). Verified against the stored BCrypt hash."),
            ],
            responses=[
                resp("200 OK — Token issued", 200, OK_LOGIN),
                resp("400 Bad Request — Wrong password", 400, {
                    "success": False, "statusCode": 400,
                    "message": "Password doesn't match", "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("404 Not Found — Email", 404, {
                    "success": False, "statusCode": 404,
                    "message": "Email not found", "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("400 Bad Request — i18n (ar)", 400, ERR_400_AR),
            ]
        ),
        req(
            "Forgot Password",
            "POST", "auth/forgot-password",
            "## Forgot Password\nValidation group `Forgot`: email only. Sends reset link with raw code; DB stores SHA-256 hash.\n\n**DTO:** `ResetPasswordRequest` (Forgot group)",
            headers=hdr(auth=False),
            formdata=[
                form_field("email", "ahmed.hassan@example.com", "Registered account email used to receive the reset link. Required for the Forgot validation group (`@NotBlank`, `@Email`)."),
            ],
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Password reset code sent to your email",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("404 Not Found", 404, {
                    "success": False, "statusCode": 404,
                    "message": "User not found", "timestamp": "2026-07-23T18:00:00Z"
                }),
            ]
        ),
        req(
            "Reset Password",
            "POST", "auth/reset-password",
            "## Reset Password\nValidation group `Reset`: `code` + `newPassword`. Bumps `tokenVersion` (invalidates old JWTs).\n\n**DTO:** `ResetPasswordRequest` (Reset group)",
            headers=hdr(auth=False),
            formdata=[
                form_field("code", "a1b2c3d4e5f6789012345678abcdef01", "Raw password-reset code from the email link. Required for the Reset validation group. Length: 5–64 characters (`@NotBlank`, `@Size`)."),
                form_field("newPassword", "NewSecure@456", "New account password. Required. Length: 8–30 characters (`@NotBlank`, `@Size`)."),
            ],
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Password updated successfully",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("400 Bad Request — Invalid / expired code", 400, {
                    "success": False, "statusCode": 400,
                    "message": "Invalid reset code", "timestamp": "2026-07-23T18:00:00Z"
                }),
            ]
        ),
    ]
}

# ========== USERS ==========
users_folder = {
    "name": "02 — Users",
    "description": "Authenticated user profile, password change, logout (blacklist), profile picture, admin user listing.",
    "item": [
        req(
            "Get My Profile",
            "GET", "users/me",
            "## Get Current User Profile\n**Auth:** Bearer JWT\nReturns `UserDto` (password never serialized).",
            headers=hdr(auth=True),
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "User retrieved",
                    "data": {
                        "id": 10, "firstName": "Ahmed", "lastName": "Hassan",
                        "email": "ahmed.hassan@example.com", "phoneNumber": "01012345678",
                        "active": True, "roles": ["CUSTOMER"]
                    },
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("401 Unauthorized", 401, ERR_401),
            ]
        ),
        req(
            "List All Users (ADMIN)",
            "GET", "users",
            "## Paginated Users\n**Auth:** ADMIN (`@PreAuthorize`)\nQuery: `page` (default 0), `size` (default 50).",
            headers=hdr(auth=True),
            query=[
                {"key": "page", "value": "0", "description": "Zero-based page index. Default: `0`."},
                {"key": "size", "value": "50", "description": "Number of records per page. Default: `50`."},
            ],
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Users retrieved",
                    "data": {"content": [], "totalElements": 0, "number": 0, "size": 50},
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("403 Forbidden", 403, ERR_403),
            ]
        ),
        req(
            "Update Password",
            "PUT", "users/me/password",
            "## Change Password (authenticated)\n**DTO:** `UpdatePasswordRequest`\nIncrements `tokenVersion` and blacklists session via subsequent logout if needed.",
            headers=hdr(auth=True),
            formdata=[
                form_field("oldPassword", "Secure@123", "Current password for verification. Required (`@NotBlank`)."),
                form_field("newPassword", "Newer@78901", "New password to set. Required (`@NotBlank`). Stored after BCrypt encoding."),
            ],
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Password changed successfully",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("400 Bad Request — Old password incorrect", 400, {
                    "success": False, "statusCode": 400,
                    "message": "Old password not correct",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
            ]
        ),
        req(
            "Logout (Blacklist Token)",
            "POST", "users/me/logout",
            "## Logout\nRequires `Authorization` header. Adds JWT to in-memory blacklist until natural expiry.",
            headers=hdr(auth=True),
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Logged out successfully",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("400 Bad Request — Missing Authorization", 400, {
                    "success": False, "statusCode": 400,
                    "message": "Missing or invalid Authorization header",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
            ]
        ),
        {
            "name": "Upload Profile Picture",
            "request": {
                "method": "PUT",
                "header": hdr(auth=True),
                "body": {
                    "mode": "formdata",
                    "formdata": [
                        {
                            "key": "file",
                            "type": "file",
                            "src": [],
                            "description": "Profile image file. Multipart form field named `file` (`@RequestParam(\"file\")`). Remains true `multipart/form-data` (not converted to JSON)."
                        }
                    ]
                },
                "url": {
                    "raw": "{{baseUrl}}/users/me/profile-picture",
                    "host": ["{{baseUrl}}"],
                    "path": ["users", "me", "profile-picture"]
                },
                "description": "## Upload Profile Picture\nTrue `multipart/form-data`. Pre-request script skips JSON conversion when a file field is present."
            },
            "response": [
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Profile picture uploaded successfully",
                    "data": "profile-picture/9f3c2a1b-....jpg",
                    "timestamp": "2026-07-23T18:00:00Z"
                })
            ]
        },
    ]
}

# ========== ACCOUNTS ==========
accounts_folder = {
    "name": "03 — Accounts",
    "description": "Customer account listing and close-account (zero balance required). Account numbers use configurable prefix (default `66`).",
    "item": [
        req(
            "Get My Accounts",
            "GET", "accounts/me",
            "## List accounts for the authenticated user",
            headers=hdr(auth=True),
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "User accounts fetched successfully",
                    "data": [{
                        "accountNumber": "6612345678",
                        "balance": 1500.00,
                        "accountType": "SAVINGS",
                        "currency": "USD",
                        "status": "ACTIVE"
                    }],
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("401 Unauthorized", 401, ERR_401),
            ]
        ),
        req(
            "Close Account",
            "DELETE", "accounts/{{myAccountNumber}}",
            "## Close Account\n**Path:** `accountNumber`\nRules: must belong to caller; balance must be **zero**; status → `CLOSED`.",
            headers=hdr(auth=True),
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Account closed successfully",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("400 Bad Request — Non-zero balance", 400, {
                    "success": False, "statusCode": 400,
                    "message": "Account balance must be zero before closing",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("404 Not Found", 404, ERR_404),
            ]
        ),
    ]
}

# ========== TRANSACTIONS ==========
txn_common_amount = form_field(
    "amount", "100.00",
    "Transaction amount. Required. Minimum `0.01`; max 17 integer digits and 2 fractional digits (`@NotNull`, `@DecimalMin`, `@Digits`)."
)
txn_account = form_field(
    "accountNumber", "{{myAccountNumber}}",
    "Source account number owned by the authenticated user. Required. Length: 10–15 characters (`@NotBlank`, `@Size`)."
)
txn_desc = form_field(
    "description", "Salary top-up / peer transfer",
    "Optional free-text note for the transaction. Max 255 characters (`@Size`)."
)
txn_idem = form_field(
    "idempotencyKey", "{{$guid}}",
    "Client-generated idempotency key. Required. Max 64 characters (`@NotBlank`, `@Size`). Prefer sending the same value in the `Idempotency-Key` header."
)

transactions_folder = {
    "name": "04 — Transactions & Money Movement",
    "description": (
        "Core fintech posting API. Uses pessimistic locks, ownership checks, CLOSED/SUSPENDED guards, "
        "daily/max limits, double-entry transfers, and ledger append in the same DB transaction. "
        "Emails fire **after commit** via `AfterCommit` + `@Async`.\n\n"
        "**Always send `Idempotency-Key` header** (collection includes it)."
    ),
    "item": [
        req(
            "Deposit",
            "POST", "transactions",
            "## Deposit\n`transactionType=DEPOSIT`. Credits **own** account only (`assertOwnership`).",
            headers=hdr(auth=True, idem=True),
            formdata=[
                form_field("transactionType", "DEPOSIT", "Transaction type enum. Required (`@NotNull`). Allowed values: `DEPOSIT`, `WITHDRAWAL`, `TRANSFER`."),
                txn_common_amount,
                txn_account,
                txn_desc,
                txn_idem,
            ],
            responses=[
                resp("200 OK", 200, {**OK_TXN, "data": {**OK_TXN["data"], "transactionType": "DEPOSIT", "entrySide": "CREDIT"}}),
                resp("400 Bad Request — Validation", 400, ERR_400_VAL),
                resp("400 Bad Request — Not owner / closed", 400, {
                    "success": False, "statusCode": 400,
                    "message": "Account does not belong to the authenticated user",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
            ]
        ),
        req(
            "Withdrawal",
            "POST", "transactions",
            "## Withdrawal\n`transactionType=WITHDRAWAL`. Debits own account; enforces max-single + daily outgoing limits.",
            headers=hdr(auth=True, idem=True),
            formdata=[
                form_field("transactionType", "WITHDRAWAL", "Must be `WITHDRAWAL` for this operation (`TransactionType`, `@NotNull`)."),
                form_field("amount", "50.00", "Withdrawal amount. Required. Min `0.01`, max 2 decimal places. Also subject to `app.limits.max-single-amount` and daily outgoing limits."),
                txn_account,
                form_field("description", "ATM cash-out", "Optional free-text note. Max 255 characters (`@Size`)."),
                txn_idem,
            ],
            responses=[
                resp("200 OK", 200, {**OK_TXN, "data": {**OK_TXN["data"], "transactionType": "WITHDRAWAL", "entrySide": "DEBIT", "amount": 50.00}}),
                resp("400 Bad Request — Insufficient balance", 400, {
                    "success": False, "statusCode": 400,
                    "message": "Insufficient balance", "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("400 Bad Request — Limit exceeded", 400, {
                    "success": False, "statusCode": 400,
                    "message": "Amount exceeds maximum allowed per transaction: 10000.00",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
            ]
        ),
        req(
            "Transfer (Double-Entry)",
            "POST", "transactions",
            "## Transfer\nCreates DEBIT leg on source + CREDIT leg on destination (`correlationId` shared). Destination need not be owned; source must be.",
            headers=hdr(auth=True, idem=True),
            formdata=[
                form_field("transactionType", "TRANSFER", "Must be `TRANSFER` for this operation (`TransactionType`, `@NotNull`)."),
                form_field("amount", "250.50", "Transfer amount. Required. Min `0.01`; banking scale with max 2 decimal places (`@NotNull`, `@DecimalMin`, `@Digits`)."),
                form_field("accountNumber", "{{myAccountNumber}}", "Source account number owned by the caller. Required. Length: 10–15 characters (`@NotBlank`, `@Size`)."),
                form_field("destinationAccountNumber", "{{destAccountNumber}}", "Destination account number. Required for transfers. Length: 10–15 characters when provided (`@Size`)."),
                form_field("description", "Rent payment to landlord", "Optional free-text note. Max 255 characters (`@Size`)."),
                txn_idem,
            ],
            responses=[
                resp("200 OK — Debit leg returned", 200, OK_TXN),
                resp("200 OK — Idempotent replay", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Transaction already processed",
                    "data": {"id": 101, "idempotencyKey": "…"},
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("400 Bad Request — Insufficient source balance", 400, ERR_INSUFFICIENT),
                resp("404 Not Found — Destination", 404, {
                    "success": False, "statusCode": 404,
                    "message": "Destination account not found",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
            ]
        ),
        req(
            "Get Transactions For My Account (Paginated)",
            "GET", "accounts/{{myAccountNumber}}/transactions",
            "## Account statement\nOwnership enforced. Returns `data` list + `meta` pagination.",
            headers=hdr(auth=True),
            query=[
                {"key": "page", "value": "0", "description": "Zero-based page index. Default: `0`."},
                {"key": "size", "value": "50", "description": "Number of records per page. Default: `50`."},
            ],
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Transactions retrieved",
                    "data": [OK_TXN["data"]],
                    "meta": {"currentPage": 0, "totalItems": 1, "totalPages": 1, "pageSize": 50},
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("404 Not Found", 404, ERR_404),
                resp("400 Bad Request — Not owner", 400, {
                    "success": False, "statusCode": 400,
                    "message": "Account does not belong to the authenticated user",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
            ]
        ),
    ]
}

# ========== ROLES ==========
roles_folder = {
    "name": "05 — Roles (ADMIN)",
    "description": "Role CRUD — class-level `@PreAuthorize(\"hasAuthority('ADMIN')\")`. Entity `Role`: `id`, `name` (`@NotBlank`, unique).",
    "item": [
        req(
            "Create Role",
            "POST", "roles",
            "## Create Role\n**DTO/Entity:** `Role`",
            headers=hdr(auth=True),
            formdata=[
                form_field("name", "AUDITOR", "Role name. Required and unique (`@NotBlank`). Typical values: `ADMIN`, `CUSTOMER`, `AUDITOR`."),
            ],
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Role saved successfully",
                    "data": {"id": 3, "name": "AUDITOR"},
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("409 Conflict — Exists", 409, {
                    "success": False, "statusCode": 409,
                    "message": "Role already exists",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("403 Forbidden", 403, ERR_403),
            ]
        ),
        req(
            "Update Role",
            "PUT", "roles",
            "## Update Role",
            headers=hdr(auth=True),
            formdata=[
                form_field("id", "3", "Existing role primary key (`Long`). Required for update."),
                form_field("name", "AUDITOR_LEAD", "Updated role name. Required and must remain unique (`@NotBlank`)."),
            ],
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Role updated successfully",
                    "data": {"id": 3, "name": "AUDITOR_LEAD"},
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("404 Not Found", 404, {
                    "success": False, "statusCode": 404,
                    "message": "Role not found",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
            ]
        ),
        req(
            "List Roles",
            "GET", "roles",
            "## List all roles (small catalog — full list OK)",
            headers=hdr(auth=True),
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Roles retrieved successfully",
                    "data": [
                        {"id": 1, "name": "ADMIN"},
                        {"id": 2, "name": "CUSTOMER"},
                        {"id": 3, "name": "AUDITOR"}
                    ],
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
            ]
        ),
        req(
            "Delete Role",
            "DELETE", "roles/3",
            "## Delete Role by id",
            headers=hdr(auth=True),
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Role deleted successfully",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("404 Not Found", 404, {
                    "success": False, "statusCode": 404,
                    "message": "Role not found",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
            ]
        ),
    ]
}

# ========== AUDIT ==========
audit_folder = {
    "name": "06 — Audit / Ledger Read APIs (ADMIN | AUDITOR)",
    "description": (
        "Read-only supervisory APIs. All methods require `ADMIN` or `AUDITOR`. "
        "Responses use unified `Response<T>`. Transaction history is pageable. "
        "Ledger write path is internal (`LedgerService.appendEntries`) — no public write endpoint by design (immutability)."
    ),
    "item": [
        req(
            "System Totals",
            "GET", "audit/totals",
            "## Aggregate counters (users / accounts / transactions)",
            headers=hdr(auth=True),
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "System totals",
                    "data": {"users": 120, "accounts": 130, "transactions": 5400},
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("403 Forbidden", 403, ERR_403),
            ]
        ),
        req(
            "Find User By Email",
            "GET", "audit/users",
            "## Lookup user DTO by email",
            headers=hdr(auth=True),
            query=[
                {"key": "email", "value": "ahmed.hassan@example.com",
                 "description": "Exact registered email address used for the supervisory user lookup."},
            ],
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "User retrieved",
                    "data": {"id": 10, "email": "ahmed.hassan@example.com", "roles": ["CUSTOMER"]},
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("404 Not Found", 404, {
                    "success": False, "statusCode": 404,
                    "message": "User not found",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
            ]
        ),
        req(
            "Find Account Details",
            "GET", "audit/accounts/{{myAccountNumber}}",
            "## Account details by account number (supervisory)",
            headers=hdr(auth=True),
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Account retrieved",
                    "data": {
                        "accountNumber": "6612345678",
                        "balance": 1500.00,
                        "status": "ACTIVE",
                        "accountType": "SAVINGS"
                    },
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("404 Not Found", 404, ERR_404),
            ]
        ),
        req(
            "Transactions By Account (Paginated)",
            "GET", "audit/accounts/{{myAccountNumber}}/transactions",
            "## Full statement for any account (auditor scope) with pagination meta",
            headers=hdr(auth=True),
            query=[
                {"key": "page", "value": "0", "description": "Zero-based page index. Default: `0`."},
                {"key": "size", "value": "50", "description": "Number of records per page. Default: `50`."},
            ],
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Transactions retrieved",
                    "data": [OK_TXN["data"]],
                    "meta": {"currentPage": 0, "totalItems": 1, "totalPages": 1, "pageSize": 50},
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
            ]
        ),
        req(
            "Transaction By Id",
            "GET", "audit/transactions/101",
            "## Fetch single transaction DTO by database id",
            headers=hdr(auth=True),
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Transaction retrieved",
                    "data": OK_TXN["data"],
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
                resp("404 Not Found", 404, {
                    "success": False, "statusCode": 404,
                    "message": "Transaction not found",
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
            ]
        ),
    ]
}

# Ledger informational folder (no public write API)
ledger_folder = {
    "name": "07 — Ledger (Design Notes)",
    "description": (
        "## Immutable Ledger\n"
        "There is **no public HTTP write API** for ledger entries by design. "
        "`LedgerService.appendEntries` is invoked inside the same `@Transactional` boundary as money movement. "
        "Columns are `updatable=false`; optional DB grants: `src/main/resources/db/ledger_grants.sql`.\n\n"
        "Auditors observe financial history via **06 — Audit** transaction queries (which correlate to ledger via `transactionId` / `correlationId`)."
    ),
    "item": [
        req(
            "[Reference] Read transactions via Audit (ledger-correlated)",
            "GET", "audit/accounts/{{myAccountNumber}}/transactions",
            "Use Audit folder for read models. This item is a cross-link for architects reviewing ledger observability.",
            headers=hdr(auth=True),
            query=[
                {"key": "page", "value": "0", "description": "Zero-based page index. Default: `0`."},
                {"key": "size", "value": "50", "description": "Number of records per page. Default: `50`."},
            ],
            responses=[
                resp("200 OK", 200, {
                    "success": True, "statusCode": 200,
                    "message": "Transactions retrieved",
                    "data": [],
                    "meta": {"currentPage": 0, "totalItems": 0, "totalPages": 0, "pageSize": 50},
                    "timestamp": "2026-07-23T18:00:00Z"
                }),
            ]
        )
    ]
}

collection["item"] = [
    auth_folder,
    users_folder,
    accounts_folder,
    transactions_folder,
    roles_folder,
    audit_folder,
    ledger_folder,
]

out = Path(r"J:\spring\spring_projects\fintech\postman_collection.json")
out.write_text(json.dumps(collection, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"Wrote {out} ({out.stat().st_size} bytes)")
