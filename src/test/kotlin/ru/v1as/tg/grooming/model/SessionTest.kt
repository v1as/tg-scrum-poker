package ru.v1as.tg.grooming.model

import java.time.LocalDateTime.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.v1as.tg.grooming.model.Voted.CLEARED
import ru.v1as.tg.grooming.model.Voted.VOTED
import ru.v1as.tg.starter.model.TgTestUser

class SessionTest {
    @Test
    fun shouldCreate() {
        val session = Session("session title", emptySet())
        assertThat(session.text()).contains("session title")

        val bob = TgTestUser(1, "bob")
        assertEquals(VOTED, session.vote("1", bob))
        assertThat(session.text()).contains("session title").contains("@bob $CARD")

        assertEquals(CLEARED, session.vote("1", bob))
        assertThat(session.text()).contains("session title").contains("@bob $WAITING")
    }

    @Test
    fun `should close`() {
        val session = Session("session title", emptySet())
        session.vote("5", TgTestUser(1, "bob"))
        session.vote("2", TgTestUser(1, "mary"))
        val john = TgTestUser(1, "john")
        session.vote("2", john)
        session.vote("2", john)
        assertThat(session.text()).containsSubsequence("@bob", "@mary", "@john")
        session.close()
        assertTrue(session.closed)
        assertThat(session.text())
            .contains("session title")
            .contains("@bob: 5")
            .contains("@mary: 2")
            .doesNotContain("john")
            .doesNotContain("Голосовали", "минут")
            .contains("Итог: 3.50")
    }

    @Test
    fun `Should calculate duration`() {
        val session = Session("session title", emptySet(), now().minusMinutes(5))
        val bob = TgTestUser(1, "bob")
        session.vote("5", bob)
        session.vote(TURN_OVER, bob)
        assertThat(session.text())
            .containsSubsequence("session title", "@bob: 5", "Голосовали 5 минут", "Итог: 5")
    }
}
