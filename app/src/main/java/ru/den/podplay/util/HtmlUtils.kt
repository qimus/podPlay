package ru.den.podplay.util

import android.os.Build
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView

object HtmlUtils {
    fun htmlToSpannable(htmlDesc: String): Spanned {
        var newHtmlDesc = htmlDesc.replace("\n".toRegex(), "")
        newHtmlDesc = newHtmlDesc.replace("(<(/)img>)|(<img.+?>)".toRegex(), "")

        val descSpan: Spanned
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            descSpan = Html.fromHtml(newHtmlDesc, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            descSpan = Html.fromHtml(newHtmlDesc)
        }
        return descSpan
    }

    private fun makeLinkClickable(stringBuilder: SpannableStringBuilder,
                                  span: URLSpan, onClick: (URLSpan) -> Unit) {
        val start = stringBuilder.getSpanStart(span)
        val end = stringBuilder.getSpanEnd(span)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                onClick(span)
            }
        }
        stringBuilder.setSpan(clickableSpan, start, end, stringBuilder.getSpanFlags(span))
        stringBuilder.removeSpan(span)
    }

    fun setTextViewHTML(text: TextView, html: String, onClick: (URLSpan) -> Unit) {
        @Suppress("DEPRECATION")
        val sequence = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(html)
        }
        val strBuilder = SpannableStringBuilder(sequence)
        val urls = strBuilder.getSpans(0, sequence.length, URLSpan::class.java)
        for (span in urls) {
            makeLinkClickable(strBuilder, span, onClick)
        }
        text.text = strBuilder
        text.movementMethod = LinkMovementMethod()
    }
}
