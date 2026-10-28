package mn.blazeapps.blazein

import mn.blazeapps.blazein.media.iTunesMetadataService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class iTunesMetadataServiceTest {

    @Test
    fun testParseMovieQuery_agadhaExampleWithWebsiteAndTags() {
        val raw1 = "Agadha (2026) Telugu HQ HDRip - x264 - AAC - 700MB - ESub.mkv"
        val query1 = iTunesMetadataService.parseMovieQuery(raw1)
        assertEquals("Agadha", query1.title)
        assertEquals("2026", query1.year)
        assertEquals("Agadha 2026", query1.searchQuery)

        val raw2 = "www.1TamilMV.yt - Agadha (2026) Telugu HQ HDRip - x264 - AAC - 700MB - ESub.mkv"
        val query2 = iTunesMetadataService.parseMovieQuery(raw2)
        assertEquals("Agadha", query2.title)
        assertEquals("2026", query2.year)
        assertEquals("Agadha 2026", query2.searchQuery)

        val raw3 = "[1TamilMV.com] Agadha (2026) Telugu HQ HDRip - x264 - AAC - 700MB - ESub.mkv"
        val query3 = iTunesMetadataService.parseMovieQuery(raw3)
        assertEquals("Agadha", query3.title)
        assertEquals("2026", query3.year)
        assertEquals("Agadha 2026", query3.searchQuery)
    }

    @Test
    fun testParseMovieQuery_variousReleasePatterns() {
        val raw1 = "Spider-Man.Across.the.Spider-Verse.2023.1080p.WEBRip.x264.AAC-[YTS.MX]"
        val query1 = iTunesMetadataService.parseMovieQuery(raw1)
        assertEquals("Spider-Man Across the Spider-Verse", query1.title)
        assertEquals("2023", query1.year)
        assertEquals("Spider-Man Across the Spider-Verse 2023", query1.searchQuery)

        val raw2 = "Inception.2010.Bluray.2160p.4K.HDR.HEVC.DTS-HD.MA.5.1"
        val query2 = iTunesMetadataService.parseMovieQuery(raw2)
        assertEquals("Inception", query2.title)
        assertEquals("2010", query2.year)
        assertEquals("Inception 2010", query2.searchQuery)

        val raw3 = "[Erai-raws] Suzume no Tojimari (2022) [1080p][HEVC]"
        val query3 = iTunesMetadataService.parseMovieQuery(raw3)
        assertEquals("Suzume no Tojimari", query3.title)
        assertEquals("2022", query3.year)
        assertEquals("Suzume no Tojimari 2022", query3.searchQuery)

        val raw4 = "www.Movierulz.tc - Kalki 2898 AD (2024) Telugu True WEB-DL.mkv"
        val query4 = iTunesMetadataService.parseMovieQuery(raw4)
        assertEquals("Kalki 2898 AD", query4.title)
        assertEquals("2024", query4.year)
        assertEquals("Kalki 2898 AD 2024", query4.searchQuery)

        val raw5 = "Dust.Bunny.2025.720p.WEBRip.x264.AAC-[YTS.BZ].mp4"
        val query5 = iTunesMetadataService.parseMovieQuery(raw5)
        assertEquals("Dust Bunny", query5.title)
        assertEquals("2025", query5.year)
        assertEquals("Dust Bunny 2025", query5.searchQuery)
    }

    @Test
    fun testIsTitleMatch_accuratelyRejectsFalsePositivesAndAcceptsMatches() {
        // Exact match
        assertTrue(iTunesMetadataService.isTitleMatch("Dust Bunny", "Dust Bunny"))

        // Wikipedia title with (film)
        assertTrue(iTunesMetadataService.isTitleMatch("Dust Bunny", "Dust Bunny (film)"))

        // Subtitle or punctuation variance
        assertTrue(iTunesMetadataService.isTitleMatch("Spider-Man Across the Spider-Verse", "Spider-Man: Across the Spider-Verse"))

        // Must reject completely different titles
        assertFalse(iTunesMetadataService.isTitleMatch("Dust Bunny", "The Second"))
        assertFalse(iTunesMetadataService.isTitleMatch("Dust Bunny", "Bunny"))
        assertFalse(iTunesMetadataService.isTitleMatch("Inception", "Interstellar"))
        assertFalse(iTunesMetadataService.isTitleMatch("Agadha", "Aagatha"))
    }

    @Test
    fun testIsYearAcceptable() {
        assertTrue(iTunesMetadataService.isYearAcceptable("2025", "2025"))
        assertTrue(iTunesMetadataService.isYearAcceptable("2025", "2024"))
        assertTrue(iTunesMetadataService.isYearAcceptable("2025", "2026"))

        // Reject distant years
        assertFalse(iTunesMetadataService.isYearAcceptable("2025", "2021"))
        assertFalse(iTunesMetadataService.isYearAcceptable("2025", "2018"))
    }
}
