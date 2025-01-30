package ru.v1as.tg.grooming.model

import java.time.LocalDateTime.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.v1as.tg.grooming.model.Voted.CHANGED
import ru.v1as.tg.grooming.model.Voted.CLEARED
import ru.v1as.tg.grooming.model.Voted.VOTED
import ru.v1as.tg.starter.model.TgTestUser

private val bob = TgTestUser(1, "bob")
private val mary = TgTestUser(2, "mary")
private val john = TgTestUser(3, "john")
private val zakh = TgTestUser(4, "zakh")
private val dev = EstimationRole("DEV", "🔧")
private val qa = EstimationRole("QA", "💎")

class SessionTest {
    @Test
    fun shouldCreate() {
        val session = Session("session title", -1)
        assertThat(session.text()).contains("session title")

        assertEquals(VOTED, session.vote("1", bob))
        assertThat(session.text()).contains("session title").contains("@bob $CARD")

        assertEquals(CLEARED, session.vote("1", bob))
        assertThat(session.text()).contains("session title").contains("@bob $WAITING")
    }

    @Test
    fun `should close`() {
        val session = Session("session title", -1)
        session.vote("5", bob)
        session.vote("2", mary)
        session.vote("2", john)
        session.vote("2", john)
        session.vote(COFFEE, zakh)
        assertThat(session.text()).containsSubsequence("@bob", "@mary", "@john", "@zakh")
        session.close()
        assertTrue(session.closed)
        assertThat(session.text())
            .contains("session title")
            .contains("@bob: 5")
            .contains("@mary: 2")
            .contains("@zakh: $COFFEE")
            .doesNotContain("john")
            .doesNotContain("Голосовали", "минут")
            .contains("Итог: 3.5  ~  3")
    }

    @Test
    fun `Should remove vote after closing`() {
        val session = Session("session title", -1)
        session.vote("5", bob)
        session.vote("2", mary)

        session.vote("5", bob)
        session.close()

        assertThat(session.text()).doesNotContain("bob")
    }

    @Test
    fun `Should close after reset`() {
        val session = Session("session title", -1)
        session.vote("5", bob)
        session.resetVotes()
        session.close()
        assertThat(session.voters()).contains(bob)
        assertThat(session.text())
            .contains("session title")
            .contains("bob", WAITING)
            .doesNotContain("5")
    }

    @Test
    fun `Should comment votes`() {
        val session = Session("session title", -1)
        session.vote("1", bob)
        session.vote("1", mary)
        session.vote("1", john)
        session.vote("5", zakh)
        session.close()
        assertThat(session.text())
            .contains("@zakh: 5 \uD83D\uDCAC")
            .doesNotContain("@mary: 1 \uD83D\uDCAC")
            .doesNotContain("@john: 1 \uD83D\uDCAC")
            .doesNotContain("@bob: 1 \uD83D\uDCAC")
    }

    @Test
    fun `Should not write best match if unnecessary`() {
        val session = Session("session title", -1)
        session.vote("5", bob)
        session.vote("21", mary)
        session.close()
        assertThat(session.text()).doesNotContain("  ~  ")
    }

    @Test
    fun `Should write best match if necessary`() {
        val session = Session("session title", -1)
        session.vote("13", bob)
        session.vote("21", mary)
        session.close()
        assertThat(session.text()).contains("  ~  21")
    }

    @Test
    fun `Should calculate duration`() {
        val session = Session("session title", -1, started = now().minusMinutes(5))
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
        val session = Session("session title", -1)
        votes.split("-").forEachIndexed { i, vote ->
            session.vote(vote, TgTestUser(i.toLong(), "user $i"))
        }
        assertEquals(expected, session.needDiscussion())
    }

    @Test
    fun `Should estimate days and then vote`() {
        val session = Session("session title", -1)

        assertEquals(VOTED, session.estimate(dev, "3", bob))
        assertThat(session.text()).containsSubsequence("session title", "@bob $TIMER")

        session.vote("5", bob)
        assertThat(session.text()).containsSubsequence("session title", "@bob $CARD$TIMER")

        session.vote("5", bob)
        assertThat(session.text()).containsSubsequence("session title", "@bob $TIMER")

        assertEquals(CHANGED, session.estimate(dev, "5", bob))
        assertThat(session.text()).containsSubsequence("session title", "@bob $TIMER")

        session.vote("5", bob)
        assertThat(session.text()).containsSubsequence("session title", "@bob $CARD$TIMER")
    }

    @Test
    fun `Should vote and then estimate`() {
        val session = Session("session title", -1)

        session.vote("5", bob)
        assertThat(session.text()).containsSubsequence("session title", "@bob $CARD")

        assertEquals(VOTED, session.estimate(dev, "3", bob))
        assertThat(session.text()).containsSubsequence("session title", "@bob $CARD$TIMER")

        session.close()
        assertThat(session.text()).containsSubsequence("session title", "@bob: 5(${dev.emoji}3)")
    }

    @Test
    fun `Should vote and estimate coffee`() {
        val session = Session("session title", -1)

        session.vote(COFFEE, bob)
        assertThat(session.text()).containsSubsequence("session title", "@bob $CARD")

        session.estimate(qa, COFFEE, bob)
        assertThat(session.text()).containsSubsequence("session title", "@bob $CARD$TIMER")

        session.close()
        assertThat(session.text())
            .containsSubsequence("session title", "@bob: $COFFEE(${qa.emoji}$COFFEE)")
    }

    @Test
    fun `Should only estimate`() {
        val session = Session("session title", -1)

        assertEquals(VOTED, session.estimate(dev, "3", bob))
        assertEquals(VOTED, session.estimate(dev, "4", john))
        assertEquals(VOTED, session.estimate(qa, "5", mary))
        session.vote(TURN_OVER, bob)
        assertThat(session.text())
            .containsSubsequence(
                "session title", "@bob: (🔧3)", "@mary: (💎5)", "🔧 DEV 3.5  ~  4", "💎 QA 5.0")
            .doesNotContain("Итог")
            .doesNotContain("Оценить время")
    }

    @Test
    fun `Should copy voters estimate`() {
        val session = Session("session title", -1)
        assertEquals(VOTED, session.estimate(dev, "3", bob))
        session.vote("3", bob)
        session.vote(TURN_OVER, bob)

        val session2 = Session("session title2", -1, session.voters())
        assertThat(session2.text()).contains("session title2").contains("@bob $WAITING")
    }
}
