package dev.cheleb.ziolaminartapir

import scala.scalajs.js.Date

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L.*

import zio.json.*
import dev.cheleb.ziojwt.WithToken
import sttp.model.Uri

trait Session[UserToken <: WithToken] {
  def apply[A](withoutSession: => A)(
      withSession: UserToken => A
  ): Signal[A]
  def whenActive[A](callback: => A): Signal[Option[A]]

  /** This method is used to produce an Option when the user is active.
    *
    * @return
    */
  def isActive: Boolean

  /** Save the token in the storage. Tokens are stored by issuer (the host and
    * port of the issuer).
    *
    * @param issuer
    * @param token
    */
  def saveToken(issuer: Uri, token: UserToken): Unit

  /** Get the token from the storage. Tokens are stored by issuer (the host and
    * port of the issuer).
    *
    * @param issuer
    * @return
    */
  def getToken(issuer: Uri): Option[UserToken]

  /** Load the user state from the storage. This method is used to log in the
    *
    * @param issuer
    */
  def loadUserState(issuer: Uri): Unit

  /** Clear the user state. This method is used to log out the user. It should
    * remove the token from the session and the storage.
    */
  def clearUserState(): Unit
}

class SessionLive[UserToken <: WithToken](using JsonCodec[UserToken])
    extends Session[UserToken] {
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

  /** This method is used to produce different values depending on the user
    * state.
    *
    * @param withoutSession
    * @param withSession
    * @return
    */
  def apply[A](
      withoutSession: => A
  )(withSession: UserToken => A): Signal[A] =
    userState.signal.map {
      case Some(userToken) => withSession(userToken)
      case None            => withoutSession
    }

  /** This method is used to produce an Option when the user is active.
    *
    * Convenient to render an element only when the user is active.
    *
    * See ChildReceiver.maybe for more information.
    *
    * @param callback
    * @return
    */
  def whenActive[A](callback: => A): Signal[Option[A]] =
    userState.signal.map(_.map(_ => callback))

  def isActive = userState
    .now()
    .map(_.expiration * 1000 > new Date().getTime()) match {
    case Some(true) => true
    case Some(false) =>
      userState.set(Option.empty[UserToken])
      Storage.removeAll()
      false
    case None => false
  }

  def saveToken(issuer: Uri, token: UserToken): Unit = {
    userState.set(Option(token))
    Storage.set(userTokenKey(issuer), token)
  }

  def getToken(issuer: Uri): Option[UserToken] =
    loadUserState(issuer)
    userState.now()

  def loadUserState(issuer: Uri): Unit =
    Storage
      .get[UserToken](userTokenKey(issuer))
      .foreach {
        case exp: WithToken if exp.expiration * 1000 < new Date().getTime() =>
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
