package iti.grad.nutriscan.presentation.settings.help.state

import iti.grad.nutriscan.presentation.settings.help.model.FaqItem

data class HelpState(
    val faqItems: List<FaqItem> = FaqItem.default(),
    val expandedFaqId: Int? = null,
)
