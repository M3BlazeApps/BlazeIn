package mn.blazeapps.blazein.media

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mn.blazeapps.blazein.data.model.MovieMetadata
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

data class CleanedMovieQuery(
    val title: String,
    val year: String,
    val searchQuery: String
)

class iTunesMetadataService(private val context: Context) {

    private val metadataCache = ConcurrentHashMap<String, MovieMetadata>()

    suspend fun fetchMetadataForFile(fileName: String): MovieMetadata? = withContext(Dispatchers.IO) {
        val query = parseMovieQuery(fileName)
        if (query.title.isBlank()) return@withContext null

        metadataCache[fileName]?.let { return@withContext it }
        metadataCache[query.searchQuery]?.let { return@withContext it }

        Log.d(TAG, "Parsed movie query: title='${query.title}', year='${query.year}', search='${query.searchQuery}'")

        // 1. First attempt: Search iTunes with "$title $year" restricted to movies
        var metadata = queryiTunes(query.searchQuery, query.title, query.year)

        // 2. Second attempt: Search iTunes with title alone if year was present and no match
        if (metadata == null && query.year.isNotBlank() && query.searchQuery != query.title) {
            Log.d(TAG, "Retrying iTunes search with title only: '${query.title}'")
            metadata = queryiTunes(query.title, query.title, query.year)
        }

        // 3. Fallback: Search Wikipedia if iTunes returned no verified match
        if (metadata == null) {
            Log.d(TAG, "iTunes returned no verified match. Searching Wikipedia for '${query.title}' (${query.year})")
            metadata = queryWikipedia(query.title, query.year)
        }

        // 4. Clean fallback: If no online database has a verified match, use the clean parsed title and year
        if (metadata == null) {
            metadata = MovieMetadata(
                title = query.title,
                year = query.year,
                genre = "Movie",
                plot = "",
                posterUrl = "",
                posterLocalPath = null
            )
        }

        metadataCache[fileName] = metadata
        metadataCache[query.searchQuery] = metadata
        metadata
    }

    private fun queryiTunes(searchTerm: String, expectedTitle: String, expectedYear: String): MovieMetadata? {
        try {
            val encodedTitle = URLEncoder.encode(searchTerm, "UTF-8")
            val url = URL("https://itunes.apple.com/search?term=$encodedTitle&media=movie&entity=movie&limit=10")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", "BlazeIn/2.0 (Android; movie-metadata)")

            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)

            val count = json.optInt("resultCount", 0)
            if (count > 0) {
                val results = json.getJSONArray("results")
                var bestObj: JSONObject? = null

                for (i in 0 until results.length()) {
                    val obj = results.getJSONObject(i)
                    val trackName = obj.optString("trackName", "")
                    val releaseDate = obj.optString("releaseDate", "").take(4)

                    // Verify title similarity and year acceptability
                    if (isTitleMatch(expectedTitle, trackName) && isYearAcceptable(expectedYear, releaseDate)) {
                        bestObj = obj
                        break
                    }
                }

                bestObj?.let { result ->
                    val trackName = result.optString("trackName", expectedTitle)
                    val releaseDate = result.optString("releaseDate", "").take(4).ifBlank { expectedYear }
                    val genre = result.optString("primaryGenreName", "Movie")
                    val plot = result.optString("longDescription", result.optString("shortDescription", ""))
                    val artworkRaw = result.optString("artworkUrl100", "")
                    val highResPosterUrl = if (artworkRaw.isNotBlank()) {
                        artworkRaw.replace("100x100bb", "600x600bb")
                    } else ""

                    val localPosterPath = if (highResPosterUrl.isNotBlank()) {
                        downloadPosterToCache(highResPosterUrl, trackName)
                    } else null

                    return MovieMetadata(
                        title = trackName,
                        year = releaseDate,
                        genre = genre,
                        plot = plot,
                        posterUrl = highResPosterUrl,
                        posterLocalPath = localPosterPath
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "iTunes search error for '$searchTerm': ${e.message}")
        }
        return null
    }

    private fun queryWikipedia(title: String, year: String): MovieMetadata? {
        try {
            val searchTerm = if (year.isNotBlank()) "$title $year film" else "$title film"
            val encodedSearch = URLEncoder.encode(searchTerm, "UTF-8")
            val searchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encodedSearch&format=json"

            val searchConn = URL(searchUrl).openConnection() as HttpURLConnection
            searchConn.connectTimeout = 8000
            searchConn.readTimeout = 8000
            searchConn.setRequestProperty("User-Agent", "BlazeIn/2.0 (Android; movie-metadata)")

            val searchResp = searchConn.inputStream.bufferedReader().use { it.readText() }
            val searchJson = JSONObject(searchResp)
            val searchArray = searchJson.optJSONObject("query")?.optJSONArray("search")
            if (searchArray == null || searchArray.length() == 0) return null

            var matchedPageTitle: String? = null
            for (i in 0 until searchArray.length()) {
                val page = searchArray.getJSONObject(i)
                val pTitle = page.optString("title", "")
                if (isTitleMatch(title, pTitle)) {
                    matchedPageTitle = pTitle
                    break
                }
            }

            if (matchedPageTitle == null && year.isNotBlank()) {
                val fallbackSearch = URLEncoder.encode("$title film", "UTF-8")
                val fallbackUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$fallbackSearch&format=json"
                val fbConn = URL(fallbackUrl).openConnection() as HttpURLConnection
                fbConn.connectTimeout = 8000
                fbConn.readTimeout = 8000
                fbConn.setRequestProperty("User-Agent", "BlazeIn/2.0 (Android; movie-metadata)")
                val fbResp = fbConn.inputStream.bufferedReader().use { it.readText() }
                val fbArray = JSONObject(fbResp).optJSONObject("query")?.optJSONArray("search")
                if (fbArray != null) {
                    for (i in 0 until fbArray.length()) {
                        val pTitle = fbArray.getJSONObject(i).optString("title", "")
                        if (isTitleMatch(title, pTitle)) {
                            matchedPageTitle = pTitle
                            break
                        }
                    }
                }
            }

            val pageTitle = matchedPageTitle ?: return null

            val encodedPage = URLEncoder.encode(pageTitle.replace(" ", "_"), "UTF-8")
            val summaryUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/$encodedPage"

            val sumConn = URL(summaryUrl).openConnection() as HttpURLConnection
            sumConn.connectTimeout = 8000
            sumConn.readTimeout = 8000
            sumConn.setRequestProperty("User-Agent", "BlazeIn/2.0 (Android; movie-metadata)")

            val sumResp = sumConn.inputStream.bufferedReader().use { it.readText() }
            val sumJson = JSONObject(sumResp)

            val resolvedTitle = sumJson.optString("title", title)
            val plot = sumJson.optString("extract", "")
            val description = sumJson.optString("description", "Film")

            val thumbObj = sumJson.optJSONObject("thumbnail")
            val origObj = sumJson.optJSONObject("originalimage")
            val posterUrl = origObj?.optString("source")
                ?: thumbObj?.optString("source")
                ?: ""

            val localPoster = if (posterUrl.isNotBlank()) {
                downloadPosterToCache(posterUrl, resolvedTitle)
            } else null

            return MovieMetadata(
                title = resolvedTitle,
                year = year,
                genre = description,
                plot = plot,
                posterUrl = posterUrl,
                posterLocalPath = localPoster
            )
        } catch (e: Exception) {
            Log.w(TAG, "Wikipedia search error for '$title': ${e.message}")
        }
        return null
    }

    private fun downloadPosterToCache(posterUrl: String, title: String): String? {
        return try {
            val postersDir = File(context.externalCacheDir ?: context.cacheDir, "itunes_posters")
            if (!postersDir.exists()) postersDir.mkdirs()

            val safeName = title.replace(Regex("[^a-zA-Z0-9.-]"), "_").take(50).trim('_')
            val targetFile = File(postersDir, "poster_${safeName}.jpg")

            if (targetFile.exists() && targetFile.length() > 0) {
                return targetFile.absolutePath
            }

            val conn = URL(posterUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.setRequestProperty("User-Agent", "BlazeIn/2.0 (Android; movie-metadata)")

            conn.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "Error downloading poster: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "iTunesMetadataService"

        private val STOPWORDS = setOf(
            "the", "a", "an", "and", "or", "of", "in", "on", "at", "to", "for", "with", "by", "film", "movie"
        )

        fun isTitleMatch(expectedTitle: String, candidateTitle: String): Boolean {
            val normExpected = normalizeTitleForComparison(expectedTitle)
            val normCandidate = normalizeTitleForComparison(candidateTitle)

            if (normExpected.isBlank() || normCandidate.isBlank()) return false
            if (normExpected == normCandidate) return true

            val expWords = normExpected.split(" ").filter { it.isNotBlank() }
            val candWords = normCandidate.split(" ").filter { it.isNotBlank() }

            val expContent = expWords.filter { it.length >= 2 && !STOPWORDS.contains(it) }.ifEmpty { expWords }
            val candContent = candWords.filter { it.length >= 2 && !STOPWORDS.contains(it) }.ifEmpty { candWords }

            val expSet = expContent.toSet()
            val candSet = candContent.toSet()

            // If both sets of content words are identical (e.g. word order difference or stopwords omitted)
            if (expSet == candSet) return true

            val matchingCount = expContent.count { candSet.contains(it) }

            return when {
                // Single word title: must match exactly (ignoring stopwords)
                expContent.size == 1 -> {
                    candContent.size == 1 && expContent[0] == candContent[0]
                }
                // Two word title: all content words must be present in candidate, and candidate can't have too many extra words
                expContent.size == 2 -> {
                    matchingCount == 2 && candContent.size <= 3
                }
                // Multi-word title (3+ words): at least 70% of expected content words must be in candidate,
                // and at least 50% of candidate content words must be in expected
                else -> {
                    val expOverlap = matchingCount.toFloat() / expContent.size
                    val candOverlap = candContent.count { expSet.contains(it) }.toFloat() / candContent.size
                    expOverlap >= 0.7f && candOverlap >= 0.5f
                }
            }
        }

        fun isYearAcceptable(expectedYear: String, candidateYear: String): Boolean {
            if (expectedYear.isBlank() || candidateYear.isBlank()) return true
            val expY = expectedYear.toIntOrNull() ?: return true
            val candY = candidateYear.toIntOrNull() ?: return true
            return kotlin.math.abs(expY - candY) <= 1
        }

        private fun normalizeTitleForComparison(raw: String): String {
            return raw.lowercase()
                .replace(Regex("""\(.*?\)"""), " ")
                .replace(Regex("""\[.*?\]"""), " ")
                .replace(Regex("""[^a-z0-9]"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
        }

        fun parseMovieQuery(rawName: String): CleanedMovieQuery {
            // Strip folder paths and extension
            val baseName = rawName
                .substringAfterLast('/')
                .substringAfterLast('\\')
                .let { if (it.contains('.')) it.substringBeforeLast('.') else it }

            var cleaned = baseName.trim()

            // 1. Strip website domains from the front (e.g. www.1TamilMV.yt -, [1TamilMV.com] -, 1TamilBlasters.vip, etc.)
            cleaned = cleaned.replace(
                Regex("""^(?:\[[^\]]*\]|\([^\)]*\)|\{[^\}]*\})?\s*(?:www\.[^\s-_]+|[a-zA-Z0-9.-]+\.(?:com|net|org|yt|tc|vet|vip|ws|lol|in|me|cc|biz|info|tv|co|top|icu|xyz|club|site|online|link|to|pro|pm|is|li|click|mov|app|live))\s*[-_–—:]*\s*""", RegexOption.IGNORE_CASE),
                ""
            )

            // 2. Strip leading bracketed release groups (e.g. [Erai-raws], [YTS.MX], [TGx])
            cleaned = cleaned.replace(
                Regex("""^\[[a-zA-Z0-9._ -]+\]\s*[-_–—:]*\s*"""),
                ""
            )

            // 3. Detect 4-digit release year (19xx or 20xx)
            val yearRegex = Regex("""[\(\[\s._-]((?:19|20)\d{2})(?:[\]\)\s._-]|$)""")
            val yearMatch = yearRegex.find(cleaned)

            val (rawTitlePart, year) = if (yearMatch != null) {
                val y = yearMatch.groupValues[1]
                val t = cleaned.substring(0, yearMatch.range.first)
                t to y
            } else {
                // If no year, cut before common scene / quality / language / audio tags
                val tagRegex = Regex("""(?i)\b(?:1080p|720p|480p|2160p|4k|bluray|bdrip|brrip|web-?dl|webrip|hdrip|dvdrip|hdtc|hd-tc|hq|telugu|tamil|hindi|english|kannada|malayalam|dual audio|multi|x264|x265|hevc|h264|h265|avc|aac|dts|ddp|dd5\.1|5\.1|esub|remux)\b""")
                val tagMatch = tagRegex.find(cleaned)
                val t = if (tagMatch != null) cleaned.substring(0, tagMatch.range.first) else cleaned
                t to ""
            }

            // Clean title part
            var title = rawTitlePart
                .replace(".", " ")
                .replace("_", " ")
                .replace(Regex("""\[.*?\]"""), "")
                .replace(Regex("""\(.*?\)"""), "")
                .trim(' ', '-', '_', ':')
                .replace(Regex("""\s+"""), " ")
                .trim()

            if (title.isBlank()) {
                title = cleaned.replace(".", " ").replace("_", " ").trim()
            }

            val searchQuery = if (year.isNotBlank()) "$title $year".trim() else title

            return CleanedMovieQuery(
                title = title,
                year = year,
                searchQuery = searchQuery
            )
        }

        fun cleanFileName(rawName: String): String {
            return parseMovieQuery(rawName).searchQuery
        }
    }
}
