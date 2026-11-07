package dev.cheleb.ziotapir

import dev.cheleb.ziojwt.WithToken
import sttp.model.Uri

final case class TestToken(token: String, expiration: Option[Long] = None)
    extends WithToken

/** A minimal in-memory [[Session]], keyed by issuer. */
final class TestSession(defaultIssuer: Uri) extends Session[TestToken] {

  private var tokens = Map.empty[Uri, TestToken]

  def isActive: Boolean = tokens.nonEmpty
  def saveToken(issuer: Uri, token: String): Unit =
    tokens += issuer -> TestToken(token)
  def saveToken(token: String): Unit = saveToken(defaultIssuer, token)
  def getToken(issuer: Uri): Option[WithToken] = tokens.get(issuer)
  def loadUserState(): Unit = ()
  def loadUserState(issuer: Uri): Unit = ()
  def clearUserState(): Unit = tokens = Map.empty
}
