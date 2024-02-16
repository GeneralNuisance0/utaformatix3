package process.phonemes

import external.require
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import model.Format
import model.Note
import model.PhonemesMappingPreset
import model.Project
import model.Track
import process.validateNotes

@Serializable
data class PhonemesMappingRequest(
    val mapText: String = "",
) {
    val isValid get() = map.isNotEmpty()

    @Transient
    val map = mapText.lines().mapNotNull { line ->
        if (line.contains("=").not()) return@mapNotNull null
        val from = line.substringBefore("=").trim()
        val to = line.substringAfter("=").trim()
        from to to
    }
        .sortedByDescending { it.first.length }
        .sortedByDescending { it.first.split(" ").size }

    companion object {

        fun findPreset(name: String) = Presets.find { it.name == name }?.phonemesMap

        fun getPreset(name: String) = requireNotNull(findPreset(name))

        val Presets: List<PhonemesMappingPreset> by lazy {
            listOf(
                PhonemesMappingPreset(
                    sourceFormats = listOf(Format.Svp),
                    targetFormats = Format.vocaloidFormats,
                    name = "Japanese (SynthV to Vocaloid)",
                    phonemesMap = PhonemesMappingRequest(
                        require("./texts/SynthV JA to Vocaloid JA.txt").default as String,
                    ),
                ),
                PhonemesMappingPreset(
                    sourceFormats = Format.vocaloidFormats,
                    targetFormats = listOf(Format.Svp),
                    name = "Japanese (Vocaloid to SynthV)",
                    phonemesMap = PhonemesMappingRequest(
                        require("./texts/Vocaloid JA to SynthV JA.txt").default as String,
                    ),
                ),
            )
        }
    }
}

fun Project.mapPhonemes(request: PhonemesMappingRequest?) = copy(
    tracks = tracks.map { it.replacePhonemes(request) },
)

fun Track.replacePhonemes(request: PhonemesMappingRequest?) = copy(
    notes = notes.mapNotNull { note -> note.replacePhonemes(request) }
        .validateNotes(),
)

fun Note.replacePhonemes(request: PhonemesMappingRequest?): Note {
    val input = phoneme?.split(" ") ?: return this
    if (request == null) return copy(phoneme = null)
    val output = mutableListOf<String>()
    var pos = 0
    while (pos <= input.lastIndex) {
        val restInput = input.drop(pos).joinToString(" ")
        var matched = false
        for ((key, value) in request.map) {
            if (restInput.startsWith(key)) {
                output += value.split(" ")
                pos += key.split(" ").size
                matched = true
                break
            }
        }
        if (!matched) {
            output += input[pos]
            pos++
        }
    }
    return copy(phoneme = output.filter { it.isNotBlank() }.joinToString(" "))
}
