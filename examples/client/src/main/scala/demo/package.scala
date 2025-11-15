package demo

import dev.cheleb.ziotapir.*

import zio.json.*

/** A simple user token implementation fot demo purposes.
  *
  * Note: no expiration is set for simplicity.
  *
  * @param token
  * @param name
  */
case class UserToken(token: String, name: String)
    extends dev.cheleb.ziojwt.WithToken derives JsonCodec:
  def expiration: Option[Long] = None

given session: Session[UserToken] = SessionLive[UserToken]
