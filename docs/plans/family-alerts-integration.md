# Family Alerts — Backend Integration Plan

**Date:** 2026-08-16  
**Feature:** Display per-family-member safety alerts on the Product Details screen  
**Triggered by:** Backend added `familyAlerts` array inside `foodSafetyResponse` on `GET /api/v1/scans/{scanId}` and `PATCH /api/v1/scans/{scanId}`

---

## Background

Backend added a `familyAlerts` array inside `foodSafetyResponse` in both:
- `GET /api/v1/scans/{scanId}`
- `PATCH /api/v1/scans/{scanId}`

Each alert identifies a family member who is at risk from this product:

```json
"familyAlerts": [
  {
    "targetProfile": "Ahmed (Son)",
    "severity": "UNSAFE",
    "reason": "Contains tree nuts — allergic per profile"
  }
]
```

> ⚠️ **Health-critical:** This data must never be silently dropped — a missing family alert that should show "Ahmed can't eat this" is a safety failure.

---

## Schema (New fields only)

```
FoodSafetyResponseDto
└── familyAlerts: List<FamilyAlertDto>   ← NEW
        ├── targetProfile: String
        ├── severity: String  ("UNSAFE" | "CAUTION" | "SAFE")
        └── reason: String

FoodSafetyResponse  (domain)
└── familyAlerts: List<FamilyAlert>      ← NEW

FamilyAlert  (new domain model)
    ├── targetProfile: String
    ├── severity: ProductVerdict
    └── reason: String

ProductDetail
└── familyAlerts: List<FamilyAlert>      ← NEW
```

---

## Changes by Layer

### Layer 1 — Data: DTO
**File:** `data/.../dto/FoodSafetyResponseDto.kt`
- Add `FamilyAlertDto(targetProfile, severity, reason)`
- Add `familyAlerts: List<FamilyAlertDto> = emptyList()` to `FoodSafetyResponseDto`

### Layer 2 — Domain: Models
**New:** `domain/.../scan/model/FamilyAlert.kt`
- `data class FamilyAlert(targetProfile, severity: ProductVerdict, reason)`

**Modify:** `FoodSafetyResponse.kt`
- Add `familyAlerts: List<FamilyAlert>`

**Modify:** `ProductDetail.kt`
- Add `familyAlerts: List<FamilyAlert> = emptyList()`

### Layer 3 — Data: Mapper
**File:** `data/.../repository/mapper/ScanMapper.kt`
- Add private `FamilyAlertDto.toDomain()` using `mapVerdict()` for severity
- Update `FoodSafetyResponseDto.toDomain()` to pass `familyAlerts`

### Layer 4 — Presentation: ViewModel
**File:** `ProductDetailsViewModel.kt`
- `mapToProductDetail()`: map `familyAlerts`
- `mapToScanResult()`: round-trip `familyAlerts` back
- Fix: fully-qualified `GetScanResultUseCase` → use import (AGENTS.md rule)

### Layer 5 — Presentation: New UI Component
**New:** `product_details/view/components/FamilyAlertsSection.kt`
- Section title: "Family Alerts"
- Card per alert: avatar initial + profile name, severity badge, reason text
- Severity colors: UNSAFE→Red, CAUTION→Yellow, SAFE→Green

### Layer 6 — Presentation: Screen
**File:** `ProductDetailsScreen.kt`
- Inject `FamilyAlertsSection` after `FlaggedIngredientsRow` (guard: `isNotEmpty()`)

### Layer 7 — Resources
**File:** `strings.xml`
- `product_details_family_alerts_title` → "Family Alerts"
- `product_details_family_alert_severity_unsafe` → "Unsafe"
- `product_details_family_alert_severity_caution` → "Caution"
- `product_details_family_alert_severity_safe` → "Safe"

---

## Verification

1. Build & run → navigate to a completed scan
2. If backend returns `familyAlerts` → section appears below flagged ingredients
3. Each card shows: profile name, severity badge (correct color), reason text
4. Empty `familyAlerts` → section hidden
5. Bookmark round-trip still works
