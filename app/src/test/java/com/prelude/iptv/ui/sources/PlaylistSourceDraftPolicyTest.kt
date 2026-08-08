package com.prelude.iptv.ui.sources

import com.prelude.iptv.data.PlaylistType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistSourceDraftPolicyTest {
    @Test fun formatsMacWhileUserTypes() {
        assertEquals("00:1A:79:AB:CD:EF", PlaylistSourceDraftPolicy.formatMac("001a79ab-cd ef"))
    }

    @Test fun buildsUrlAndLocalM3uSourcesWithoutMixingStorageModes() {
        val remote = PlaylistSourceDraftPolicy.build(
            PlaylistSourceDraft(
                method = PlaylistSourceMethod.URL,
                playlistUrl = "https://example.com/list.m3u",
                name = "Main",
            ),
        )
        assertEquals(PlaylistType.M3U, remote?.type)
        assertTrue(remote?.isUrl == true)

        val local = PlaylistSourceDraftPolicy.build(
            PlaylistSourceDraft(
                method = PlaylistSourceMethod.FILE,
                filePath = "C:/app/files/playlists/list.m3u",
                fileLabel = "list.m3u",
            ),
        )
        assertEquals(PlaylistType.M3U, local?.type)
        assertFalse(local?.isUrl == true)
        assertEquals("list.m3u", local?.name)
    }

    @Test fun buildsNormalizedStalkerSource() {
        val playlist = PlaylistSourceDraftPolicy.build(
            PlaylistSourceDraft(
                method = PlaylistSourceMethod.MAC,
                portal = "https://portal.example.com/c/",
                macAddress = "001a79abcdef",
            ),
        )

        assertEquals(PlaylistType.STALKER, playlist?.type)
        assertEquals("00:1A:79:AB:CD:EF", playlist?.mac)
    }

    @Test fun detectsXtreamCredentialsInsideM3uUrl() {
        val detection = PlaylistSourceDraftPolicy.detect(
            "https://provider.example:8080/get.php?username=demo_user&password=demo_pass&type=m3u_plus",
        )

        assertEquals(PlaylistSourceMethod.XTREAM, detection?.draft?.method)
        assertEquals("https://provider.example:8080", detection?.draft?.server)
        assertEquals("demo_user", detection?.draft?.username)
        assertEquals("demo_pass", detection?.draft?.password)
    }

    @Test fun detectsPortalAndNormalizesMacFromProviderText() {
        val detection = PlaylistSourceDraftPolicy.detect(
            "Portal: http://portal.example/c/\nMAC: 00-1a-79-ab-cd-ef",
        )

        assertEquals(PlaylistSourceMethod.MAC, detection?.draft?.method)
        assertEquals("00:1A:79:AB:CD:EF", detection?.draft?.macAddress)
    }

    @Test fun detectsThreeLineXtreamCredentialsAndAddsMissingScheme() {
        val detection = PlaylistSourceDraftPolicy.detect("provider.example:8080\ndemo\nsecret")

        assertEquals(PlaylistSourceMethod.XTREAM, detection?.draft?.method)
        assertEquals("http://provider.example:8080", detection?.draft?.server)
    }

    @Test fun detectsSingleXtreamUrlWithoutScheme() {
        val detection = PlaylistSourceDraftPolicy.detect(
            "provider.example/get.php?username=demo&password=secret",
        )

        assertEquals(PlaylistSourceMethod.XTREAM, detection?.draft?.method)
        assertEquals("http://provider.example", detection?.draft?.server)
    }

    @Test fun validationIdentifiesExactFieldAndBuildNormalizesUrl() {
        val incomplete = PlaylistSourceDraft(
            method = PlaylistSourceMethod.XTREAM,
            server = "provider.example",
            username = "demo",
        )
        assertEquals(PlaylistSourceField.PASSWORD, PlaylistSourceDraftPolicy.validation(incomplete)?.field)

        val playlist = PlaylistSourceDraftPolicy.build(incomplete.copy(password = "secret"))
        assertEquals(PlaylistType.XTREAM, playlist?.type)
        assertEquals("http://provider.example", playlist?.server)
    }

    @Test fun submissionNeverBuildsBeforeSuccessfulProviderTest() = runBlocking {
        var testCalls = 0
        val draft = PlaylistSourceDraft(
            method = PlaylistSourceMethod.URL,
            playlistUrl = "https://example.com/list.m3u",
        )

        val failed = submitPlaylistSource(draft) {
            testCalls += 1
            PlaylistConnectionTestResult(false, "Ο server δεν απάντησε.")
        }
        assertFalse(failed.successful)
        assertEquals(1, testCalls)

        val success = submitPlaylistSource(draft) {
            testCalls += 1
            PlaylistConnectionTestResult(true, "Έγκυρο M3U")
        }
        assertTrue(success.successful)
        assertNotNull(success.playlist)
        assertEquals(2, testCalls)
    }

    @Test fun providerFailuresBecomeActionableWithoutLeakingRawTransportText() {
        assertEquals(
            "Δεν βρέθηκε ο server. Έλεγξε προσεκτικά τη διεύθυνση.",
            PlaylistConnectionMessagePolicy.failure("java.net.UnknownHostException: private.provider.test"),
        )
        assertEquals(
            "Ο server απέρριψε τα στοιχεία. Έλεγξε όνομα χρήστη και κωδικό.",
            PlaylistConnectionMessagePolicy.failure("HTTP 401 Unauthorized"),
        )
        assertEquals(
            "Η σύνδεση απέτυχε. Έλεγξε τα στοιχεία και δοκίμασε ξανά.",
            PlaylistConnectionMessagePolicy.failure("internal stack detail"),
        )
    }
}
