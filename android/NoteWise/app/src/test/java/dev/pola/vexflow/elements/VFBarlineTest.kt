package dev.pola.vexflow.elements

import dev.pola.vexflow.core.RecordingContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VFBarlineTest {

    private fun rig(type: VFBarlineType): Pair<VFBarline, RecordingContext> {
        val ctx = RecordingContext()
        val stave = VFStave(0f, 100f, 400f)
        val bl = VFBarline(type).apply {
            this.stave = stave
            this.x = 400f
        }
        return bl to ctx
    }

    @Test
    fun `NONE draws nothing`() {
        val (bl, ctx) = rig(VFBarlineType.NONE)
        bl.draw(ctx)
        assertEquals(0, ctx.strokeCalls.size)
    }

    @Test
    fun `SINGLE draws one stroke`() {
        val (bl, ctx) = rig(VFBarlineType.SINGLE)
        bl.draw(ctx)
        assertEquals(1, ctx.strokeCalls.size)
    }

    @Test
    fun `DOUBLE draws two strokes`() {
        val (bl, ctx) = rig(VFBarlineType.DOUBLE)
        bl.draw(ctx)
        assertEquals(2, ctx.strokeCalls.size)
    }

    @Test
    fun `fromString parses repeat-end`() {
        assertEquals(VFBarlineType.REPEAT_END, VFBarlineType.fromString("repeat-end"))
    }

    @Test
    fun `fromString unknown defaults to SINGLE`() {
        assertEquals(VFBarlineType.SINGLE, VFBarlineType.fromString("xyz"))
    }
}
