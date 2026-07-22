package iti.grad.nutriscan.domain.common.model

/**
 * Health verdict of a product. Shared by the Saved catalog and the food log —
 * lives in domain since both features need it.
 */
enum class ProductVerdict {
    SAFE,
    CAUTION,
    UNSAFE
}
