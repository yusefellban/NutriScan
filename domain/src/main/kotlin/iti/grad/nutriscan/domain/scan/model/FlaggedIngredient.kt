package iti.grad.nutriscan.domain.scan.model

/**
 * A single ingredient that was flagged as problematic for the user's profile.
 *
 * Each [FlaggedIngredient] maps to a card in the "Why It's Unsafe?" section
 * on the Product Details screen.
 */
data class FlaggedIngredient(
    /** Display name of the ingredient (e.g. "Hazelnuts", "Skimmed Milk Powder"). */
    val name: String,
    /** The allergy/condition tag this ingredient matches (e.g. "Tree Nuts Allergy", "Lactose Intolerance"). */
    val matchTag: String,
    /** Human-readable reason text (e.g. "Matches allergy in your profile"). */
    val reason: String,
)
