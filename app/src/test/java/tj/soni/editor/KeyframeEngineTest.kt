package tj.soni.editor

import org.junit.Assert.assertEquals
import org.junit.Test
import tj.soni.editor.core.Keyframe
import tj.soni.editor.core.KeyframeEngine

class KeyframeEngineTest {
    private val frames = listOf(
        Keyframe(0f, x = 0f, opacity = 0f),
        Keyframe(2f, x = 1f, opacity = 1f)
    )

    @Test fun interpolatesLinearly() {
        val k = KeyframeEngine.sample(frames, 1f)
        assertEquals(0.5f, k.x, 1e-6f)
        assertEquals(0.5f, k.opacity, 1e-6f)
    }

    @Test fun clampsOutsideRange() {
        assertEquals(0f, KeyframeEngine.sample(frames, -1f).x, 1e-6f)
        assertEquals(1f, KeyframeEngine.sample(frames, 5f).x, 1e-6f)
    }
}
