package de.mm20.launcher2.contacts.providers

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import androidx.core.graphics.drawable.toDrawable
import de.mm20.launcher2.contacts.AndroidContactSerializer
import de.mm20.launcher2.icons.ColorLayer
import de.mm20.launcher2.icons.LauncherIcon
import de.mm20.launcher2.icons.StaticIconLayer
import de.mm20.launcher2.icons.StaticLauncherIcon
import de.mm20.launcher2.ktx.asBitmap
import de.mm20.launcher2.ktx.tryStartActivity
import de.mm20.launcher2.search.Contact
import de.mm20.launcher2.search.SearchableSerializer
import de.mm20.launcher2.search.contact.CustomContactAction
import de.mm20.launcher2.search.contact.EmailAddress
import de.mm20.launcher2.search.contact.PhoneNumber
import de.mm20.launcher2.search.contact.PostalAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class AndroidContact(
    internal val id: Long,
    override val name: String,
    override val phoneNumbers: List<PhoneNumber>,
    override val emailAddresses: List<EmailAddress>,
    override val postalAddresses: List<PostalAddress>,
    override val customActions: List<CustomContactAction>, internal val lookupKey: String,
    override val labelOverride: String? = null,
) : Contact {


    override val domain: String = Domain
    override val key: String
        get() = "$Domain://$id"

    override val summary: String
        get() {
            return (phoneNumbers.map { it.number } + emailAddresses.map { it.address }).joinToString(", ")
        }

    override fun overrideLabel(label: String): Contact {
        return copy(labelOverride = label)
    }

    override fun launch(context: Context, options: Bundle?): Boolean {
        val uri =
            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id)
        val intent = Intent(Intent.ACTION_VIEW).setData(uri).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return context.tryStartActivity(intent, options)
    }

    override suspend fun loadIcon(
        context: Context,
        size: Int,
        themed: Boolean,
    ): LauncherIcon? {
        val contentResolver = context.contentResolver
        val bmp = withContext(Dispatchers.IO) {
            val uri =
                ContactsContract.Contacts.getLookupUri(id, lookupKey) ?: return@withContext null
            ContactsContract.Contacts.openContactPhotoInputStream(contentResolver, uri, false)
                ?.asBitmap()
        } ?: return null

        return StaticLauncherIcon(
            foregroundLayer = StaticIconLayer(
                icon = bmp.toDrawable(context.resources),
            ),
            backgroundLayer = ColorLayer(0xFF2364AA.toInt())
        )
    }

    override fun getSerializer(): SearchableSerializer {
        return AndroidContactSerializer()
    }

    companion object {
        const val Domain = "contact"
    }
}