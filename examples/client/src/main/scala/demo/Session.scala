package demo

import com.raquo.laminar.api.L.*
import dev.cheleb.ziotapir.laminar.SecuredContent
import zio.json.*
import io.github.nguyenyou.webawesome.laminar.*
import org.scalajs.dom.HTMLDivElement
import com.raquo.laminar.nodes.ReactiveHtmlElement

import zio.json.JsonEncoder

def sessionManagement =

  val securedContent = new SecuredContent {
    override def notlogged = div(
      h1("Please log"),
      div(
        Button(_.variant.neutral)(
          "Login",
          onClick --> (_ =>
            session.saveToken(
              UserToken("my-secret-token", "Olivier").toJson
            )
          )
        )
      )
    )

    override def securedContent(
        userToken: UserToken
    ): ReactiveHtmlElement[HTMLDivElement] =
      div(
        h2(s"Welcome ${userToken.name}"),
        p("This is secured content."),
        p(
          "You can see this content because you are logged in and your session is active."
        ),
        p(
          "💡 Try to refresh the page or close and reopen the browser. Your session should persist thanks to local storage."
        ),
        p(
          "When endpoint calls are made, to a secured endpoint, the token associated with the issuer, will be automatically attached."
        ),
        Button(_.variant.danger)(
          "Log out",
          onClick --> (_ => session.clearUserState())
        )
      )
  }

  div(
    onMountCallback(_ => session.loadUserState()),
    h2("Session Management Demo"),
    p(
      "This demo shows how to manage user sessions using local storage."
    ),
    child.maybe <-- session.whenActive:
      Callout(
        _.slots.icon(Icon(_.name := "circle-info")())
      )(
        "You are now logged in. Enjoy the secured content",
        br(),
        " 💡 Check local storage."
      )
  ).amend(securedContent())
