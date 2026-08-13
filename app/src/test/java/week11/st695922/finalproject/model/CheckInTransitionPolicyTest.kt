package week11.st695922.finalproject.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CheckInTransitionPolicyTest {

    @Test
    fun `enter with no active station checks in`() {
        assertEquals(
            CheckInOperation.CHECK_IN,
            decide(active = "", target = "oakville", wantsCheckIn = true)
        )
    }

    @Test
    fun `duplicate enter is ignored`() {
        assertEquals(
            CheckInOperation.NO_OP,
            decide(active = "oakville", target = "oakville", wantsCheckIn = true)
        )
    }

    @Test
    fun `enter at another station switches`() {
        assertEquals(
            CheckInOperation.SWITCH_STATION,
            decide(active = "bronte", target = "oakville", wantsCheckIn = true)
        )
    }

    @Test
    fun `exit from active station checks out`() {
        assertEquals(
            CheckInOperation.CHECK_OUT,
            decide(active = "oakville", target = "oakville", wantsCheckIn = false)
        )
    }

    @Test
    fun `exit from unrelated station is ignored`() {
        assertEquals(
            CheckInOperation.NO_OP,
            decide(active = "oakville", target = "bronte", wantsCheckIn = false)
        )
    }

    @Test
    fun `automatic transition is ignored when preference is off`() {
        assertEquals(
            CheckInOperation.NO_OP,
            decide(
                active = "",
                target = "oakville",
                wantsCheckIn = true,
                automatic = true,
                automaticEnabled = false
            )
        )
    }

    private fun decide(
        active: String,
        target: String,
        wantsCheckIn: Boolean,
        automatic: Boolean = false,
        automaticEnabled: Boolean = true
    ): CheckInOperation = CheckInTransitionPolicy.decide(
        activeStationId = active,
        targetStationId = target,
        wantsCheckIn = wantsCheckIn,
        automatic = automatic,
        automaticEnabled = automaticEnabled
    )
}
