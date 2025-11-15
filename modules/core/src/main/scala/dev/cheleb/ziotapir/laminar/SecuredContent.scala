package dev.cheleb.ziotapir.laminar

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import dev.cheleb.ziojwt.WithToken
import dev.cheleb.ziotapir.Session
import org.scalajs.dom.*

/** A trait to define secured content for Laminar applications.
  *
  * @tparam UserToken
  *   the type of the user token, which should extend
  *   [[dev.cheleb.ziojwt.WithToken]].
  */
trait SecuredContent[UserToken <: WithToken](using
    session: Session[UserToken]
):

  /** The content to show when the user is not logged in.
    *
    * Should be overridden to provide custom content.
    */
  protected def notlogged: ReactiveHtmlElement[HTMLElement] = h1("Please log")

  /** The secured content to show when the user is logged in.
    *
    * Should be overridden to provide custom content.
    *
    * @param userToken
    *   the user token
    * @return
    */
  protected def securedContent(
      userToken: UserToken
  ): ReactiveHtmlElement[HTMLDivElement]

  /** An initialization method that is called when the component is mounted.
    *
    * Can be overridden to provide custom initialization logic.
    */
  protected def init: Unit = ()

  /** The main component that shows either the secured content or the not
    * logged-in content.
    *
    * @return
    */
  final def apply(): ReactiveHtmlElement[HTMLDivElement] =
    div(
      child <-- session(notlogged) { userToken =>
        div(
          onMountCallback(_ => init),
          securedContent(userToken)
        )
      }
    )
