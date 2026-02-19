/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.squareup.stoic.helpers.*
import radiography.Radiography
import radiography.ScanScopes
import radiography.ViewStateRenderers

/**
 * A Stoic plugin that prints the view hierarchy using Radiography.
 *
 * Usage:
 *   stoic radiography.apk
 *
 * Options:
 *   --pii          Include PII (personally identifiable information) like text content
 *   --focused      Scan only the focused window instead of all windows
 *
 * Examples:
 *   stoic radiography.apk
 *   stoic radiography.apk --pii
 *   stoic radiography.apk --focused
 *   stoic radiography.apk --pii --focused
 */
fun main(args: Array<String>) {
  // Validate arguments
  val validArgs = setOf("--pii", "--focused")
  val invalidArgs = args.filterNot { it in validArgs }
  if (invalidArgs.isNotEmpty()) {
    System.err.println("Error: Unknown argument(s): ${invalidArgs.joinToString(", ")}")
    System.err.println()
    System.err.println("Usage: stoic radiography.apk [OPTIONS]")
    System.err.println()
    System.err.println("Options:")
    System.err.println("  --pii       Include PII (personally identifiable information) like text content")
    System.err.println("  --focused   Scan only the focused window instead of all windows")
    kotlin.system.exitProcess(1)
  }

  val includePii = args.contains("--pii")
  val focusedOnly = args.contains("--focused")

  eprintln("Radiography - View Hierarchy Scanner")
  eprintln("=====================================")
  if (includePii) {
    eprintln("Mode: Including PII (text content)")
  } else {
    eprintln("Mode: No PII (text content excluded)")
  }
  if (focusedOnly) {
    eprintln("Scope: Focused window only")
  } else {
    eprintln("Scope: All windows")
  }
  eprintln("")

  try {
    runOnMainLooper {
      val viewStateRenderers = if (includePii) {
        ViewStateRenderers.DefaultsIncludingPii
      } else {
        ViewStateRenderers.DefaultsNoPii
      }

      val scanScope = if (focusedOnly) {
        ScanScopes.FocusedWindowScope
      } else {
        ScanScopes.AllWindowsScope
      }

      val hierarchy = Radiography.scan(
        scanScope = scanScope,
        viewStateRenderers = viewStateRenderers
      )

      println(hierarchy)
    }
  } catch (e: Exception) {
    System.err.println("Error scanning view hierarchy: ${e.message}")
    e.printStackTrace()
  }
}
