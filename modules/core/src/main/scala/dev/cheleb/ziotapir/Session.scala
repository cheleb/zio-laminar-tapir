package dev.cheleb.ziotapir

import zio.json.*

import scala.scalajs.js.Date

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L.*
import dev.cheleb.ziojwt.WithToken
import dev.cheleb.ziotapir.BackendClientLive
import dev.cheleb.ziotapir.Storage
import sttp.model.Uri

/** A session management interface for Laminar applications.
  *
  * @tparam UserToken
  *   The type of the user token, which should extend
  *   [[dev.cheleb.ziojwt.WithToken]].
  */
trait Session[+UserToken <: WithToken] {

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
      withSession: WithToken => A
  ): Signal[A]
  def whenActive[A](callback: => A): Signal[Option[A]]

  /** This method returns true user is active.
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
  def saveToken(issuer: Uri, token: WithToken)(using
      JsonEncoder[WithToken]
  ): Unit

  /** Save the token in the storage. Tokens are stored by issuer (the host and
    * port of the issuer).
    *
    * @param token
    */
  def saveToken(token: WithToken)(using
      JsonEncoder[WithToken]
  ): Unit

  /** Get the token from the storage. Tokens are stored by issuer (the host and
    * port of the issuer).
    *
    * @param issuer
    * @return
    */
  def getToken(issuer: Uri): Option[WithToken]

  /** Load the user state from the storage with Same Origin issuer. This method
    * is used to log in the user.
    */
  def loadUserState(): Unit

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

class SessionLive[UserToken <: WithToken](using
//    JsonEncoder[UserToken],
    JsonDecoder[UserToken]
) extends Session[UserToken] {
  val userState: Var[Option[WithToken]] = Var(Option.empty[WithToken])

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
  )(withSession: WithToken => A): Signal[A] =
    userState.signal.map {
      case Some(userToken) => withSession(userToken)
      case None            => withoutSession
    }

  def whenActive[A](callback: => A): Signal[Option[A]] =
    userState.signal.map(_.map(_ => callback))

  def isActive = userState
    .now()
    .map(_.expiration * 1000 > new Date().getTime()) match {
    case Some(true)  => true
    case Some(false) =>
      userState.set(Option.empty[UserToken])
      Storage.removeAll()
      false
    case None => false
  }

  def saveToken(issuer: Uri, token: WithToken)(using
      JsonEncoder[WithToken]
  ): Unit = {
    userState.set(Option(token))
    Storage.set(userTokenKey(issuer), token)
  }

  def saveToken(token: WithToken)(using JsonEncoder[WithToken]): Unit = {
    userState.set(Option(token))
    Storage.set(userTokenKey(BackendClientLive.backendBaseURL), token)
  }

  def getToken(issuer: Uri): Option[WithToken] =
    loadUserState(issuer)
    userState.now()

  def loadUserState(): Unit =
    loadUserState(BackendClientLive.backendBaseURL)
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
