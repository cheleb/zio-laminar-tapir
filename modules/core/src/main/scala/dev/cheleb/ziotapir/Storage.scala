package dev.cheleb.ziotapir

import zio.json.*

import org.scalajs.dom

object Storage {

  def set[A: JsonEncoder](key: String, value: A): Unit =
    dom.window.localStorage.setItem(key, value.toJson)

  def get[A: JsonDecoder](key: String): Option[A] =
    Option(dom.window.localStorage.getItem(key))
      .filter(_.nonEmpty)
      .flatMap(_.fromJson[A].toOption)

  def remove(key: String): Unit =
    dom.window.localStorage.removeItem(key)

  def removeAll(): Unit =
    dom.window.localStorage.clear()

}
