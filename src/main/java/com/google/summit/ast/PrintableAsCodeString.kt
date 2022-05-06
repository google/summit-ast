package com.google.summit.ast

/**
 * This interface is for nodes that can print their subtree as an Apex code string.
 *
 * Not every node corresponds to actual syntax in a well-formed Apex compilation unit. For example,
 * anonymous code blocks and special callsites may utilize special name identifiers for internal
 * use. These identifiers would never be printed in the code of their parent node, but the child
 * nodes may still return a code-like string for testing or debugging.
 */
interface PrintableAsCodeString {
  /** Returns the node's subtree printed as an Apex code string. */
  fun asCodeString(): String
}
