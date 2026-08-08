package com.prelude.iptv.data

import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class MultiviewLaunchStoreTest {
    @After fun cleanup() = MultiviewLaunchStore.clearForTests()

    @Test fun tokenIsOneShotAndIntentSafe() {
        val launch = MultiviewLaunchStore.Launch(
            MultiviewLaunchStore.Stream("https://host/user/pass/1.ts", "A"),
            MultiviewLaunchStore.Stream("https://host/user/pass/2.ts", "B")
        )
        val token = MultiviewLaunchStore.put(launch, 1000)
        assertFalse(token.contains("host"))
        assertEquals(launch, MultiviewLaunchStore.consume(token, 1001))
        assertNull(MultiviewLaunchStore.consume(token, 1002))
    }

    @Test fun expiredTokenIsRejected() {
        val launch = MultiviewLaunchStore.Launch(
            MultiviewLaunchStore.Stream("a", "A"), MultiviewLaunchStore.Stream("b", "B")
        )
        val token = MultiviewLaunchStore.put(launch, 0)
        assertNull(MultiviewLaunchStore.consume(token, 60_001))
    }
}
