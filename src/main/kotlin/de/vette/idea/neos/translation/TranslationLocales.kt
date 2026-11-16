/*
 *  IntelliJ IDEA plugin to support the Neos CMS.
 *  Copyright (C) 2016  Christian Vette
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vette.idea.neos.translation

/**
 * How locales relate to each other.
 *
 * Flow looks a label up along a chain of locales: the locale itself, its parents ("de_CH" falls back to "de")
 * and finally the default locale. A label that is missing in a locale is therefore not missing for its users
 * if a parent locale has it. The default locale is left out here: it is the locale the labels are written in,
 * and a label that only exists there is what "not translated" means.
 *
 * The `fallbackRule` setting of Flow (other fallback locales, strict mode) is not taken into account.
 */
object TranslationLocales {

    /** True for two notations of the same locale, e.g. "en-US" and "en_us" */
    @JvmStatic
    fun same(a: String, b: String): Boolean = normalize(a) == normalize(b)

    /** The locales a locale falls back to, the closest first: "sr_Cyrl_RS" has "sr_Cyrl" and "sr" */
    @JvmStatic
    fun parents(locale: String): List<String> {
        val parts = locale.replace('-', '_').split('_').filter(String::isNotEmpty)
        return (parts.size - 1 downTo 1).map { parts.take(it).joinToString("_") }
    }

    /** True if the locale is one of the given locales */
    @JvmStatic
    fun contains(locales: Iterable<String>, locale: String): Boolean = locales.any { same(it, locale) }

    /**
     * True if a label that exists in the given locales is translated for the users of the locale:
     * it exists in the locale itself or in one of its parents.
     */
    @JvmStatic
    fun isCovered(locale: String, available: Iterable<String>): Boolean =
        contains(available, locale) || parents(locale).any { contains(available, it) }

    private fun normalize(locale: String): String = locale.replace('-', '_').lowercase()
}
