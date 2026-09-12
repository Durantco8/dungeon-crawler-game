package com.durantco.dungeon.sim;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The command line entry point, driven the way a terminal would drive it. */
class SimulateMainTest {

  private static String runMain(String... args) {
    PrintStream originalOut = System.out;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
      Simulate.main(args);
    } finally {
      System.setOut(originalOut);
    }
    return captured.toString(StandardCharsets.UTF_8);
  }

  @Test
  void helpPrintsTheUsageAndRunsNothing() {
    String output = runMain("--help");
    assertTrue(output.contains("--games"));
    assertTrue(!output.contains("Clear rate by level"), "Help should not play any games");
  }

  @Test
  @DisplayName("a small batch prints a complete report")
  void printsAReport() {
    String output = runMain("--games", "3", "--agent", "exit-runner", "--max-turns", "20");

    assertTrue(output.contains("Dungeon Crawler simulation"));
    assertTrue(output.contains("exit-runner"));
    assertTrue(output.contains("Clear rate by level"));
    assertTrue(output.contains("Survival turns"));
    assertTrue(output.contains("Generator"));
  }

  @Test
  void acceptsAHardModeRun() {
    String output = runMain("--games", "2", "--max-turns", "15", "--hard");
    assertTrue(output.contains("HARD"));
  }

  @Test
  void acceptsAnExplicitSeed() {
    String output = runMain("--games", "2", "--seed", "4242", "--max-turns", "10");
    assertTrue(output.contains("4242"));
  }

  @Test
  @DisplayName("defaults apply when nothing is passed for an option")
  void usesItsDefaults() {
    String output = runMain("--games", "2", "--max-turns", "10");
    assertTrue(output.contains("treasure-hunter"), "The default agent should be named in the report");
    assertTrue(output.contains("EASY"));
  }

  @Test
  void rejectsANonNumericCount() {
    assertThrows(IllegalArgumentException.class, () -> runMain("--games", "lots"));
  }

  @Test
  void rejectsANonNumericSeed() {
    assertThrows(IllegalArgumentException.class, () -> runMain("--seed", "later"));
  }

  @Test
  void rejectsAnUnknownAgent() {
    assertThrows(
        IllegalArgumentException.class, () -> runMain("--games", "1", "--agent", "mastermind"));
  }
}
