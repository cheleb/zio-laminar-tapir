package dev.cheleb.ziotapir.laminar

import com.raquo.laminar.api.L.*
import dev.cheleb.ziojwt.WithToken
import dev.cheleb.ziotapir.*
import sttp.model.Uri
import zio.json.*
import scala.scalajs.js.Date

trait LaminarSession[+UserToken <: WithToken] extends Session[UserToken] {

  /** This method will return a Signal that will be updated when the user state
    * changes.
    *
    * It takes two arguments to produce a value depending on the user state
    *
    * @param withoutSession
    *   the value to produce when the user is not logged in
    * @param withSession
    *   the value to produce when the user is logged in
    * @return
    */
  def apply[A](withoutSession: => A)(
      withSession: UserToken => A
  ): Signal[A]
  def whenActive[A](callback: => A): Signal[Option[A]]
}

/** A live implementation of the Session trait using localStorage to persist the
  * user token.
  *
  * @tparam UserToken
  *   the type of the user token, which should extend
  *   [[dev.cheleb.ziojwt.WithToken]].
  */
class LaminarSessionLive[UserToken <: WithToken](using
    JsonDecoder[UserToken]
) extends LaminarSession[UserToken] {
  val userState: Var[Option[UserToken]] = Var(Option.empty[UserToken])

  /** This method is used to produce a key to store the token in the storage.
    *
    * @param issuer
    * @return
    */
  private def userTokenKey(issuer: Uri): String =
    (for {
      host <- issuer.host
      port <- issuer.port
    } yield s"userToken@$host:$port")
      .getOrElse(s"userToken@${issuer.toString}")

  def apply[A](
      withoutSession: => A
  )(withSession: UserToken => A): Signal[A] =
    userState.signal.map {
      case Some(userToken) => withSession(userToken)
      case None            => withoutSession
    }

  def whenActive[A](callback: => A): Signal[Option[A]] =
    userState.signal.map(_.map(_ => callback))

  def isActive = userState
    .now()
    .map(_.expiration)
    .map {
      case Some(exp) => exp * 1000 > new Date().getTime()
      case None      => true // No expiration means always valid
    } match {
    case Some(true)  => true
    case Some(false) =>
      userState.set(Option.empty[UserToken])
      Storage.removeAll()
      false
    case None => false
  }

  def saveToken(issuer: Uri, token: String): Unit = {
    userState.set(token.fromJson[UserToken].toOption)
    Storage.set(userTokenKey(issuer), token)
  }

  def saveToken(token: String): Unit = {
    userState.set(token.fromJson[UserToken].toOption)
    Storage.set(userTokenKey(BackendClientLive.backendBaseURL), token)
  }

  def getToken(issuer: Uri): Option[WithToken] =
    loadUserState(issuer)
    userState.now()

  def loadUserState(): Unit =
    loadUserState(BackendClientLive.backendBaseURL)
  def loadUserState(issuer: Uri): Unit =
    Storage
      .get(userTokenKey(issuer))
      .flatMap(tokenStr =>
        summon[JsonDecoder[UserToken]].decodeJson(tokenStr).toOption
      )
      .foreach {
        case token if token.expired =>
          Storage.remove(userTokenKey(issuer))
        case token =>
          userState.now() match
            case Some(value) if token != value => userState.set(Some(token))
            case None                          => userState.set(Some(token))
            case _                             => ()
      }

  def clearUserState(): Unit =
    userState.set(Option.empty[UserToken])
    Storage.removeAll()
}
