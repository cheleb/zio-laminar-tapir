package dev.cheleb.ziolaminartapir

import scala.scalajs.js.Date

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L.*

import zio.json.*
import dev.cheleb.ziojwt.WithToken
import sttp.model.Uri

trait Session[UserToken <: WithToken] {
  def apply[A](withSession: => A)(withoutSession: => A): Signal[Option[A]]
  def whenActive[A](callback: => A): Signal[Option[A]]
  def isActive: Boolean
  def saveToken(issuer: Uri, token: UserToken): Unit
  def getToken(issuer: Uri): Option[UserToken]
  def loadUserState(issuer: Uri): Unit
  def clearUserState(): Unit
}

class SessionLive[UserToken <: WithToken](using JsonCodec[UserToken])
    extends Session[UserToken] {
  val userState: Var[Option[UserToken]] = Var(Option.empty[UserToken])

  private def userTokenKey(issuer: Uri) =
    (for {
      host <- issuer.host
      port <- issuer.port
    } yield s"userToken@$host:$port")
      .getOrElse(s"userToken@${issuer.toString}")

  def apply[A](withSession: => A)(withoutSession: => A): Signal[Option[A]] =
    userState.signal.map {
      case Some(_) => Some(withSession)
      case None    => Some(withoutSession)
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

  // TODO Should be more clever about expiration.
  def isActive = userState.now().isDefined

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
