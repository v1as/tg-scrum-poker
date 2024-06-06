package ru.v1as.tg.grooming.model

import java.time.LocalDateTime.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.v1as.tg.grooming.model.Voted.CLEARED
import ru.v1as.tg.grooming.model.Voted.VOTED
import ru.v1as.tg.starter.model.TgTestUser

class SessionTest {
    @Test
    fun shouldCreate() {
        val session = Session("session title")
        assertThat(session.text()).contains("session title")

        val bob = TgTestUser(1, "bob")
        assertEquals(VOTED, session.vote("1", bob))
        assertThat(session.text()).contains("session title").contains("@bob $CARD")

        assertEquals(CLEARED, session.vote("1", bob))
        assertThat(session.text()).contains("session title").contains("@bob $WAITING")
    }

    @Test
    fun `should close`() {
        val session = Session("session title")
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
            .contains("Итог: 3.5  ~  3")
    }

    @Test
    fun `Should close after reset`() {
        val session = Session("session title")
        val bob = TgTestUser(1, "bob")
        session.vote("5", bob)
        session.resetVotes()
        session.close()
        assertThat(session.voters()).contains(bob)
        assertThat(session.text())
            .contains("session title")
            .contains("bob", WAITING)
            .doesNotContain("5")
            .contains("Итог: 0.0  ~  1")
    }

    @Test
    fun `Should comment votes`() {
        val session = Session("session title")
        session.vote("1", TgTestUser(1, "bob"))
        session.vote("1", TgTestUser(1, "mary"))
        session.vote("1", TgTestUser(1, "john"))
        session.vote("5", TgTestUser(1, "zakh"))
        session.close()
        assertThat(session.text())
            .contains("@zakh: 5 \uD83D\uDCAC")
            .doesNotContain("@mary: 1 \uD83D\uDCAC")
            .doesNotContain("@john: 1 \uD83D\uDCAC")
            .doesNotContain("@bob: 1 \uD83D\uDCAC")
    }

    @Test
    fun `Should not write best match if unnecessary`() {
        val session = Session("session title")
        session.vote("5", TgTestUser(1, "bob"))
        session.vote("21", TgTestUser(1, "mary"))
        session.close()
        assertThat(session.text()).doesNotContain("  ~  ")
    }

    @Test
    fun `Should write best match if necessary`() {
        val session = Session("session title")
        session.vote("13", TgTestUser(1, "bob"))
        session.vote("21", TgTestUser(1, "mary"))
        session.close()
        assertThat(session.text()).contains("  ~  21")
    }

    @Test
    fun `Should calculate duration`() {
        val session = Session("session title", started = now().minusMinutes(5))
        val bob = TgTestUser(1, "bob")
        session.vote("5", bob)
        session.vote(TURN_OVER, bob)
        assertThat(session.text())
            .containsSubsequence("session title", "@bob: 5", "Голосовали 5 минут", "Единогласно: 5")
    }

    @ParameterizedTest
    @CsvSource(
        "1-2, false",
        "1-5, true",
        "8-8-8-5, false",
    )
    fun `Need discussion test`(votes: String, expected: Boolean) {
        val session = Session("session title")
        votes.split("-").forEachIndexed { i, vote ->
            session.vote(vote, TgTestUser(i.toLong(), "user $i"))
        }
        assertEquals(expected, session.needDiscussion())
    }
}
