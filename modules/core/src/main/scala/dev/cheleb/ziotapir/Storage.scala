package dev.cheleb.ziotapir

import org.scalajs.dom

/** A simple storage utility using the browser's localStorage.
  */
object Storage {

  /** Sets a value in localStorage for the given key.
    *
    * @param key
    *   the key to set
    * @param value
    *   the value to set
    */
  def set(key: String, value: String): Unit =
    dom.window.localStorage.setItem(key, value)

  /** Gets a value from localStorage for the given key.
    *
    * @param key
    *   the key to get
    * @return
    *   an option containing the value if it exists and is non-empty
    */
  def get(key: String): Option[String] =
    Option(dom.window.localStorage.getItem(key))
      .filter(_.nonEmpty)

  /** Removes a value from localStorage for the given key. *
    * @param key
    *   the key to remove
    */
  def remove(key: String): Unit =
    dom.window.localStorage.removeItem(key)

  /** Removes all values from localStorage.
    */
  def removeAll(): Unit =
    dom.window.localStorage.clear()

}
