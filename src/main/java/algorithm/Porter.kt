/* author:   Fotis Lazarinis (actually I translated from C to Java)
   date:     June 1997
   address:  Psilovraxou 12, Agrinio, 30100

   comments: Compile it, import the Porter class into you program and create an instance.
	     Then use the stripAffixes method of this method which takes a String as
             input and returns the stem of this String again as a String.

*/

package algorithm

internal class NewString(var str: String = "")

class Porter {

    private fun Clean(str: String): String {
        val last = str.length

        val temp = StringBuilder()

        for (i in 0 until last) {
            if (Character.isLetterOrDigit(str[i]))
                temp.append(str[i])
        }

        return temp.toString()
    }

    private fun hasSuffix(word: String, suffix: String, stem: NewString): Boolean {
        if (word.length <= suffix.length)
            return false
        if (suffix.length > 1)
            if (word[word.length - 2] != suffix[suffix.length - 2])
                return false

        stem.str = ""

        for (i in 0 until word.length - suffix.length)
            stem.str += word[i]
        var tmp = stem.str

        for (i in 0 until suffix.length)
            tmp += suffix[i]

        return tmp.compareTo(word) == 0
    }

    private fun vowel(ch: Char, prev: Char): Boolean {
        return when (ch) {
            'a', 'e', 'i', 'o', 'u' -> true
            'y' -> {
                when (prev) {
                    'a', 'e', 'i', 'o', 'u' -> false
                    else -> true
                }
            }
            else -> false
        }
    }

    private fun measure(stem: String): Int {

        var i = 0
        var count = 0
        val length = stem.length

        while (i < length) {
            while (i < length) {
                if (i > 0) {
                    if (vowel(stem[i], stem[i - 1]))
                        break
                } else {
                    if (vowel(stem[i], 'a'))
                        break
                }
                i++
            }

            i++
            while (i < length) {
                if (i > 0) {
                    if (!vowel(stem[i], stem[i - 1]))
                        break
                } else {
                    if (!vowel(stem[i], '?'))
                        break
                }
                i++
            }
            if (i < length) {
                count++
                i++
            }
        } //while

        return count
    }

    private fun containsVowel(word: String): Boolean {

        for (i in 0 until word.length)
            if (i > 0) {
                if (vowel(word[i], word[i - 1]))
                    return true
            } else {
                if (vowel(word[0], 'a'))
                    return true
            }

        return false
    }

    private fun cvc(str: String): Boolean {
        val length = str.length

        if (length < 3)
            return false

        return if (!vowel(str[length - 1], str[length - 2])
            && str[length - 1] != 'w' && str[length - 1] != 'x' && str[length - 1] != 'y'
            && vowel(str[length - 2], str[length - 3])
        ) {

            if (length == 3) {
                !vowel(str[0], '?')
            } else {
                !vowel(str[length - 4], str[length - 3])
            }
        } else false

    }

    private fun step1(str: String): String {
        var mutableStr = str

        val stem = NewString()

        if (mutableStr[mutableStr.length - 1] == 's') {
            if (hasSuffix(mutableStr, "sses", stem) || hasSuffix(mutableStr, "ies", stem)) {
                var tmp = ""
                for (i in 0 until mutableStr.length - 2)
                    tmp += mutableStr[i]
                mutableStr = tmp
            } else {
                if (mutableStr.length == 1 && mutableStr[mutableStr.length - 1] == 's') {
                    mutableStr = ""
                    return mutableStr
                }
                if (mutableStr[mutableStr.length - 2] != 's') {
                    var tmp = ""
                    for (i in 0 until mutableStr.length - 1)
                        tmp += mutableStr[i]
                    mutableStr = tmp
                }
            }
        }

        if (hasSuffix(mutableStr, "eed", stem)) {
            if (measure(stem.str) > 0) {
                var tmp = ""
                for (i in 0 until mutableStr.length - 1)
                    tmp += mutableStr[i]
                mutableStr = tmp
            }
        } else {
            if (hasSuffix(mutableStr, "ed", stem) || hasSuffix(mutableStr, "ing", stem)) {
                if (containsVowel(stem.str)) {

                    var tmp = ""
                    for (i in 0 until stem.str.length)
                        tmp += mutableStr[i]
                    mutableStr = tmp
                    if (mutableStr.length == 1)
                        return mutableStr

                    if (hasSuffix(mutableStr, "at", stem) || hasSuffix(mutableStr, "bl", stem) || hasSuffix(mutableStr, "iz", stem)) {
                        mutableStr += "e"

                    } else {
                        val length = mutableStr.length
                        if (mutableStr[length - 1] == mutableStr[length - 2]
                            && mutableStr[length - 1] != 'l' && mutableStr[length - 1] != 's' && mutableStr[length - 1] != 'z'
                        ) {

                            tmp = ""
                            for (i in 0 until mutableStr.length - 1)
                                tmp += mutableStr[i]
                            mutableStr = tmp
                        } else if (measure(mutableStr) == 1) {
                            if (cvc(mutableStr))
                                mutableStr += "e"
                        }
                    }
                }
            }
        }

        if (hasSuffix(mutableStr, "y", stem))
            if (containsVowel(stem.str)) {
                var tmp = ""
                for (i in 0 until mutableStr.length - 1)
                    tmp += mutableStr[i]
                mutableStr = tmp + "i"
            }
        return mutableStr
    }

    private fun step2(str: String): String {
        var mutableStr = str

        val suffixes = arrayOf(
            arrayOf("ational", "ate"),
            arrayOf("tional", "tion"),
            arrayOf("enci", "ence"),
            arrayOf("anci", "ance"),
            arrayOf("izer", "ize"),
            arrayOf("iser", "ize"),
            arrayOf("abli", "able"),
            arrayOf("alli", "al"),
            arrayOf("entli", "ent"),
            arrayOf("eli", "e"),
            arrayOf("ousli", "ous"),
            arrayOf("ization", "ize"),
            arrayOf("isation", "ize"),
            arrayOf("ation", "ate"),
            arrayOf("ator", "ate"),
            arrayOf("alism", "al"),
            arrayOf("iveness", "ive"),
            arrayOf("fulness", "ful"),
            arrayOf("ousness", "ous"),
            arrayOf("aliti", "al"),
            arrayOf("iviti", "ive"),
            arrayOf("biliti", "ble")
        )
        val stem = NewString()


        for (index in suffixes.indices) {
            if (hasSuffix(mutableStr, suffixes[index][0], stem)) {
                if (measure(stem.str) > 0) {
                    mutableStr = stem.str + suffixes[index][1]
                    return mutableStr
                }
            }
        }

        return mutableStr
    }

    private fun step3(str: String): String {
        var mutableStr = str

        val suffixes = arrayOf(
            arrayOf("icate", "ic"),
            arrayOf("ative", ""),
            arrayOf("alize", "al"),
            arrayOf("alise", "al"),
            arrayOf("iciti", "ic"),
            arrayOf("ical", "ic"),
            arrayOf("ful", ""),
            arrayOf("ness", "")
        )
        val stem = NewString()

        for (index in suffixes.indices) {
            if (hasSuffix(mutableStr, suffixes[index][0], stem))
                if (measure(stem.str) > 0) {
                    mutableStr = stem.str + suffixes[index][1]
                    return mutableStr
                }
        }
        return mutableStr
    }

    private fun step4(str: String): String {
        var mutableStr = str

        val suffixes = arrayOf(
            "al",
            "ance",
            "ence",
            "er",
            "ic",
            "able",
            "ible",
            "ant",
            "ement",
            "ment",
            "ent",
            "sion",
            "tion",
            "ou",
            "ism",
            "ate",
            "iti",
            "ous",
            "ive",
            "ize",
            "ise"
        )

        val stem = NewString()

        for (index in suffixes.indices) {
            if (hasSuffix(mutableStr, suffixes[index], stem)) {

                if (measure(stem.str) > 1) {
                    mutableStr = stem.str
                    return mutableStr
                }
            }
        }
        return mutableStr
    }

    private fun step5(str: String): String {
        var mutableStr = str

        if (mutableStr[mutableStr.length - 1] == 'e') {
            if (measure(mutableStr) > 1) {/* measure(str)==measure(stem) if ends in vowel */
                var tmp = ""
                for (i in 0 until mutableStr.length - 1)
                    tmp += mutableStr[i]
                mutableStr = tmp
            } else if (measure(mutableStr) == 1) {
                var stem = ""
                for (i in 0 until mutableStr.length - 1)
                    stem += mutableStr[i]

                if (!cvc(stem))
                    mutableStr = stem
            }
        }

        if (mutableStr.length == 1)
            return mutableStr
        if (mutableStr[mutableStr.length - 1] == 'l' && mutableStr[mutableStr.length - 2] == 'l' && measure(mutableStr) > 1)
            if (measure(mutableStr) > 1) {/* measure(str)==measure(stem) if ends in vowel */
                var tmp = ""
                for (i in 0 until mutableStr.length - 1)
                    tmp += mutableStr[i]
                mutableStr = tmp
            }
        return mutableStr
    }

    private fun stripPrefixes(str: String): String {

        val prefixes = arrayOf("kilo", "micro", "milli", "intra", "ultra", "mega", "nano", "pico", "pseudo")

        val last = prefixes.size
        for (i in 0 until last) {
            if (str.startsWith(prefixes[i])) {
                var temp = ""
                for (j in 0 until str.length - prefixes[i].length)
                    temp += str[j + prefixes[i].length]
                return temp
            }
        }

        return str
    }


    private fun stripSuffixes(str: String): String {
        var mutableStr = str

        mutableStr = step1(mutableStr)
        if (mutableStr.isNotEmpty())
            mutableStr = step2(mutableStr)
        if (mutableStr.isNotEmpty())
            mutableStr = step3(mutableStr)
        if (mutableStr.isNotEmpty())
            mutableStr = step4(mutableStr)
        if (mutableStr.isNotEmpty())
            mutableStr = step5(mutableStr)

        return mutableStr
    }


    fun stripAffixes(str: String): String {
        var mutableStr = str

        mutableStr = mutableStr.toLowerCase()
        mutableStr = Clean(mutableStr)

        if (mutableStr !== "" && mutableStr.length > 2) {
            mutableStr = stripPrefixes(mutableStr)

            if (mutableStr !== "")
                mutableStr = stripSuffixes(mutableStr)

        }

        return mutableStr
    } //stripAffixes

}
