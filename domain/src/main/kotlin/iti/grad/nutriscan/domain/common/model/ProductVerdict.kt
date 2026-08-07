package iti.grad.nutriscan.domain.common.model

import kotlinx.serialization.Serializable

/**
 * Health verdict of a product. Shared by the Saved catalog and the food log —
 * lives in domain since both features need it.
 */
@Serializable
enum class ProductVerdict {
    SAFE,
    CAUTION,
    UNSAFE
}
