package dev.cheleb.ziotapir.laminar

import dev.cheleb.ziotapir.Session
import dev.cheleb.ziojwt.WithToken

import com.raquo.laminar.api.L.*

import org.scalajs.dom.*
import com.raquo.laminar.nodes.ReactiveHtmlElement

trait SecuredContent(using
    session: Session[WithToken]
):

  def notlogged: ReactiveHtmlElement[HTMLElement] = h1("Please log")

  def securedContent(userToken: WithToken): ReactiveHtmlElement[HTMLDivElement]

  def content() =
    div(
      child <-- session(notlogged) { userToken =>
        securedContent(userToken)
      }
    )

  def apply(): ReactiveHtmlElement[HTMLDivElement] =
    content()
