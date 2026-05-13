package me.whereareiam.spawner.task

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RunServerGroupTaskTest {
	@Test
	fun `formats regular process output with prefix`() {
		assertEquals(
			"[proxy] 11:15:09 [INFO] Command not found",
			formatProcessLogLine("proxy", "11:15:09 [INFO] Command not found")
		)
	}

	@Test
	fun `ignores blank process output`() {
		assertNull(formatProcessLogLine("proxy", ""))
		assertNull(formatProcessLogLine("proxy", "  "))
	}

	@Test
	fun `ignores prompt only process output`() {
		assertNull(formatProcessLogLine("proxy", ">"))
		assertNull(formatProcessLogLine("proxy", ">>"))
		assertNull(formatProcessLogLine("proxy", " > "))
	}
}
