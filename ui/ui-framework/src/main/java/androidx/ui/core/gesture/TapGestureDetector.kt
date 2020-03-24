/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.Modifier
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.anyPositionChangeConsumed
import androidx.ui.core.changedToDown
import androidx.ui.core.changedToUp
import androidx.ui.core.consumeDownChange
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.unit.IntPxSize
import androidx.ui.util.fastAny

/**
 * This gesture detector fires a callback when a traditional press is being released.  This is
 * generally the same thing as "onTap" or "onClick".
 *
 * More specifically, it will call [onTap] if:
 * - All of the first [PointerInputChange]s it receives during the [PointerEventPass.PostUp] pass
 *   have unconsumed down changes, thus representing new set of pointers, none of which have had
 *   their down events consumed.
 * - The last [PointerInputChange] it receives during the [PointerEventPass.PostUp] pass has
 *   an unconsumed up change.
 * - While it has at least one pointer touching it, no [PointerInputChange] has had any
 *   movement consumed (as that would indicate that something in the heirarchy moved and this a
 *   press should be cancelled.
 *
 *   @param onTap Called when a tap has occurred.
 */
// TODO(b/139020678): Probably has shared functionality with other press based detectors.
@Composable
fun TapGestureDetector(
    onTap: () -> Unit
): Modifier {
    val recognizer = remember { TapGestureRecognizer() }
    recognizer.onTap = onTap
    recognizer.consumeDownOnStart = false
    return PointerInputModifierImpl(recognizer)
}

internal class TapGestureRecognizer : PointerInputFilter() {
    /**
     * Called to indicate that a press gesture has successfully completed.
     *
     * This should be used to fire a state changing event as if a button was pressed.
     */
    lateinit var onTap: () -> Unit

    /**
     * True if down change should be consumed when start is called.  The default is true.
     */
    var consumeDownOnStart = true

    /**
     * True when we are primed to call [onTap] and may be consuming all down changes.
     */
    private var active = false

    override fun onPointerInput(
        changes: List<PointerInputChange>,
        pass: PointerEventPass,
        bounds: IntPxSize
    ): List<PointerInputChange> {

        var internalChanges = changes

        if (pass == PointerEventPass.PostUp) {

            if (internalChanges.all { it.changedToDown() }) {
                // If we have not yet started and all of the changes changed to down, we are
                // starting.
                active = true
            } else if (active && internalChanges.all { it.changedToUp() }) {
                // If we have started and all of the changes changed to up, we are stopping.
                active = false
                internalChanges = internalChanges.map { it.consumeDownChange() }
                onTap.invoke()
            } else if (!internalChanges.anyPointersInBounds(bounds)) {
                // If none of the pointers are in bounds of our bounds, we should reset and wait
                // till all pointers are changing to down.
                reset()
            }

            if (active && consumeDownOnStart) {
                // If we have started, we should consume the down change on all changes.
                internalChanges = internalChanges.map { it.consumeDownChange() }
            }
        }

        if (pass == PointerEventPass.PostDown && active &&
            internalChanges.fastAny { it.anyPositionChangeConsumed() }
        ) {
            // On the final pass, if we have started and any of the changes had consumed
            // position changes, we cancel.
            reset()
        }

        return internalChanges
    }

    override fun onCancel() {
        reset()
    }

    private fun reset() {
        active = false
    }
}