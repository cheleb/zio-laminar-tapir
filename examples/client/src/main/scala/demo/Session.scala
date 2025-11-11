package demo

import com.raquo.laminar.api.L.*
import dev.cheleb.ziotapir.laminar.SecuredContent
import zio.json.*
import io.github.nguyenyou.webawesome.laminar.*
import org.scalajs.dom.HTMLDivElement
import com.raquo.laminar.nodes.ReactiveHtmlElement
import dev.cheleb.ziojwt.WithToken
import zio.json.JsonEncoder

def sessionManagement =
  new SecuredContent:
    override def notlogged = div(
      h1("Please log"),
      div(
        p("This demo shows session management using local storage."),
        Button(_.variant.neutral)(
          "Simulate Login",
          onClick --> (_ =>
            session.saveToken(UserToken("my-secret-token", 1000, "zozo").toJson)
          )
        )
      )
    )

    def securedContent(
        userToken: WithToken
    ): ReactiveHtmlElement[HTMLDivElement] =
      div(
        h2("Secured Content"),
        p(s"Welcome, your token is: ${userToken.token}"),
        Button(_.variant.danger)(
          "Log out",
          onClick --> (_ => session.clearUserState())
        )
      )
