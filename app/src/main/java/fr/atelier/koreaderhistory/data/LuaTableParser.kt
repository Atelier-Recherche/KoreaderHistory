package fr.atelier.koreaderhistory.data

/**
 * Minimal Lua table reader for KOReader settings shaped as:
 * return { [1] = { ["key"] = value, ... }, ... }
 * or return { ["key"] = value, ... }
 */
object LuaTableParser {

    fun parseRootTable(source: String): Map<String, Any?> {
        val body = extractReturnTable(source) ?: return emptyMap()
        return parseTableBody(body)
    }

    fun parseArrayOfTables(source: String): List<Map<String, Any?>> {
        val root = parseRootTable(source)
        if (root.isEmpty()) return emptyList()
        val indexed = root.entries
            .mapNotNull { (k, v) ->
                val idx = k.toIntOrNull() ?: return@mapNotNull null
                val table = v as? Map<*, *> ?: return@mapNotNull null
                idx to table.entries.associate { it.key.toString() to it.value }
            }
            .sortedBy { it.first }
            .map { it.second }
        if (indexed.isNotEmpty()) return indexed
        return emptyList()
    }

    private fun extractReturnTable(source: String): String? {
        val cleaned = source
            .lines()
            .filterNot { line ->
                val t = line.trimStart()
                t.startsWith("--")
            }
            .joinToString("\n")
        val idx = cleaned.indexOf("return")
        if (idx < 0) return null
        val after = cleaned.substring(idx + "return".length).trimStart()
        if (!after.startsWith("{")) return null
        return extractBalanced(after, 0)
    }

    private fun extractBalanced(s: String, openIndex: Int): String? {
        var depth = 0
        var inString = false
        var stringDelim = '"'
        var i = openIndex
        while (i < s.length) {
            val c = s[i]
            if (inString) {
                if (c == '\\') {
                    i += 2
                    continue
                }
                if (c == stringDelim) inString = false
                i++
                continue
            }
            when (c) {
                '"', '\'' -> {
                    inString = true
                    stringDelim = c
                }
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return s.substring(openIndex + 1, i)
                }
            }
            i++
        }
        return null
    }

    private fun parseTableBody(body: String): Map<String, Any?> {
        val result = linkedMapOf<String, Any?>()
        var i = 0
        while (i < body.length) {
            i = skipWsComma(body, i)
            if (i >= body.length) break

            val key: String
            if (body[i] == '[') {
                val close = body.indexOf(']', i)
                if (close < 0) break
                key = unquote(body.substring(i + 1, close).trim())
                i = skipWsComma(body, close + 1)
                if (i >= body.length || body[i] != '=') break
                i++
            } else {
                // bare key = value (rare in KOReader dumps)
                val eq = body.indexOf('=', i)
                if (eq < 0) break
                key = body.substring(i, eq).trim()
                i = eq + 1
            }

            i = skipWsComma(body, i)
            if (i >= body.length) break

            val (value, next) = parseValue(body, i)
            result[key] = value
            i = next
        }
        return result
    }

    private fun parseValue(s: String, start: Int): Pair<Any?, Int> {
        var i = skipWsComma(s, start)
        if (i >= s.length) return null to i
        return when (s[i]) {
            '{' -> {
                val inner = extractBalanced(s, i) ?: return null to s.length
                val end = i + inner.length + 2
                parseTableBody(inner) to end
            }
            '"', '\'' -> {
                val (str, end) = parseString(s, i)
                str to end
            }
            else -> {
                val end = findValueEnd(s, i)
                val token = s.substring(i, end).trim().trimEnd(',')
                parseScalar(token) to end
            }
        }
    }

    private fun parseString(s: String, start: Int): Pair<String, Int> {
        val delim = s[start]
        val out = StringBuilder()
        var i = start + 1
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                out.append(s[i + 1])
                i += 2
                continue
            }
            if (c == delim) return out.toString() to (i + 1)
            out.append(c)
            i++
        }
        return out.toString() to s.length
    }

    private fun findValueEnd(s: String, start: Int): Int {
        var i = start
        while (i < s.length) {
            val c = s[i]
            if (c == ',' || c == '}') return i
            if (c == '\n') return i
            i++
        }
        return s.length
    }

    private fun parseScalar(token: String): Any? {
        when (token) {
            "true" -> return true
            "false" -> return false
            "nil" -> return null
        }
        token.toLongOrNull()?.let { return it }
        token.toDoubleOrNull()?.let { return it }
        return token
    }

    private fun unquote(raw: String): String {
        val t = raw.trim()
        if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'"))) {
            return t.substring(1, t.length - 1)
        }
        return t
    }

    private fun skipWsComma(s: String, start: Int): Int {
        var i = start
        while (i < s.length) {
            val c = s[i]
            if (c.isWhitespace() || c == ',') {
                i++
                continue
            }
            if (c == '-' && i + 1 < s.length && s[i + 1] == '-') {
                while (i < s.length && s[i] != '\n') i++
                continue
            }
            break
        }
        return i
    }
}
