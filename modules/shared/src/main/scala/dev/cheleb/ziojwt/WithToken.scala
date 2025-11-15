package dev.cheleb.ziojwt

/** A trait to represent a token with an expiration date.
  */
trait WithToken {

  /** The token itself.
    */
  val token: String

  /** The expiration date of the token.
    */
  def expiration: Option[Long]

  def expired: Boolean =
    expiration match {
      case Some(exp) => exp * 1000 < new java.util.Date().getTime()
      case None      => false
    }
}
