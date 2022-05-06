package com.google.summit.ast

/**
 * Source range within a file.
 *
 * A special "unknown" value can also be constructed.
 *
 * @property startLine the line where the source location starts
 * @property startColumn the column where the source location starts
 * @property endLine the line where the source location ends
 * @property endColumn the column where the source location ends
 */
data class SourceLocation(
  val startLine: Int?,
  val startColumn: Int?,
  val endLine: Int?,
  val endColumn: Int?,
) {
  /** Returns whether this is an unknown source location. */
  fun isUnknown(): Boolean = (startLine == null)

  /** Prints known source locations as a range and unknown ones as such. */
  override fun toString(): String {
    return if (isUnknown()) {
      "<unknown>"
    } else {
      "[$startLine:$startColumn,$endLine:$endColumn]"
    }
  }

  companion object {
    /** An unknown source location. */
    val UNKNOWN = SourceLocation(null, null, null, null)
  }
}
