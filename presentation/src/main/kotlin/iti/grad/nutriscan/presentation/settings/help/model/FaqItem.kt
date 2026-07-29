package iti.grad.nutriscan.presentation.settings.help.model

import androidx.annotation.StringRes
import iti.grad.presentation.R

data class FaqItem(
    val id: Int,
    @StringRes val questionRes: Int,
    @StringRes val answerRes: Int,
) {
    companion object {
        fun default(): List<FaqItem> = listOf(
            FaqItem(1, R.string.help_faq_q_scan, R.string.help_faq_a_scan),
            FaqItem(2, R.string.help_faq_q_receipt, R.string.help_faq_a_receipt),
            FaqItem(3, R.string.help_faq_q_nutrigpt, R.string.help_faq_a_nutrigpt),
            FaqItem(4, R.string.help_faq_q_log, R.string.help_faq_a_log),
            FaqItem(5, R.string.help_faq_q_family, R.string.help_faq_a_family),
            FaqItem(6, R.string.help_faq_q_history, R.string.help_faq_a_history),
            FaqItem(7, R.string.help_faq_q_news, R.string.help_faq_a_news),
            FaqItem(8, R.string.help_faq_q_profile, R.string.help_faq_a_profile),
            FaqItem(9, R.string.help_faq_q_privacy, R.string.help_faq_a_privacy),
            FaqItem(10, R.string.help_faq_q_appearance, R.string.help_faq_a_appearance),
        )
    }
}
