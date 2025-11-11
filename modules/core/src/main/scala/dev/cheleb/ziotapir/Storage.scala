package dev.cheleb.ziotapir

import org.scalajs.dom

object Storage {

  def set(key: String, value: String): Unit =
    dom.window.localStorage.setItem(key, value)

  def get(key: String): Option[String] =
    Option(dom.window.localStorage.getItem(key))
      .filter(_.nonEmpty)

  def remove(key: String): Unit =
    dom.window.localStorage.removeItem(key)

  def removeAll(): Unit =
    dom.window.localStorage.clear()

}
