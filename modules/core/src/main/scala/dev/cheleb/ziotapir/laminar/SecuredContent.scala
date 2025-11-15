package dev.cheleb.ziotapir.laminar

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import dev.cheleb.ziojwt.WithToken
import dev.cheleb.ziotapir.Session
import org.scalajs.dom.*

trait SecuredContent[UserToken <: WithToken](using
    session: Session[UserToken]
):

  def notlogged: ReactiveHtmlElement[HTMLElement] = h1("Please log")

  def securedContent(
      userToken: UserToken
  ): ReactiveHtmlElement[HTMLDivElement]

  def content() =
    div(
      child <-- session(notlogged) { userToken =>
        securedContent(userToken)
      }
    )

  def apply(): ReactiveHtmlElement[HTMLDivElement] =
    content()
