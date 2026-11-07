package tj.soni.editor.core

data class Keyframe(
    val t: Float,
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val opacity: Float = 1f
)

object KeyframeEngine {
    /** Linear interpolation between keyframes; clamps outside the range. Frames must be sorted by t. */
    fun sample(frames: List<Keyframe>, t: Float): Keyframe {
        require(frames.isNotEmpty())
        if (t <= frames.first().t) return frames.first().copy(t = t)
        if (t >= frames.last().t) return frames.last().copy(t = t)
        val i = frames.indexOfLast { it.t <= t }
        val a = frames[i]
        val b = frames[i + 1]
        val k = (t - a.t) / (b.t - a.t)
        fun l(p: Float, q: Float) = p + (q - p) * k
        return Keyframe(t, l(a.x, b.x), l(a.y, b.y), l(a.scale, b.scale), l(a.rotation, b.rotation), l(a.opacity, b.opacity))
    }
}
