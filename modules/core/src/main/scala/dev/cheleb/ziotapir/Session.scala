package dev.cheleb.ziotapir

import dev.cheleb.ziojwt.WithToken
import sttp.model.Uri

/** A session management interface for Laminar applications.
  *
  * @tparam UserToken
  *   is covariant to allow Session[SubType]. The type of the user token, which
  *   should extend [[dev.cheleb.ziojwt.WithToken]].
  */
trait Session[+UserToken <: WithToken] {

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
  def saveToken(issuer: Uri, token: String): Unit

  /** Save the token in the storage. Tokens are stored by issuer (the host and
    * port of the issuer).
    *
    * @param token
    */
  def saveToken(token: String): Unit

  /** Get the token from the storage. Tokens are stored by issuer (the host and
    * port of the issuer).
    *
    * @param issuer
    * @return
    */
  def getToken(issuer: Uri): Option[WithToken]

  /** Load the user state from the storage with Same Origin issuer.
    *
    * This method is used restore the session (ie: page reload).
    */
  def loadUserState(): Unit

  /** Load the user state from the storage.
    *
    * his method is used restore the session (ie: page reload).
    *
    * @param issuer
    */
  def loadUserState(issuer: Uri): Unit

  /** Clear the user state. This method is used to log out the user. It should
    * remove the token from the session and the storage.
    */
  def clearUserState(): Unit
}
