package com.prelude.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Test

class VlcStateMapperTest {
    @Test fun `new VLC frame advances the shared rendered frame counter`() {
        val result = VlcStateMapper.merge(
            previous = PlaybackEngine.State(renderedFrames = 7),
            snapshot = VlcBackend.Snapshot(renderedFrame = true),
            audioTracks = emptyList(),
            subtitleTracks = emptyList(),
            renderedFrameAdvanced = true,
        )

        assertEquals(8, result.renderedFrames)
    }

    @Test fun `repeated VLC state publication does not advance the frame counter`() {
        val result = VlcStateMapper.merge(
            previous = PlaybackEngine.State(renderedFrames = 7),
            snapshot = VlcBackend.Snapshot(renderedFrame = true),
            audioTracks = emptyList(),
            subtitleTracks = emptyList(),
            renderedFrameAdvanced = false,
        )

        assertEquals(7, result.renderedFrames)
    }
}
