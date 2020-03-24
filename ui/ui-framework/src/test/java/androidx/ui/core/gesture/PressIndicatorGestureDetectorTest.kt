/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ui.core.gesture

import androidx.ui.core.PointerEventPass
import androidx.ui.core.consumeDownChange
import androidx.ui.testutils.consume
import androidx.ui.testutils.down
import androidx.ui.testutils.invokeOverAllPasses
import androidx.ui.testutils.invokeOverPasses
import androidx.ui.testutils.moveBy
import androidx.ui.testutils.moveTo
import androidx.ui.testutils.up
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.ipx
import androidx.ui.unit.milliseconds
import androidx.ui.unit.px
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PressIndicatorGestureDetectorTest {

    private lateinit var recognizer: PressIndicatorGestureRecognizer

    @Before
    fun setup() {
        recognizer = PressIndicatorGestureRecognizer()
        recognizer.onStart = mock()
        recognizer.onStop = mock()
        recognizer.onCancel = mock()
    }

    // Verification of scenarios where onStart should not be called.

    @Test
    fun onPointerInput_downConsumed_onStartNotCalled() {
        recognizer::onPointerInput
            .invokeOverAllPasses(down(0, 0.milliseconds).consumeDownChange())
        verify(recognizer.onStart!!, never()).invoke(any())
    }

    @Test
    fun onPointerInput_disabledDown_onStartNotCalled() {
        recognizer.setEnabled(false)
        recognizer::onPointerInput
            .invokeOverAllPasses(down(0, 0.milliseconds).consumeDownChange())
        verify(recognizer.onStart!!, never()).invoke(any())
    }

    // Verification of scenarios where onStart should be called once.

    @Test
    fun onPointerInput_down_onStartCalledOnce() {
        recognizer::onPointerInput.invokeOverAllPasses(down(0, 0.milliseconds))
        verify(recognizer.onStart!!).invoke(any())
    }

    @Test
    fun onPointerInput_downDown_onStartCalledOnce() {
        var pointer0 = down(0)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0)
        pointer0 = pointer0.moveTo(1.milliseconds)
        val pointer1 = down(1, 1.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)

        verify(recognizer.onStart!!).invoke(any())
    }

    @Test
    fun onPointerInput_2Down1Up1Down_onStartCalledOnce() {
        var pointer0 = down(0)
        var pointer1 = down(1)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)
        pointer0 = pointer0.up(100.milliseconds)
        pointer1 = pointer1.moveTo(100.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)
        pointer0 = down(0, duration = 200.milliseconds)
        pointer1 = pointer1.moveTo(200.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)

        verify(recognizer.onStart!!).invoke(any())
    }

    @Test
    fun onPointerInput_1DownMoveOutside2ndDown_onStartOnlyCalledOnce() {
        var pointer0 = down(0, x = 0f, y = 0f)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, IntPxSize(5.ipx, 5.ipx))
        pointer0 = pointer0.moveTo(100.milliseconds, 10f, 0f)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, IntPxSize(5.ipx, 5.ipx))
        pointer0 = pointer0.moveTo(200.milliseconds)
        val pointer1 = down(1, x = 0f, y = 0f)

        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)

        verify(recognizer.onStart!!).invoke(any())
    }

    // Verification of scenarios where onStop should not be called.

    @Test
    fun onPointerInput_downMoveConsumedUp_onStopNotCalled() {
        var pointer = down(0, 0.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.moveTo(100.milliseconds, 5f).consume(1f)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.up(200.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)

        verify(recognizer.onStop!!, never()).invoke()
    }

    @Test
    fun onPointerInput_downConsumedUp_onStopNotCalled() {
        var pointer = down(0, 0.milliseconds).consumeDownChange()
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.up(100.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)

        verify(recognizer.onStop!!, never()).invoke()
    }

    @Test
    fun onPointerInput_2DownUp_onStopNotCalled() {
        var pointer0 = down(0)
        var pointer1 = down(1)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)
        pointer0 = pointer0.moveTo(100.milliseconds)
        pointer1 = pointer1.up(100.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)

        verify(recognizer.onStop!!, never()).invoke()
    }

    @Test
    fun onPointerInput_downDisabledUp_onStopNotCalled() {
        var pointer = down(0, 0.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        recognizer.setEnabled(false)
        pointer = pointer.up(100.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)

        verify(recognizer.onStop!!, never()).invoke()
    }

    @Test
    fun onPointerInput_downDisabledEnabledUp_onStopNotCalled() {
        var pointer = down(0, 0.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        recognizer.setEnabled(false)
        recognizer.setEnabled(true)
        pointer = pointer.up(100.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)

        verify(recognizer.onStop!!, never()).invoke()
    }

    // Verification of scenarios where onStop should be called once.

    @Test
    fun onPointerInput_downUp_onStopCalledOnce() {
        var pointer = down(0, 0.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.up(100.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)

        verify(recognizer.onStop!!).invoke()
    }

    @Test
    fun onPointerInput_downUpConsumed_onStopCalledOnce() {
        var pointer = down(0, 0.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.up(100.milliseconds).consumeDownChange()
        recognizer::onPointerInput.invokeOverAllPasses(pointer)

        verify(recognizer.onStop!!).invoke()
    }

    @Test
    fun onPointerInput_downMoveUp_onStopCalledOnce() {
        var pointer = down(0, 0.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.moveTo(100.milliseconds, 5f)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.up(200.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)

        verify(recognizer.onStop!!).invoke()
    }

    @Test
    fun onPointerInput_2Down2Up_onStopCalledOnce() {
        var pointer1 = down(0)
        var pointer2 = down(1)
        recognizer::onPointerInput.invokeOverAllPasses(pointer1, pointer2)
        pointer1 = pointer1.up(100.milliseconds)
        pointer2 = pointer2.up(100.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer1, pointer2)

        verify(recognizer.onStop!!).invoke()
    }

    // Verification of scenarios where onCancel should not be called.

    @Test
    fun onPointerInput_downConsumedMoveConsumed_onCancelNotCalled() {
        var pointer = down(0, 0.milliseconds).consumeDownChange()
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.moveBy(100.milliseconds, 5f).consume(1f)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)

        verify(recognizer.onCancel!!, never()).invoke()
    }

    @Test
    fun onPointerInput_downUp_onCancelNotCalled() {
        var pointer = down(0, 0.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.up(100.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)

        verify(recognizer.onCancel!!, never()).invoke()
    }

    @Test
    fun onPointerInput_downMoveUp_onCancelNotCalled() {
        var pointer = down(0, 0.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.moveTo(100.milliseconds, 5f)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.up(100.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)

        verify(recognizer.onCancel!!, never()).invoke()
    }

    @Test
    fun onPointerInput_2DownOneMoveOutsideOfBounds_onCancelNotCalled() {
        var pointer0 = down(0, x = 0f, y = 0f)
        var pointer1 = down(0, x = 4f, y = 4f)
        recognizer::onPointerInput
            .invokeOverAllPasses(pointer0, pointer1, size = IntPxSize(5.ipx, 5.ipx))
        pointer0 = pointer0.moveTo(100.milliseconds, 0f, 0f)
        pointer1 = pointer1.moveTo(100.milliseconds, 5f, 4f)
        recognizer::onPointerInput
            .invokeOverAllPasses(pointer0, pointer1, size = IntPxSize(5.ipx, 5.ipx))

        verify(recognizer.onCancel!!, never()).invoke()
    }

    @Test
    fun onPointerInput_notEnabledDownNotEnabled_onCancelNotCalled() {
        recognizer.setEnabled(false)
        recognizer::onPointerInput.invokeOverAllPasses(down(0, 0.milliseconds))
        recognizer.setEnabled(false)

        verify(recognizer.onCancel!!, never()).invoke()
    }

    // Verification of scenarios where onCancel should be called once.

    @Test
    fun onPointerInput_downMoveConsumed_onCancelCalledOnce() {
        var pointer = down(0, 0.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.moveBy(100.milliseconds, 5f).consume(1f)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun onPointerInput_downMoveConsumedMoveConsumed_onCancelCalledOnce() {
        var pointer = down(0, 0.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.moveBy(100.milliseconds, 5f).consume(1f)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.moveBy(100.milliseconds, 5f).consume(1f)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun onPointerInput_2Down2MoveConsumed_onCancelCalledOnce() {
        var pointer0 = down(0)
        var pointer1 = down(1)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)
        pointer0 = pointer0.moveBy(100.milliseconds, 5f).consume(1f)
        pointer1 = pointer1.moveBy(100.milliseconds, 5f).consume(1f)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun onPointerInput_2Down1MoveConsumedTheOtherMoveConsume_onCancelCalledOnce() {
        var pointer0 = down(0)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0)
        pointer0 = pointer0.moveTo(100.milliseconds)
        var pointer1 = down(1, duration = 100.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)
        pointer0 = pointer0.moveBy(100L.milliseconds, 5f).consume(5f)
        pointer1 = pointer1.moveBy(100L.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)
        pointer0 = pointer0.moveBy(100L.milliseconds)
        pointer1 = pointer1.moveBy(100L.milliseconds, 5f).consume(5f)
        recognizer::onPointerInput.invokeOverAllPasses(pointer0, pointer1)

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun onPointerInput_1DownMoveOutsideOfBoundsLeft_onCancelCalledOnce() {
        var pointer0 = down(0, x = 0f, y = 0f)
        recognizer::onPointerInput
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))
        pointer0 = pointer0.moveTo(100.milliseconds, -1f, 0f)
        recognizer::onPointerInput
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun onPointerInput_1DownMoveOutsideOfBoundsRight_onCancelCalledOnce() {
        var pointer0 = down(0, x = 0f, y = 0f)
        recognizer::onPointerInput
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))
        pointer0 = pointer0.moveTo(100.milliseconds, 1f, 0f)
        recognizer::onPointerInput
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun onPointerInput_1DownMoveOutsideOfBoundsUp_onCancelCalledOnce() {
        var pointer0 = down(0, x = 0f, y = 0f)
        recognizer::onPointerInput
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))
        pointer0 = pointer0.moveTo(100.milliseconds, 0f, -1f)
        recognizer::onPointerInput
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun onPointerInput_1DownMoveOutsideOfBoundsDown_onCancelCalledOnce() {
        var pointer0 = down(0, x = 0f, y = 0f)
        recognizer::onPointerInput
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))
        pointer0 = pointer0.moveTo(100.milliseconds, 0f, 1f)
        recognizer::onPointerInput
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun onPointerInput_2DownBothMoveOutsideOfBounds_onCancelCalledOnce() {
        var pointer0 = down(0, x = 0f, y = 4f)
        var pointer1 = down(1, x = 4f, y = 0f)
        recognizer::onPointerInput
            .invokeOverAllPasses(pointer0, pointer1, size = IntPxSize(5.ipx, 5.ipx))
        pointer0 = pointer0.moveTo(100.milliseconds, 0f, 5f)
        pointer1 = pointer1.moveTo(100.milliseconds, 5f, 0f)
        recognizer::onPointerInput
            .invokeOverAllPasses(pointer0, pointer1, size = IntPxSize(5.ipx, 5.ipx))

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun onPointerInput_1DownMoveOutsideBoundsThenInsideThenOutside_onCancelCalledOnce() {
        var pointer0 = down(0, x = 0f, y = 0f)
        recognizer::onPointerInput
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))
        pointer0 = pointer0.moveTo(100.milliseconds, 0f, 1f)
        recognizer::onPointerInput
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))
        pointer0 = pointer0.moveTo(200.milliseconds, 0f, 0f)
        recognizer::onPointerInput
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))
        pointer0 = pointer0.moveTo(300.milliseconds, 0f, 1f)
        recognizer::onPointerInput
            .invokeOverAllPasses(pointer0, IntPxSize(1.ipx, 1.ipx))

        verify(recognizer.onCancel!!).invoke()
    }

    @Test
    fun onPointerInput_1DownMoveOutsideBoundsUpTwice_onCancelCalledTwice() {
        var time = 0L

        repeat(2) {
            var pointer = down(0, x = 0f, y = 0f)
            recognizer::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
            pointer = pointer.moveTo(time.milliseconds, 0f, 1f)
            recognizer::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
            pointer = pointer.up(time.milliseconds)
            recognizer::onPointerInput.invokeOverAllPasses(pointer, IntPxSize(1.ipx, 1.ipx))
            time += 100L
        }

        verify(recognizer.onCancel!!, times(2)).invoke()
    }

    @Test
    fun onPointerInput_downDisabled_onCancelCalledOnce() {
        val pointer = down(0, 0.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        recognizer.setEnabled(false)

        verify(recognizer.onCancel!!).invoke()
    }

    // Verification of correct position returned by onStart.

    @Test
    fun onPointerInput_down_downPositionIsCorrect() {
        recognizer::onPointerInput
            .invokeOverAllPasses(down(0, 0.milliseconds, x = 13f, y = 17f))
        verify(recognizer.onStart!!).invoke(PxPosition(13.px, 17f.px))
    }

    // Verification of correct consumption behavior.

    @Test
    fun onPointerInput_down_downChangeConsumedDuringPostUp() {
        var pointer = down(0, 0.milliseconds)
        pointer = recognizer::onPointerInput.invokeOverPasses(
            pointer,
            PointerEventPass.InitialDown,
            PointerEventPass.PreUp,
            PointerEventPass.PreDown
        )
        assertThat(pointer.consumed.downChange, `is`(false))

        pointer = recognizer::onPointerInput.invoke(
            listOf(pointer),
            PointerEventPass.PostUp,
            IntPxSize(0.ipx, 0.ipx)
        ).first()
        assertThat(pointer.consumed.downChange, `is`(true))
    }

    @Test
    fun onPointerInput_disabledDown_noDownChangeConsumed() {
        recognizer.setEnabled(false)
        var pointer = down(0, 0.milliseconds)
        pointer = recognizer::onPointerInput.invokeOverAllPasses(pointer)
        assertThat(pointer.consumed.downChange, `is`(false))
    }

    // Verification of correct cancellation handling.

    @Test
    fun onCancel_justCancel_noCallbacksCalled() {
        recognizer.onCancel()

        verifyNoMoreInteractions(recognizer.onStart, recognizer.onStop, recognizer.onCancel)
    }

    @Test
    fun onCancel_downConsumedCancel_noCallbacksCalled() {
        recognizer::onPointerInput
            .invokeOverAllPasses(down(0, 0.milliseconds).consumeDownChange())
        recognizer.onCancel()

        verifyNoMoreInteractions(recognizer.onStart, recognizer.onStop, recognizer.onCancel)
    }

    @Test
    fun onCancel_downCancel_justStartAndCancelCalledInOrderOnce() {
        recognizer::onPointerInput.invokeOverAllPasses(down(0, 0.milliseconds))
        recognizer.onCancel()

        inOrder(recognizer.onStart!!, recognizer.onCancel!!) {
            verify(recognizer.onStart!!).invoke(any())
            verify(recognizer.onCancel!!).invoke()
        }
        verifyNoMoreInteractions(recognizer.onStart, recognizer.onStop, recognizer.onCancel)
    }

    @Test
    fun onCancel_downUpCancel_justStartAndStopCalledInOrderOnce() {
        var pointer = down(0, 0.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.up(100.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        recognizer.onCancel()

        inOrder(recognizer.onStart!!, recognizer.onStop!!) {
            verify(recognizer.onStart!!).invoke(any())
            verify(recognizer.onStop!!).invoke()
        }
        verifyNoMoreInteractions(recognizer.onStart, recognizer.onStop, recognizer.onCancel)
    }

    @Test
    fun onCancel_downMoveCancel_justStartAndCancelCalledInOrderOnce() {
        var pointer = down(0, 0.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.moveTo(50.milliseconds, 1f)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        recognizer.onCancel()

        inOrder(recognizer.onStart!!, recognizer.onCancel!!) {
            verify(recognizer.onStart!!).invoke(any())
            verify(recognizer.onCancel!!).invoke()
        }
        verifyNoMoreInteractions(recognizer.onStart, recognizer.onStop, recognizer.onCancel)
    }

    @Test
    fun onCancel_downMoveConsumedCancel_justStartAndCancelCalledInOrderOnce() {
        var pointer = down(0, 0.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        pointer = pointer.moveTo(50.milliseconds, 1f).consume(1f)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        recognizer.onCancel()

        inOrder(recognizer.onStart!!, recognizer.onCancel!!) {
            verify(recognizer.onStart!!).invoke(any())
            verify(recognizer.onCancel!!).invoke()
        }
        verifyNoMoreInteractions(recognizer.onStart, recognizer.onStop, recognizer.onCancel)
    }

    @Test
    fun onCancel_downThenCancelThenDown_justStartCancelStartCalledInOrderOnce() {
        var pointer = down(0, 0.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)
        recognizer.onCancel()
        pointer = down(0, 0.milliseconds)
        recognizer::onPointerInput.invokeOverAllPasses(pointer)

        inOrder(recognizer.onStart!!, recognizer.onCancel!!) {
            verify(recognizer.onStart!!).invoke(any())
            verify(recognizer.onCancel!!).invoke()
            verify(recognizer.onStart!!).invoke(any())
        }
        verifyNoMoreInteractions(recognizer.onStart, recognizer.onStop, recognizer.onCancel)
    }
}