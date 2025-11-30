package dev.cheleb.ziotapir.laminar

import zio.*

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import dev.cheleb.ziotapir.*
import dev.cheleb.ziotapir.laminar.*
import io.github.nguyenyou.webawesome.laminar.*
import io.github.nguyenyou.webawesome.laminar.Button.WaButtonComponent
import org.scalajs.dom.HTMLElement
import sttp.model.Uri

/** A Laminar button that runs a ZIO effect on click.
  */
object ZButton {

  /** Creates a ZButton that runs the given ZIO effect when clicked.
    *
    * @param label
    * @param iconName
    * @param onClickEffect
    * @return
    */
  def apply[A](label: String, iconName: String)(
      onClickEffect: => ZIO[BackendClient, Throwable, A]
  ): ReactiveHtmlElement[WaButtonComponent & HTMLElement] =
    Button(_.variant.brand)(
      Icon(_.name := iconName)(),
      label,
      onClick --> { _ =>
        onClickEffect.run
      }
    )

    /** Creates a ZButton that runs the given ZIO effect when clicked, passing
      * the given URI.
      *
      * @param label
      * @param iconName
      * @param uri
      * @param onClickEffect
      * @return
      */
  def apply[A](label: String, iconName: String, uri: => Uri)(
      onClickEffect: => ZIO[BackendClient, Throwable, A]
  ): ReactiveHtmlElement[WaButtonComponent & HTMLElement] =
    Button(_.variant.brand)(
      Icon(_.name := iconName)(),
      label,
      onClick --> { _ =>
        onClickEffect.run(uri)
      }
    )
}
