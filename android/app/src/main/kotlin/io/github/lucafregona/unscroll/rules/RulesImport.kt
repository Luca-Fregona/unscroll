package io.github.lucafregona.unscroll.rules

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer

/**
 * Imports a rule file handed over as a stream, such as the one the system file picker returns. Reads at most
 * [MAX_RULES_BYTES] plus one byte, so a huge or endless file cannot fill the memory.
 */
fun RulesRepository.importStream(input: InputStream): ImportResult {
    val body = ByteArrayOutputStream()
    val buffer = ByteArray(8_192)
    input.use { stream ->
        while (body.size() <= MAX_RULES_BYTES) {
            val wanted = minOf(buffer.size, MAX_RULES_BYTES + 1 - body.size())
            val read = stream.read(buffer, 0, wanted)
            if (read < 0) break
            body.write(buffer, 0, read)
        }
    }
    if (body.size() > MAX_RULES_BYTES) return ImportResult.Rejected("the file is larger than ${MAX_RULES_BYTES / 1024} KiB")
    val text = try {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(body.toByteArray()))
            .toString()
    } catch (_: CharacterCodingException) {
        return ImportResult.Rejected("the file is not UTF-8 text")
    }
    return import(text)
}
